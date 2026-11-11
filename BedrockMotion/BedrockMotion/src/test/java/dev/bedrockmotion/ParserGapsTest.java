package dev.bedrockmotion;

import dev.bedrockmotion.bedrock.ClientEntityDef;
import dev.bedrockmotion.bedrock.GeometryDef;
import dev.bedrockmotion.bedrock.LocatorSolver;
import dev.bedrockmotion.bedrock.ParticleDef;
import dev.bedrockmotion.json.Json;
import dev.bedrockmotion.molang.BoneSolver;
import dev.bedrockmotion.molang.Struct;
import dev.bedrockmotion.molang.Env;
import dev.bedrockmotion.molang.Script;
import dev.bedrockmotion.molang.Symbols;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public final class ParserGapsTest {

    private static int failures;

    private static final String ENTITY = """
        {"minecraft:client_entity":{"description":{"identifier":"test:thing","scripts":{
        "scaleX":"variable.h","scaleY":"variable.v","scaleZ":"variable.h","scale":"1.5",
        "parent_setup":"variable.chest_layer_visible = 0.0;",
        "variables":{"variable.attack_time":"public","variable.hidden":"private","v.Other_Var":"public"}}}}}
        """;

    private static final String LEGACY = """
        {"geometry.legacy":{"texturewidth":64,"textureheight":64,"bones":[
        {"name":"body","pivot":[0,19,2],"bind_pose_rotation":[90,0,0],"cubes":[{"origin":[-4,13,-5],"size":[8,16,6],"uv":[28,8]}]},
        {"name":"cape","pivot":[0,24,3],"rotation":[0,180,0],"bind_pose_rotation":[0,180,0]}]}}
        """;

    private static final String HORSE = """
        {"geometry.horse":{"bones":[
        {"name":"Leg1A","pivot":[3,11,9],"cubes":[{"origin":[1,0,7],"size":[4,11,4],"uv":[48,21]}]}]}}
        """;

    private static final String PARTICLE = """
        {"format_version":"1.10.0","particle_effect":{"description":{"identifier":"test:ground",
        "basic_render_parameters":{"material":"particles_alpha","texture":"atlas.terrain"}},
        "components":{"minecraft:emitter_rate_instant":{"num_particles":1},
        "minecraft:emitter_lifetime_once":{"active_time":1},
        "minecraft:particle_lifetime_expression":{"max_lifetime":1},
        "minecraft:particle_appearance_billboard":{"size":[0.1,0.1],"facing_camera_mode":"lookat_xyz",
        "uv":{"uv":["0.25","0.5"],"uv_size":["0.125","0.125"]}}}}}
        """;

    private static void check(String what, Object actual, Object expected) {
        if (!String.valueOf(actual).equals(String.valueOf(expected))) {
            failures++;
            System.out.println("ERROR " + what + ": " + actual + ", expected " + expected);
        }
    }

    public static void main(String[] args) {
        Symbols symbols = new Symbols();
        List<String> problems = new ArrayList<>();
        ClientEntityDef def = ClientEntityDef.parse(Json.parse(ENTITY), symbols, problems);
        check("problems", problems, "[]");
        check("scaleX parsed", def.scaleX != null, true);
        check("scaleY parsed", def.scaleY != null, true);
        check("scaleZ parsed", def.scaleZ != null, true);
        check("uniform scale", def.scale.runNum(new Env(symbols)), 1.5);
        check("public variables", new TreeSet<>(def.publicVariables), "[attack_time, other_var]");
        check("parent_setup parsed", def.parentSetup != null, true);

        Env owner = new Env(symbols);
        def.parentSetup.run(owner);
        Script read = Script.compile("return variable.chest_layer_visible;", symbols, problems);
        check("parent_setup effect", read.runNum(owner), 0.0);

        Env attachment = new Env(symbols);
        Script write = Script.compile("variable.attack_time = 0.75; variable.hidden = 5;", symbols, problems);
        write.run(owner);
        attachment.owner = owner;
        attachment.ownerPublic = def.publicVariables;
        check(
                "public owner variable",
                Script.compile("return variable.attack_time;", symbols, problems).runNum(attachment), 0.75);
        check(
                "private owner variable",
                Script.compile("return variable.hidden;", symbols, problems).runNum(attachment), 0.0);
        check(
                "own variable wins",
                Script.compile("variable.attack_time = 2; return variable.attack_time;", symbols, problems).runNum(attachment), 2.0);

        attachment.context.put("owning_entity", owner);
        owner.entity.extra.put("ticks_since_last_kinetic_weapon_hit", 3.0);
        check(
                "owning_entity arrow query",
                Script.compile("return c.owning_entity->query.ticks_since_last_kinetic_weapon_hit >= 0.0 && c.owning_entity->query.ticks_since_last_kinetic_weapon_hit < 5.0;", symbols, problems)
                        .runNum(attachment),
                1.0);
        check(
                "owning_entity arrow variable",
                Script.compile("return c.owning_entity->variable.attack_time;", symbols, problems).runNum(attachment),
                0.75);

        List<GeometryDef> geometries = GeometryDef.parse(Json.parse(LEGACY), problems);
        GeometryDef geo = geometries.get(0);
        check("bind pose as rotation", geo.bones[geo.indexOf("body")].rotX, (float) Math.toRadians(-90));
        check("rotation wins over bind pose", geo.bones[geo.indexOf("cape")].rotY, (float) Math.toRadians(-180));

        GeometryDef horse = GeometryDef.parse(Json.parse(HORSE), problems).get(0);
        Struct box = LocatorSolver.aabbStruct(horse, "leg1a");
        Env aabbEnv = new Env(symbols);
        aabbEnv.bones = new BoneSolver() {
            @Override
            public Struct origin(String bone) {
                return new Struct();
            }

            @Override
            public Struct trs(String bone) {
                return new Struct();
            }

            @Override
            public Struct aabb(String bone) {
                return LocatorSolver.aabbStruct(horse, bone);
            }
        };
        String adjust = "variable.box = query.bone_aabb('leg1a'); return (variable.box.max.y - variable.box.min.y) * (1.0 - (query.model_scale * 16.0));";
        check("aabb height in blocks", Script.compile("variable.box = query.bone_aabb('leg1a'); return variable.box.max.y - variable.box.min.y;", symbols, problems).runNum(aabbEnv), 0.6875);
        check("aabb width in blocks", Script.compile("variable.box = query.bone_aabb('leg1a'); return variable.box.max.x - variable.box.min.x;", symbols, problems).runNum(aabbEnv), 0.25);
        check("aabb unknown bone", Script.compile("variable.box = query.bone_aabb('nope'); return variable.box.max.y;", symbols, problems).runNum(aabbEnv), 0.0);
        check("horse y_adjust at default scale", Script.compile(adjust, symbols, problems).runNum(aabbEnv), 0.0);
        aabbEnv.entity.modelScale = 0.5 / 16.0;
        check("horse y_adjust for a baby", Script.compile(adjust, symbols, problems).runNum(aabbEnv), 0.6875 * 0.5);
        check("box exists", box.fields.containsKey("min"), true);

        ParticleDef ground = ParticleDef.parse(Json.parse(PARTICLE), symbols, problems);
        check("atlas particle uv is normalized", ground.texWidth + "x" + ground.texHeight, "1.0x1.0");
        check("no problems", problems, "[]");

        System.out.println(failures == 0 ? "ParserGapsTest: all tests passed" : "ParserGapsTest: " + failures + " errors");
        if (failures > 0) System.exit(1);
    }
}

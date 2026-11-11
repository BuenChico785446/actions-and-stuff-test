package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.molang.Env;
import dev.bedrockmotion.molang.EntitySnapshot;
import dev.bedrockmotion.molang.Queries;
import dev.bedrockmotion.molang.Script;
import dev.bedrockmotion.molang.Symbols;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.world.entity.Entity;

public final class MobStatesTest {

    private static int failures;
    private static final Symbols SYMBOLS = new Symbols();

    public enum ArmadilloState { IDLE, ROLLING, SCARED, UNROLLING }

    public enum CopperState { IDLE, GETTING_ITEM, GETTING_NO_ITEM, DROPPING_ITEM, DROPPING_NO_ITEM }

    public enum Weather { UNAFFECTED, EXPOSED, WEATHERED, OXIDIZED }

    public enum HorseVariant { WHITE, CREAMY, CHESTNUT, BROWN, BLACK, GRAY, DARK_BROWN }

    public static final class Anim {
        public boolean started;

        public boolean isStarted() {
            return started;
        }
    }

    public static final class Key {
        private final String path;

        Key(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return "ResourceKey[minecraft:cat_variant / minecraft:" + path + "]";
        }
    }

    public static final class Holder {
        private final Key key;

        Holder(String path) {
            key = new Key(path);
        }

        public Optional<Key> unwrapKey() {
            return Optional.of(key);
        }

        @Override
        public String toString() {
            return "Reference{" + key + "=CatVariant[texture=entity/cat/x]}";
        }
    }

    public static final class Armadillo extends Entity {
        public ArmadilloState state = ArmadilloState.IDLE;
        public final Anim peekAnimationState = new Anim();

        public ArmadilloState getState() {
            return state;
        }
    }

    public static final class Bee extends Entity {
        public boolean nectar = true;
        public boolean stung = true;

        public boolean hasNectar() {
            return nectar;
        }

        public boolean hasStung() {
            return stung;
        }
    }

    public static final class Creaking extends Entity {
        public boolean active = true;
        public boolean canMove = false;

        public boolean isActive() {
            return active;
        }

        public boolean canMove() {
            return canMove;
        }
    }

    public static final class CopperGolem extends Entity {
        public CopperState state = CopperState.IDLE;
        public Weather weather = Weather.UNAFFECTED;

        public CopperState getState() {
            return state;
        }

        public Weather getWeatherState() {
            return weather;
        }
    }

    public static final class Slime extends Entity {
        public float squish = 0.6f;
        public float oSquish = 0.2f;

        public int getSize() {
            return 4;
        }
    }

    public static final class Horse extends Entity {
        public HorseVariant getVariant() {
            return HorseVariant.DARK_BROWN;
        }

        public HorseVariant getMarkings() {
            return HorseVariant.WHITE;
        }
    }

    public static final class Cat extends Entity {
        public Holder getVariant() {
            return new Holder("tabby");
        }
    }

    public static final class Shulker extends Entity {
        public Object getColor() {
            return null;
        }
    }

    public static final class Warden extends Entity {
        public float getTendrilAnimation(float pt) {
            return 1.0f;
        }

        public float getHeartAnimation(float pt) {
            return 0.25f;
        }
    }

    public enum PiglinArmPose { DEFAULT, DANCING, ADMIRING_ITEM }

    public static final class Piglin extends Entity {
        public PiglinArmPose pose = PiglinArmPose.DANCING;

        public PiglinArmPose getArmPose() {
            return pose;
        }
    }

    public static final class Turtle extends Entity {
        public boolean hasEgg() {
            return true;
        }
    }

    public static final class Panda extends Entity {
        private int rollCounter = 16;

        public float getSitAmount(float pt) {
            return 0.5f;
        }
    }

    private static void check(String what, Object actual, Object expected) {
        if (!String.valueOf(actual).equals(String.valueOf(expected))) {
            failures++;
            System.out.println("ERROR " + what + ": " + actual + ", expected " + expected);
        }
    }

    private static EntitySnapshot fill(Entity entity) {
        EntitySnapshot s = new EntitySnapshot();
        BmMobStates.fill(entity, s, 0.5f);
        return s;
    }

    private static double eval(EntitySnapshot s, String source) {
        ArrayList<String> problems = new ArrayList<>();
        Script script = Script.compile(source, SYMBOLS, problems);
        if (!problems.isEmpty()) System.out.println("  Problem: " + problems);
        Env env = new Env(SYMBOLS);
        env.entity = s;
        return script.runNum(env);
    }

    public static void main(String[] args) {
        Armadillo armadillo = new Armadillo();
        check("armadillo idle", fill(armadillo).properties.get("minecraft:armadillo_state"), "unrolled");
        armadillo.state = ArmadilloState.SCARED;
        check("armadillo scared", fill(armadillo).properties.get("minecraft:armadillo_state"), "rolled_up");
        armadillo.peekAnimationState.started = true;
        EntitySnapshot peek = fill(armadillo);
        check("armadillo peek", peek.properties.get("minecraft:armadillo_state"), "rolled_up_peeking");
        check("molang rolled", eval(peek, "return query.property('minecraft:armadillo_state') != 'unrolled';"), 1.0);
        check("molang peeking", eval(peek, "return query.property('minecraft:armadillo_state') == 'rolled_up_peeking';"), 1.0);
        armadillo.state = ArmadilloState.UNROLLING;
        check("armadillo unrolling", fill(armadillo).properties.get("minecraft:armadillo_state"), "rolled_up_unrolling");

        Bee bee = new Bee();
        EntitySnapshot beeState = fill(bee);
        check("bee nectar", beeState.properties.get("minecraft:has_nectar"), 1.0);
        check("bee stinger", beeState.markVariant, 1.0);
        check("molang nectar", eval(beeState, "return query.property('minecraft:has_nectar');"), 1.0);
        bee.nectar = false;
        check("molang no nectar", eval(fill(bee), "return !query.property('minecraft:has_nectar');"), 1.0);

        Creaking creaking = new Creaking();
        EntitySnapshot observed = fill(creaking);
        check("creaking observed", observed.properties.get("minecraft:creaking_state"), "hostile_observed");
        check(
                "molang variable string",
                eval(observed, "variable.state = query.property('minecraft:creaking_state'); return variable.state == 'hostile_observed';"),
                1.0);
        creaking.canMove = true;
        check("creaking unobserved", fill(creaking).properties.get("minecraft:creaking_state"), "hostile_unobserved");
        creaking.active = false;
        check("creaking neutral", fill(creaking).properties.get("minecraft:creaking_state"), "neutral");

        CopperGolem golem = new CopperGolem();
        EntitySnapshot idle = fill(golem);
        check("golem none", idle.properties.get("minecraft:chest_interaction"), "none");
        check("molang none", eval(idle, "return query.property('minecraft:chest_interaction') == 'none';"), 1.0);
        golem.state = CopperState.DROPPING_NO_ITEM;
        golem.weather = Weather.WEATHERED;
        EntitySnapshot put = fill(golem);
        check("golem put_fail", put.properties.get("minecraft:chest_interaction"), "put_fail");
        check("golem weathered", put.properties.get("minecraft:oxidation_level"), "weathered");
        check("has_property", eval(put, "return query.has_property('minecraft:oxidation_level');"), 1.0);

        EntitySnapshot slime = fill(new Slime());
        check("slime size", slime.variant, 4.0);
        check("slime squish", slime.extra.get("current_squish_value"), 0.6000000238418579);
        check("slime previous squish", slime.extra.get("previous_squish_value"), 0.20000000298023224);
        check("fuse default", slime.extra.get("fuse_time"), -1.0);

        EntitySnapshot horse = fill(new Horse());
        check("horse variant", horse.variant, 6.0);
        check("horse markings", horse.markVariant, 1.0);
        check("cat holder", fill(new Cat()).variant, 8.0);
        check("shulker undyed", fill(new Shulker()).variant, 16.0);

        EntitySnapshot warden = fill(new Warden());
        check("warden vibration", warden.extra.get("time_since_last_vibration_detection"), 0.0);
        check("warden heartbeat", warden.extra.get("heartbeat_phase"), 0.75);

        EntitySnapshot panda = fill(new Panda());
        check("panda sit", panda.extra.get("sit_amount"), 0.5);
        check("panda roll", panda.extra.get("roll_counter"), 16.0);

        check("piglin dance is the special celebration", fill(new Piglin()).extra.get("is_celebrating_special"), 1.0);
        Piglin admiring = new Piglin();
        admiring.pose = PiglinArmPose.ADMIRING_ITEM;
        EntitySnapshot admire = fill(admiring);
        check("piglin admiring", admire.extra.get("is_admiring"), 1.0);
        check("piglin not special", admire.extra.get("is_celebrating_special"), 0.0);
        check("turtle with egg", fill(new Turtle()).extra.get("is_pregnant"), 1.0);

        ArrayList<String> missing = new ArrayList<>();
        Queries.setMissingHandler(missing::add);
        EntitySnapshot probe = fill(new Panda());
        for (String name : new String[] {
            "is_ram_attacking", "is_eating", "lie_amount", "roll_counter", "sit_amount", "is_scared", "is_standing", "is_grazing",
            "surface_particle_color", "surface_particle_texture_size", "surface_particle_texture_coordinate",
            "ticks_since_last_kinetic_weapon_hit", "armor_texture_slot(1)", "armor_color_slot(4, 0)",
            "has_any_leashed_entity_of_type('minecraft:boat')", "is_riding_any_entity_of_type('minecraft:boat')", "bone_aabb('x')"}) {
            eval(probe, "return query." + name + (name.contains("(") ? "" : "") + ";");
        }
        check("all queries registered", missing, "[]");

        System.out.println(failures == 0 ? "MobStatesTest: all tests passed" : "MobStatesTest: " + failures + " errors");
        if (failures > 0) System.exit(1);
    }
}

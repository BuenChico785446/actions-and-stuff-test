package dev.bedrockmotion.molang;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public final class Queries {

    private static final Map<String, QueryFn> REGISTRY = new ConcurrentHashMap<>();
    private static final Set<String> MISSING = ConcurrentHashMap.newKeySet();
    private static volatile Consumer<String> missingHandler = name -> {};

    private static final String[] EXTRA_NUMERIC = {
            "armor_damage_slot",
            "base_swing_duration",
            "camera_distance_range_lerp",
            "equipment_count",
            "fuse_time",
            "get_root_locator_offset",
            "graphics_mode_is_any",
            "has_any_leashed_entity_of_type",
            "cape_flap_amount",
            "has_cape",
            "has_dash_cooldown",
            "is_delayed_attacking",
            "is_elder",
            "is_levitating",
            "is_pregnant",
            "is_sniffing",
            "kinetic_weapon_damage_duration",
            "kinetic_weapon_delay",
            "kinetic_weapon_dismount_duration",
            "kinetic_weapon_knockback_duration",
            "sneeze_counter",
            "unhappy_counter",
            "has_head_gear",
            "has_player_rider",
            "heartbeat_phase",
            "hurt_time",
            "invulnerable_ticks",
            "is_admiring",
            "is_angry",
            "is_attached_to_entity",
            "is_carrying_block",
            "is_casting",
            "is_celebrating",
            "is_celebrating_special",
            "is_charged",
            "is_charging",
            "is_chested",
            "is_croaking",
            "is_crawling",
            "is_dancing",
            "is_digging",
            "is_eating_mob",
            "is_emerging",
            "is_ghost",
            "is_grazing",
            "is_interested",
            "is_invisible",
            "is_jump_goal_jumping",
            "is_leashed",
            "is_persona_or_premium_skin",
            "is_playing_dead",
            "is_powered",
            "is_resting",
            "is_saddled",
            "is_searching",
            "is_shaking_wetness",
            "is_sheared",
            "is_shield_powered",
            "is_stalking",
            "is_stunned",
            "is_wall_climbing",
            "life_span",
            "main_hand_item_max_duration",
            "modified_swing_duration",
            "swelling_dir",
            "tail_angle",
            "texture_frame_index",
            "ticks_since_last_kinetic_weapon_hit",
            "current_squish_value",
            "surface_particle_color",
            "surface_particle_texture_coordinate",
            "surface_particle_texture_size",
            "bone_origin",
            "bone_orientation_trs",
            "bone_orientation_matrix",
            "time_since_last_vibration_detection", "trade_tier", "movement_direction", "previous_squish_value",
            "shake_angle", "swim_amount", "timer_flag_1", "timer_flag_2", "timer_flag_3"
    };

    static {
        registerStandard();
    }

    private Queries() {}

    public static void register(String name, QueryFn fn) {
        REGISTRY.put(name.toLowerCase(Locale.ROOT), fn);
    }

    public static void setMissingHandler(Consumer<String> handler) {
        missingHandler = handler;
    }

    public static Set<String> missing() {
        return MISSING;
    }

    public static boolean isRegistered(String name) {
        return REGISTRY.containsKey(name.toLowerCase(Locale.ROOT));
    }

    static QueryFn get(String name) {
        QueryFn fn = REGISTRY.get(name);
        if (fn != null) return fn;
        return new QueryFn() {
            private QueryFn resolved;

            @Override
            public Object call(Env env, Object[] args) {
                if (resolved == null) {
                    resolved = REGISTRY.get(name);
                    if (resolved == null) {
                        if (MISSING.add(name)) missingHandler.accept(name);
                        return Values.ZERO;
                    }
                }
                return resolved.call(env, args);
            }
        };
    }

    private static double d(Object[] a, int i) {
        return i < a.length ? Values.num(a[i]) : 0;
    }

    private static String s(Object o) {
        return o instanceof String x ? x : "";
    }

    private static String slotOf(Object[] a) {
        return a.length > 0 && a[0] instanceof String x ? x : "slot.weapon.mainhand";
    }

    private static int slotIndex(Object[] a) {
        return a.length > 1 && a[1] instanceof Number n ? n.intValue() : 0;
    }

    private static String[] strings(Object[] a, int from) {
        int n = 0;
        for (int i = from; i < a.length; i++) if (a[i] instanceof String) n++;
        String[] out = new String[n];
        int k = 0;
        for (int i = from; i < a.length; i++) if (a[i] instanceof String x) out[k++] = x;
        return out;
    }

    private static boolean sameId(String a, String b) {
        return strip(a).equalsIgnoreCase(strip(b));
    }

    private static String strip(String id) {
        return id.startsWith("minecraft:") ? id.substring(10) : id;
    }

    private static void num(String name, ToDoubleFunction<Env> f) {
        register(name, (env, a) -> f.applyAsDouble(env));
    }

    private static void flag(String name, Function<EntitySnapshot, Boolean> f) {
        register(name, (env, a) -> Values.bool(f.apply(env.entity)));
    }

    private static void indexed(String name, Function<Env, double[]> f) {
        register(name, (env, a) -> {
            double[] v = f.apply(env);
            int i = (int) d(a, 0);
            return i >= 0 && i < v.length ? v[i] : 0.0;
        });
    }

    private static void registerStandard() {
        num("anim_time", e -> e.animTime);
        num("life_time", e -> e.lifeTime);
        num("delta_time", e -> e.deltaTime);
        num("state_time", e -> e.stateTime);
        num("frame_alpha", e -> e.frameAlpha);
        num("time_stamp", e -> e.timeStamp);
        num("get_animation_frame", e -> e.animationFrame);
        num("time_of_day", e -> e.entity.timeOfDay);
        register("any_animation_finished", (e, a) -> Values.bool(e.anyAnimationFinished));
        register("all_animations_finished", (e, a) -> Values.bool(e.allAnimationsFinished));

        register("in_range", (e, a) -> Values.bool(d(a, 0) >= d(a, 1) && d(a, 0) <= d(a, 2)));
        register("any", (e, a) -> {
            for (int i = 1; i < a.length; i++) if (Values.equal(a[0], a[i])) return Values.ONE;
            return Values.ZERO;
        });
        register("all", (e, a) -> {
            for (int i = 1; i < a.length; i++) if (!Values.equal(a[0], a[i])) return Values.ZERO;
            return Values.ONE;
        });
        register("approx_eq", (e, a) -> Values.bool(Math.abs(d(a, 0) - d(a, 1)) < 1e-6));

        indexed("position", e -> e.entity.position);
        indexed("position_delta", e -> e.entity.positionDelta);
        num("ground_speed", e -> e.entity.groundSpeed);
        num("vertical_speed", e -> e.entity.verticalSpeed);
        num("modified_move_speed", e -> e.entity.modifiedMoveSpeed);
        num("modified_distance_moved", e -> e.entity.modifiedDistanceMoved);
        num("yaw_speed", e -> e.entity.yawSpeed);
        num("body_y_rotation", e -> e.entity.bodyYRotation);
        num("head_y_rotation", e -> e.entity.headYRotation);
        num("head_x_rotation", e -> e.entity.targetXRotation);
        num("target_x_rotation", e -> e.entity.targetXRotation);
        num("target_y_rotation", e -> e.entity.targetYRotation);
        num("ride_body_y_rotation", e -> e.entity.rideBodyYRotation);
        num("distance_from_camera", e -> e.entity.distanceFromCamera);
        num("model_scale", e -> e.entity.modelScale);
        register("camera_rotation", (e, a) -> d(a, 0) == 0 ? e.entity.cameraRotationX : e.entity.cameraRotationY);
        register("rotation_to_camera", (e, a) -> d(a, 0) == 0 ? e.entity.rotationToCameraX : e.entity.rotationToCameraY);

        flag("is_alive", s -> s.alive);
        flag("is_on_ground", s -> s.onGround);
        flag("is_in_water", s -> s.inWater);
        flag("is_in_water_or_rain", s -> s.inWater);
        flag("head_is_in_water", s -> s.headInWater);
        flag("is_in_lava", s -> s.inLava);
        flag("is_riding", s -> s.riding);
        flag("has_rider", s -> s.hasRider);
        flag("is_sneaking", s -> s.sneaking);
        flag("is_sprinting", s -> s.sprinting);
        flag("is_swimming", s -> s.swimming);
        flag("is_gliding", s -> s.gliding);
        flag("is_jumping", s -> s.jumping);
        flag("is_sleeping", s -> s.sleeping);
        flag("is_sitting", s -> s.sitting);
        flag("is_baby", s -> s.baby);
        flag("is_tamed", s -> s.tamed);
        flag("is_spectator", s -> s.spectator);
        flag("is_local_player", s -> s.localPlayer);
        flag("is_in_ui", s -> s.inUi);
        flag("is_on_fire", s -> s.onFire);
        flag("has_target", s -> s.hasTarget);
        flag("blocking", s -> s.blocking);
        flag("is_emoting", s -> s.emoting);
        flag("is_shaking", s -> s.shaking);
        flag("is_attached", s -> s.attached);
        flag("item_is_charged", s -> s.charged);
        flag("is_avoiding_mobs", s -> s.avoidingMobs);
        flag("is_roaring", s -> s.roaring);
        flag("is_sonic_boom", s -> s.sonicBoom);
        flag("is_moving", s -> s.moving);
        flag("is_using_item", s -> s.usingItem);
        flag("facing_target_to_range_attack", s -> s.hasTarget);

        num("health", e -> e.entity.health);
        num("max_health", e -> e.entity.maxHealth);
        num("death_ticks", e -> e.entity.deathTicks);
        num("hurt_direction", e -> e.entity.hurtDirection);
        num("swell_amount", e -> e.entity.swellAmount);
        num("variant", e -> e.entity.variant);
        num("mark_variant", e -> e.entity.markVariant);
        num("skin_id", e -> e.entity.skinId);
        num("main_hand_item_use_duration", e -> e.entity.mainHandItemUseDuration);
        num("main_hand_item_max_duration", e -> e.entity.mainHandItemMaxDuration);
        num("swim_amount", e -> e.entity.engineVars.getOrDefault("swim_amount", 0.0));
        register("movement_direction", (e, a) -> {
            double[] v = e.entity.positionDelta;
            double len = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
            int i = (int) d(a, 0);
            return len < 1.0e-9 || i < 0 || i > 2 ? 0.0 : v[i] / len;
        });
        num("item_remaining_use_duration", e -> e.entity.itemRemainingUseDuration);
        num("item_max_use_duration", e -> e.entity.itemMaxUseDuration);
        num("cooldown_time_remaining", e -> e.entity.cooldownTimeRemaining);

        register("get_name", (e, a) -> e.entity.name);
        register("is_name_any", (e, a) -> {
            for (String n : strings(a, 0)) if (n.equals(e.entity.name)) return Values.ONE;
            return Values.ZERO;
        });
        register("get_equipped_item_name", (e, a) -> e.probe.equippedItemName(slotOf(a), slotIndex(a)));
        register("is_item_equipped", (e, a) -> Values.bool(!e.probe.equippedItemName(slotOf(a), slotIndex(a)).isEmpty()));
        register("is_item_name_any", (e, a) -> {
            String have = e.probe.equippedItemName(slotOf(a), slotIndex(a));
            if (have.isEmpty()) return Values.ZERO;
            for (String n : strings(a, 1)) if (sameId(have, n)) return Values.ONE;
            return Values.ZERO;
        });
        register("equipped_item_any_tag", (e, a) -> Values.bool(e.probe.equippedItemHasAnyTag(s(a.length > 0 ? a[0] : ""), strings(a, 1))));
        register("equipped_item_all_tags", (e, a) -> Values.bool(e.probe.equippedItemHasAllTags(s(a.length > 0 ? a[0] : ""), strings(a, 1))));
        register("relative_block_has_any_tag", (e, a) -> Values.bool(e.probe.relativeBlockHasAnyTag(d(a, 0), d(a, 1), d(a, 2), strings(a, 3))));
        register("block_has_any_tag", (e, a) -> Values.bool(e.probe.blockHasAnyTag(d(a, 0), d(a, 1), d(a, 2), strings(a, 3))));
        register("entity_biome_has_any_identifier", (e, a) -> Values.bool(e.probe.biomeHasAnyIdentifier(strings(a, 0))));
        register("is_riding_any_entity_of_type", (e, a) -> Values.bool(e.probe.ridingAnyEntityOfType(strings(a, 0))));
        register("is_owner_identifier_any", (e, a) -> Values.bool(e.probe.ownerIdentifierAny(strings(a, 0))));
        register("armor_texture_slot", (e, a) -> (double) e.probe.armorTextureSlot((int) d(a, 0)));
        register("armor_color_slot", (e, a) -> (double) e.probe.armorColorSlot((int) d(a, 0)));
        register("property", (e, a) -> {
            String key = s(a.length > 0 ? a[0] : "");
            Double local = e.entity.properties.get(key);
            return local != null ? local : e.probe.property(key);
        });
        register("has_property", (e, a) -> Values.bool(e.entity.properties.containsKey(s(a.length > 0 ? a[0] : ""))));
        register("is_pack_setting_selected", (e, a) -> Values.bool(e.settings.selected(s(a.length > 0 ? a[0] : ""), a.length > 1 ? s(a[1]) : "")));
        register("graphics_mode_is_any", (e, a) -> {
            for (Object o : a) {
                if (o instanceof String s && (s.equalsIgnoreCase("fancy") || s.equalsIgnoreCase("simple"))) return Values.ONE;
            }
            return Values.ZERO;
        });
        num("base_swing_duration", e -> 6);
        num("modified_swing_duration", e -> 6);
        register("item_slot_to_bone_name", (e, a) -> {
            Object f = e.context.get("slot_bone");
            if (a.length == 0 || !(f instanceof java.util.function.Function<?, ?> fn)) return "";
            @SuppressWarnings("unchecked")
            Object r = ((java.util.function.Function<Object, Object>) fn).apply(a[0]);
            return r == null ? "" : r;
        });
        register("bone_origin", (e, a) -> e.bones == null || a.length == 0 ? Values.ZERO : e.bones.origin(s(a[0])));
        register("bone_orientation_trs", (e, a) -> e.bones == null || a.length == 0 ? Values.ZERO : e.bones.trs(s(a[0])));
        num("body_x_rotation", e -> e.entity.targetXRotation);
        num("eye_target_x_rotation", e -> e.entity.targetXRotation);
        num("eye_target_y_rotation", e -> e.entity.targetYRotation);
        for (String name : EXTRA_NUMERIC) {
            if (!REGISTRY.containsKey(name)) {
                register(name, (e, a) -> e.entity.extra.getOrDefault(name, 0.0));
            }
        }
        register("is_pack_setting_enabled", (e, a) -> Values.bool(e.settings.enabled(s(a.length > 0 ? a[0] : ""))));
    }
}

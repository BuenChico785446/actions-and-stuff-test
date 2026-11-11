package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.molang.EntitySnapshot;
import java.util.List;

final class BmMobStates {

    private static final List<String> CAT = List.of(
            "white", "black", "red", "siamese", "british_shorthair", "calico", "persian", "ragdoll", "tabby", "all_black", "jellie");
    private static final List<String> WOLF =
            List.of("pale", "ashen", "black", "chestnut", "rusty", "snowy", "spotted", "striped", "woods");
    private static final List<String> FOX = List.of("red", "snow");
    private static final List<String> AXOLOTL = List.of("lucy", "cyan", "gold", "wild", "blue");
    private static final List<String> FROG = List.of("temperate", "cold", "warm");
    private static final List<String> RABBIT = List.of("brown", "white", "black", "white_splotched", "gold", "salt");
    private static final List<String> PARROT = List.of("red_blue", "blue", "green", "yellow_blue", "gray");
    private static final List<String> PANDA = List.of("normal", "lazy", "worried", "playful", "brown", "weak", "aggressive");
    private static final List<String> MOOSHROOM = List.of("red", "brown");
    private static final List<String> LLAMA = List.of("creamy", "white", "brown", "gray");
    private static final List<String> HORSE = List.of("white", "creamy", "chestnut", "brown", "black", "gray", "dark_brown");
    private static final List<String> MARKINGS = List.of("none", "white", "white_field", "white_dots", "black_dots");
    private static final List<String> SHULKER = List.of(
            "black", "red", "green", "brown", "blue", "purple", "cyan", "light_gray", "gray", "pink", "lime", "yellow",
            "light_blue", "magenta", "orange", "white");
    private static final int SHULKER_UNDYED = 16;
    private static final List<String> PROFESSIONS = List.of(
            "none", "farmer", "fisherman", "shepherd", "fletcher", "librarian", "cartographer", "cleric", "armorer",
            "weaponsmith", "toolsmith", "butcher", "leatherworker", "stonemason", "nitwit");
    private static final List<String> VILLAGER_TYPES = List.of("plains", "desert", "jungle", "savanna", "snow", "swamp", "taiga");
    private static final double TENDRIL_SECONDS = 0.5;

    private BmMobStates() {}

    static void fill(Object entity, EntitySnapshot s, float pt) {
        s.variant = 0;
        s.markVariant = 0;
        s.extra.put("fuse_time", fuseTime(entity));
        s.extra.put("is_eating", BmReflect.flag(entity, "isEating"));
        boolean grazing = BmReflect.flag(entity, "isEating") > 0
                || BmReflect.numberFloat(entity, "getEatAnim", pt) > 0
                || BmReflect.numberFloat(entity, "getHeadEatPositionScale", pt) > 0;
        s.extra.put("is_grazing", grazing ? 1.0 : 0.0);
        switch (entity.getClass().getSimpleName()) {
            case "Armadillo" -> armadillo(entity, s);
            case "Bee" -> bee(entity, s);
            case "Creaking" -> creaking(entity, s);
            case "CopperGolem" -> copperGolem(entity, s);
            case "SulfurCube" -> {
                slime(entity, s);
                String archetype = BmReflect.enumName(BmReflect.call(entity, "getArchetype"));
                s.properties.put("minecraft:sulfur_cube_archetype", archetype.isEmpty() ? "none" : archetype);
            }
            case "Slime", "MagmaCube" -> slime(entity, s);
            case "Pufferfish" -> s.variant = BmReflect.number(entity, "getPuffState");
            case "Villager", "ZombieVillager" -> villager(entity, s);
            case "Horse" -> {
                s.variant = indexOf(HORSE, BmReflect.call(entity, "getVariant"));
                s.markVariant = indexOf(MARKINGS, BmReflect.call(entity, "getMarkings"));
            }
            case "Llama" -> {
                s.variant = indexOf(LLAMA, BmReflect.call(entity, "getVariant"));
                s.extra.put("is_chested", BmReflect.flag(entity, "hasChest"));
            }
            case "TraderLlama" -> {
                s.variant = indexOf(LLAMA, BmReflect.call(entity, "getVariant"));
                s.markVariant = 1;
                s.extra.put("is_chested", BmReflect.flag(entity, "hasChest"));
            }
            case "Donkey", "Mule" -> s.extra.put("is_chested", BmReflect.flag(entity, "hasChest"));
            case "Cat" -> s.variant = indexOf(CAT, BmReflect.call(entity, "getVariant"));
            case "Wolf" -> {
                s.variant = indexOf(WOLF, BmReflect.call(entity, "getVariant"));
                s.extra.put("shake_angle", BmReflect.fieldNumber(entity, "shakeAnim"));
                s.extra.put("head_roll_angle", BmReflect.numberFloat(entity, "getHeadRollAngle", pt));
            }
            case "Fox" -> {
                s.variant = indexOf(FOX, BmReflect.call(entity, "getVariant"));
                s.extra.put("is_stunned", BmReflect.flag(entity, "isFaceplanted"));
            }
            case "Axolotl" -> {
                s.variant = indexOf(AXOLOTL, BmReflect.call(entity, "getVariant"));
                s.extra.put("is_playing_dead", BmReflect.flag(entity, "isPlayingDead"));
            }
            case "Rabbit" -> s.variant = indexOf(RABBIT, BmReflect.call(entity, "getVariant"));
            case "Parrot" -> {
                s.variant = indexOf(PARROT, BmReflect.call(entity, "getVariant"));
                s.extra.put("wing_flap_position", lerpField(entity, "oFlap", "flap", pt));
                s.extra.put("wing_flap_speed", lerpField(entity, "oFlapSpeed", "flapSpeed", pt));
            }
            case "EnderDragon" -> s.extra.put("wing_flap_position", lerpField(entity, "oFlapTime", "flapTime", pt));
            case "PolarBear" -> s.extra.put("standing_scale", BmReflect.numberFloat(entity, "getStandingAnimationScale", pt));
            case "Evoker", "Illusioner" -> s.extra.put("is_casting", BmReflect.flag(entity, "isCastingSpell"));
            case "WitherBoss" -> s.extra.put("is_shield_powered", BmReflect.flag(entity, "isPowered"));
            case "Creeper" -> s.extra.put("swelling_dir", BmReflect.number(entity, "getSwellDir"));
            case "Mooshroom", "MushroomCow" -> s.variant = indexOf(MOOSHROOM, BmReflect.call(entity, "getVariant"));
            case "Shulker" -> shulker(entity, s);
            case "Panda" -> panda(entity, s, pt);
            case "Frog" -> {
                s.variant = indexOf(FROG, BmReflect.call(entity, "getVariant"));
                s.extra.put("is_eating_mob", BmReflect.started(entity, "tongueAnimationState") ? 1.0 : 0.0);
                s.extra.put("is_jump_goal_jumping", BmReflect.started(entity, "jumpAnimationState") ? 1.0 : 0.0);
            }
            case "Breeze" -> s.extra.put("is_jump_goal_jumping", BmReflect.started(entity, "longJump", "jump") ? 1.0 : 0.0);
            case "Goat" -> s.extra.put("is_ram_attacking", BmReflect.numberFloat(entity, "getRammingXHeadRot", pt) > 0 ? 1.0 : 0.0);
            case "Piglin", "PiglinBrute" -> {
                String pose = BmReflect.enumName(BmReflect.call(entity, "getArmPose"));
                s.extra.put("is_admiring", pose.equals("admiring_item") ? 1.0 : 0.0);
                s.extra.put("is_celebrating_special", pose.equals("dancing") || BmReflect.flag(entity, "isDancing") > 0 ? 1.0 : 0.0);
            }
            case "Turtle" -> {
                s.extra.put("is_pregnant", BmReflect.flag(entity, "hasEgg"));
                s.extra.put("is_laying_egg", BmReflect.flag(entity, "isLayingEgg"));
            }
            case "Warden" -> warden(entity, s, pt);
            case "Sniffer" -> sniffer(entity, s);
            case "HappyGhast" -> {
                Object harness = BmReflect.call(entity, "getBodyArmorItem");
                s.extra.put("equipment_count", harness != null && BmReflect.flag(harness, "isEmpty") == 0 ? 1.0 : 0.0);
            }
            default -> {}
        }
    }

    private static double lerpField(Object entity, String previous, String current, float pt) {
        double before = BmReflect.fieldNumber(entity, previous);
        return before + (BmReflect.fieldNumber(entity, current) - before) * pt;
    }

    private static int indexOf(List<String> names, Object value) {
        return Math.max(0, names.indexOf(BmReflect.enumName(value)));
    }

    private static double fuseTime(Object entity) {
        return BmReflect.call(entity, "getFuse") instanceof Number n ? n.doubleValue() : -1.0;
    }

    private static void armadillo(Object entity, EntitySnapshot s) {
        String state = switch (BmReflect.enumName(BmReflect.call(entity, "getState"))) {
            case "rolling" -> "rolled_up";
            case "scared" -> BmReflect.started(entity, "peekAnimationState") ? "rolled_up_peeking" : "rolled_up";
            case "unrolling" -> "rolled_up_unrolling";
            default -> "unrolled";
        };
        s.properties.put("minecraft:armadillo_state", state);
    }

    private static void bee(Object entity, EntitySnapshot s) {
        s.properties.put("minecraft:has_nectar", BmReflect.flag(entity, "hasNectar"));
        s.markVariant = BmReflect.flag(entity, "hasStung") > 0 ? 1 : 0;
    }

    private static void creaking(Object entity, EntitySnapshot s) {
        boolean tearingDown = BmReflect.flag(entity, "isTearingDown") > 0;
        boolean active = BmReflect.flag(entity, "isActive") > 0 || BmReflect.flag(entity, "isAggressive") > 0;
        boolean canMove = BmReflect.flag(entity, "canMove") > 0;
        String state;
        if (tearingDown) state = s.deathTicks > 0 ? "crumbling" : "twitching";
        else if (!active) state = "neutral";
        else state = canMove ? "hostile_unobserved" : "hostile_observed";
        s.properties.put("minecraft:creaking_state", state);
        boolean swaying = BmReflect.started(entity, "invulnerabilityAnimationState") || s.extra.getOrDefault("hurt_time", 0.0) > 0;
        s.properties.put("minecraft:creaking_swaying_ticks", swaying ? 1.0 : 0.0);
    }

    private static void copperGolem(Object entity, EntitySnapshot s) {
        String chest = switch (BmReflect.enumName(BmReflect.call(entity, "getState"))) {
            case "getting_item" -> "take";
            case "getting_no_item" -> "take_fail";
            case "dropping_item" -> "put";
            case "dropping_no_item" -> "put_fail";
            default -> "none";
        };
        s.properties.put("minecraft:chest_interaction", chest);
        String oxidation = switch (BmReflect.enumName(BmReflect.call(entity, "getWeatherState"))) {
            case "exposed" -> "exposed";
            case "weathered" -> "weathered";
            case "oxidized" -> "oxidized";
            default -> "unoxidized";
        };
        s.properties.put("minecraft:oxidation_level", oxidation);
    }

    private static void slime(Object entity, EntitySnapshot s) {
        s.variant = BmReflect.number(entity, "getSize");
        s.extra.put("current_squish_value", BmReflect.fieldNumber(entity, "squish"));
        s.extra.put("previous_squish_value", BmReflect.fieldNumber(entity, "oSquish"));
    }

    private static void villager(Object entity, EntitySnapshot s) {
        Object data = BmReflect.call(entity, "getVillagerData");
        if (data == null) return;
        s.variant = indexOf(PROFESSIONS, BmReflect.call(data, "profession"));
        s.markVariant = indexOf(VILLAGER_TYPES, BmReflect.call(data, "type"));
        double level = BmReflect.number(data, "level");
        s.extra.put("trade_tier", Math.max(0.0, level - 1.0));
    }

    private static void shulker(Object entity, EntitySnapshot s) {
        if (!BmReflect.has(entity, "getColor")) return;
        int index = SHULKER.indexOf(BmReflect.enumName(BmReflect.call(entity, "getColor")));
        s.variant = index >= 0 ? index : SHULKER_UNDYED;
    }

    private static void panda(Object entity, EntitySnapshot s, float pt) {
        Object gene = BmReflect.has(entity, "getVariant") ? BmReflect.call(entity, "getVariant") : BmReflect.call(entity, "getMainGene");
        s.variant = indexOf(PANDA, gene);
        s.extra.put("lie_amount", BmReflect.numberFloat(entity, "getLieOnBackAmount", pt));
        s.extra.put("sit_amount", BmReflect.numberFloat(entity, "getSitAmount", pt));
        s.extra.put("roll_counter", BmReflect.fieldNumber(entity, "rollCounter"));
        s.extra.put("is_scared", BmReflect.flag(entity, "isScared"));
    }

    private static void warden(Object entity, EntitySnapshot s, float pt) {
        double tendril = BmReflect.numberFloat(entity, "getTendrilAnimation", pt);
        s.extra.put("time_since_last_vibration_detection", (1.0 - tendril) * TENDRIL_SECONDS);
        s.extra.put("heartbeat_phase", 1.0 - BmReflect.numberFloat(entity, "getHeartAnimation", pt));
    }

    private static void sniffer(Object entity, EntitySnapshot s) {
        String state = BmReflect.enumName(BmReflect.call(entity, "getState"));
        s.extra.put("timer_flag_1", state.equals("scenting") ? 1.0 : 0.0);
        s.extra.put("timer_flag_2", state.equals("feeling_happy") ? 1.0 : 0.0);
        s.extra.put("timer_flag_3", state.equals("rising") ? 1.0 : 0.0);
    }
}

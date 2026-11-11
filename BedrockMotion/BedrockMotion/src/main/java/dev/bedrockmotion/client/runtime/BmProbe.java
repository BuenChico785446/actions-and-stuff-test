package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.client.BedrockMotionClient;
import dev.bedrockmotion.molang.Struct;
import dev.bedrockmotion.molang.WorldProbe;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

final class BmProbe implements WorldProbe {

    private static final Map<String, String> BEDROCK_ITEM_TAGS = Map.of(
            "is_sword", "swords",
            "is_axe", "axes",
            "is_pickaxe", "pickaxes",
            "is_shovel", "shovels",
            "is_hoe", "hoes",
            "is_spear", "spears",
            "is_helmet", "head_armor",
            "is_chestplate", "chest_armor",
            "is_leggings", "leg_armor",
            "is_boots", "foot_armor");

    Entity entity;

    private ItemStack stack(String slot, int index) {
        if (entity instanceof Player player && index >= 0) {
            Inventory inventory = player.getInventory();
            switch (slot) {
                case "slot.hotbar" -> {
                    return index < 9 ? inventory.getItem(index) : ItemStack.EMPTY;
                }
                case "slot.inventory" -> {
                    return index < 27 ? inventory.getItem(9 + index) : ItemStack.EMPTY;
                }
                default -> {}
            }
        }
        return stack(slot);
    }

    private ItemStack stack(String slot) {
        if (!(entity instanceof LivingEntity living)) return ItemStack.EMPTY;
        return switch (slot) {
            case "slot.weapon.mainhand" -> living.getItemBySlot(EquipmentSlot.MAINHAND);
            case "slot.weapon.offhand" -> living.getItemBySlot(EquipmentSlot.OFFHAND);
            case "slot.armor.head" -> living.getItemBySlot(EquipmentSlot.HEAD);
            case "slot.armor.chest" -> living.getItemBySlot(EquipmentSlot.CHEST);
            case "slot.armor.legs" -> living.getItemBySlot(EquipmentSlot.LEGS);
            case "slot.armor.feet" -> living.getItemBySlot(EquipmentSlot.FEET);
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public String equippedItemName(String slot) {
        return nameOf(stack(slot));
    }

    @Override
    public String equippedItemName(String slot, int index) {
        return nameOf(stack(slot, index));
    }

    private static String nameOf(ItemStack s) {
        return s.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }

    @Override
    public boolean equippedItemHasAnyTag(String slot, String[] tags) {
        ItemStack s = stack(slot);
        if (s.isEmpty()) return false;
        for (String tag : tags) {
            if (hasItemTag(s, tag)) return true;
        }
        return false;
    }

    private static boolean hasItemTag(ItemStack stack, String tag) {
        Identifier id = Identifier.tryParse(tag);
        if (id == null) return false;
        if (stack.is(TagKey.create(Registries.ITEM, id))) return true;
        if (id.getPath().equals("is_food")) return stack.has(DataComponents.FOOD);
        String javaName = BEDROCK_ITEM_TAGS.get(id.getPath());
        Identifier javaId = javaName == null ? null : Identifier.tryParse("minecraft:" + javaName);
        return javaId != null && stack.is(TagKey.create(Registries.ITEM, javaId));
    }

    @Override
    public boolean equippedItemHasAllTags(String slot, String[] tags) {
        ItemStack s = stack(slot);
        if (s.isEmpty() || tags.length == 0) return false;
        for (String tag : tags) {
            if (!hasItemTag(s, tag)) return false;
        }
        return true;
    }

    @Override
    public boolean blockHasAnyTag(double x, double y, double z, String[] tags) {
        if (entity == null) return false;
        var state = entity.level().getBlockState(BlockPos.containing(x, y, z));
        for (String tag : tags) {
            Identifier id = Identifier.tryParse(tag);
            if (id != null && state.is(TagKey.create(Registries.BLOCK, id))) return true;
        }
        return false;
    }

    @Override
    public boolean relativeBlockHasAnyTag(double dx, double dy, double dz, String[] tags) {
        if (entity == null) return false;
        return blockHasAnyTag(entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz, tags);
    }

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.BODY
    };
    private static final int BODY_SLOT = 4;
    private static final int LEATHER_RGB = 0xA06540;
    private static final List<String> WEARABLE_MATERIALS =
            List.of("leather", "chainmail", "iron", "diamond", "golden", "elytra", "turtle", "netherite", "copper");
    private static final List<String> BODY_MATERIALS = List.of("leather", "iron", "golden", "diamond", "copper", "netherite");
    private static final List<String> NAUTILUS_MATERIALS = List.of("copper", "iron", "golden", "diamond", "netherite");

    private ItemStack armorStack(int slot) {
        if (!(entity instanceof LivingEntity living) || slot < 0 || slot >= ARMOR_SLOTS.length) return ItemStack.EMPTY;
        return living.getItemBySlot(ARMOR_SLOTS[slot]);
    }

    private static int materialIndex(List<String> materials, String path) {
        for (int i = 0; i < materials.size(); i++) {
            if (path.contains(materials.get(i))) return i;
        }
        return -1;
    }

    @Override
    public int armorTextureSlot(int slot) {
        ItemStack stack = armorStack(slot);
        if (stack.isEmpty()) return slot == BODY_SLOT ? 0 : -1;
        String path = nameOf(stack);
        path = path.substring(path.indexOf(':') + 1);
        if (slot != BODY_SLOT) return materialIndex(WEARABLE_MATERIALS, path);
        String type = entity.getClass().getSimpleName();
        if (type.contains("Nautilus")) return materialIndex(NAUTILUS_MATERIALS, path) + 1;
        return materialIndex(BODY_MATERIALS, path) + 1;
    }

    @Override
    public double armorColorSlot(int slot, int channel) {
        ItemStack stack = armorStack(slot);
        if (stack.isEmpty() || channel < 0 || channel > 2 || !nameOf(stack).contains("leather")) return 1.0;
        Object dyed = stack.get(DataComponents.DYED_COLOR);
        int rgb = dyed == null ? LEATHER_RGB : (int) BmReflect.number(dyed, "rgb");
        return ((rgb >> (16 - 8 * channel)) & 0xFF) / 255.0;
    }

    private static boolean sameEntityType(String have, String want) {
        String path = have.substring(have.indexOf(':') + 1);
        String expected = want.substring(want.indexOf(':') + 1);
        boolean chest = path.contains("chest_boat") || path.contains("chest_raft");
        boolean vessel = path.endsWith("boat") || path.endsWith("raft");
        if (expected.equals("boat")) return vessel && !chest;
        if (expected.equals("chest_boat")) return vessel && chest;
        return path.equalsIgnoreCase(expected);
    }

    private static boolean anyEntityType(Entity candidate, String[] ids) {
        String have = BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType()).toString();
        for (String id : ids) {
            if (sameEntityType(have, id)) return true;
        }
        return false;
    }

    @Override
    public String ownerIdentifier() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    @Override
    public boolean ownerIdentifierAny(String[] ids) {
        return anyEntityType(entity, ids);
    }

    @Override
    public boolean ridingAnyEntityOfType(String[] ids) {
        Entity vehicle = entity.getVehicle();
        return vehicle != null && anyEntityType(vehicle, ids);
    }

    @Override
    public boolean leashedEntityOfType(String[] ids) {
        if (!(BmReflect.call(entity.level(), "entitiesForRendering") instanceof Iterable<?> all)) return false;
        for (Object other : all) {
            if (other instanceof Entity candidate && candidate != entity
                    && BmReflect.call(candidate, "getLeashHolder") == entity && anyEntityType(candidate, ids)) {
                return true;
            }
        }
        return false;
    }

    private static boolean spriteWarned;

    @Override
    public Struct surfaceParticle(String kind) {
        BlockPos pos = BlockPos.containing(entity.getX(), entity.getY() - 0.5, entity.getZ());
        BlockState state = entity.level().getBlockState(pos);
        Minecraft minecraft = Minecraft.getInstance();
        Struct out = new Struct();
        if (kind.equals("color")) {
            Object tint = BmReflect.callWith(BmReflect.call(minecraft, "getBlockColors"), "getColor", state, entity.level(), pos, 0);
            int rgb = tint instanceof Integer color && color != -1 ? color : 0xFFFFFF;
            out.fields.put("r", ((rgb >> 16) & 0xFF) / 255.0);
            out.fields.put("g", ((rgb >> 8) & 0xFF) / 255.0);
            out.fields.put("b", (rgb & 0xFF) / 255.0);
            out.fields.put("a", 1.0);
            return out;
        }
        Object shaper = BmReflect.call(BmReflect.call(minecraft, "getBlockRenderer"), "getBlockModelShaper");
        Object sprite = BmReflect.callWith(shaper, "getParticleIcon", state);
        if (sprite == null && !spriteWarned) {
            spriteWarned = true;
            BedrockMotionClient.LOGGER.warn("BedrockMotion: no particle sprite for surface particles (BlockModelShaper.getParticleIcon missing)");
        }
        double u0 = spriteEdge(sprite, "getU0", "u0");
        double v0 = spriteEdge(sprite, "getV0", "v0");
        boolean size = kind.equals("size");
        out.fields.put("u", size ? spriteEdge(sprite, "getU1", "u1") - u0 : u0);
        out.fields.put("v", size ? spriteEdge(sprite, "getV1", "v1") - v0 : v0);
        return out;
    }

    private static double spriteEdge(Object sprite, String getter, String accessor) {
        double value = BmReflect.number(sprite, getter);
        return value != 0 ? value : BmReflect.number(sprite, accessor);
    }
}

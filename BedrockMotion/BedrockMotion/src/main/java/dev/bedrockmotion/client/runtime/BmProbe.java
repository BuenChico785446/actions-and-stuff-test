package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.molang.WorldProbe;
import java.util.Map;
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
}

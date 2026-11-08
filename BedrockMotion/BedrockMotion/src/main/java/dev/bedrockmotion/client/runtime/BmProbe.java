package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.molang.WorldProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

final class BmProbe implements WorldProbe {

    Entity entity;

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
        ItemStack s = stack(slot);
        return s.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }

    @Override
    public boolean equippedItemHasAnyTag(String slot, String[] tags) {
        ItemStack s = stack(slot);
        if (s.isEmpty()) return false;
        for (String tag : tags) {
            Identifier id = Identifier.tryParse(tag);
            if (id != null && s.is(TagKey.create(Registries.ITEM, id))) return true;
        }
        return false;
    }

    @Override
    public boolean equippedItemHasAllTags(String slot, String[] tags) {
        ItemStack s = stack(slot);
        if (s.isEmpty() || tags.length == 0) return false;
        for (String tag : tags) {
            Identifier id = Identifier.tryParse(tag);
            if (id == null || !s.is(TagKey.create(Registries.ITEM, id))) return false;
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

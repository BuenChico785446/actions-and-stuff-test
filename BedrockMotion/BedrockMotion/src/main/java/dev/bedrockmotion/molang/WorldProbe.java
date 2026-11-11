package dev.bedrockmotion.molang;

public interface WorldProbe {

    WorldProbe NONE = new WorldProbe() {};

    default boolean equippedItemHasAnyTag(String slot, String[] tags) {
        return false;
    }

    default boolean equippedItemHasAllTags(String slot, String[] tags) {
        return false;
    }

    default String equippedItemName(String slot) {
        return "";
    }

    default String equippedItemName(String slot, int index) {
        return equippedItemName(slot);
    }

    default boolean relativeBlockHasAnyTag(double dx, double dy, double dz, String[] tags) {
        return false;
    }

    default boolean blockHasAnyTag(double x, double y, double z, String[] tags) {
        return false;
    }

    default boolean biomeHasAnyIdentifier(String[] ids) {
        return false;
    }

    default boolean ridingAnyEntityOfType(String[] ids) {
        return false;
    }

    default boolean ownerIdentifierAny(String[] ids) {
        return false;
    }

    default int armorTextureSlot(int slot) {
        return 0;
    }

    default double armorColorSlot(int slot, int channel) {
        return 1.0;
    }

    default Struct surfaceParticle(String kind) {
        return new Struct();
    }

    default boolean leashedEntityOfType(String[] ids) {
        return false;
    }

    default double property(String key) {
        return 0;
    }
}

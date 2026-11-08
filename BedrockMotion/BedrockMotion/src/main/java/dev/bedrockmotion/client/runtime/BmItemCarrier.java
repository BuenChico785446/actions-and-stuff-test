package dev.bedrockmotion.client.runtime;

public interface BmItemCarrier {

    void bm$setItem(String itemId, int holderId);

    String bm$getItemId();

    int bm$getHolderId();
}

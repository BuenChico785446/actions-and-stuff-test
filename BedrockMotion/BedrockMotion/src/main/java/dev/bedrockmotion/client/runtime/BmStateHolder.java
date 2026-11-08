package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.client.render.BmFrame;

public interface BmStateHolder {

    BmFrame bm$getFrame();

    void bm$setFrame(BmFrame frame);

    double[] bm$getOrigin();

    void bm$setOrigin(double[] origin);
}

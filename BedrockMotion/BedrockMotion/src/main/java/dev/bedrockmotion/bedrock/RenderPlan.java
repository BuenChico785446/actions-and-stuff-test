package dev.bedrockmotion.bedrock;

import dev.bedrockmotion.pack.MaterialKind;
import java.util.ArrayList;
import java.util.List;

public final class RenderPlan {

    public static final class Layer {
        public GeometryDef geometry;
        public String texturePath;
        public final java.util.List<String> extraTexturePaths = new ArrayList<>();
        public MaterialKind[] boneKinds;
        public boolean[] boneVisible;
        public String[] bindings;
        public float r = 1, g = 1, b = 1, a = 1;
        public float hurtR, hurtG, hurtB, hurtA;
        public float fireR, fireG, fireB, fireA;
        public float overlayR, overlayG, overlayB, overlayA;
        public float lightMultiplier = 1;
        public boolean ignoreLighting;
        public float uOffset, vOffset;
        public float uScale = 1, vScale = 1;
    }

    public final List<Layer> layers = new ArrayList<>();
    public double scaleX = 1, scaleY = 1, scaleZ = 1;
}

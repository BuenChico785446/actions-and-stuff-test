package dev.bedrockmotion.client;

import dev.bedrockmotion.bedrock.ParticleSystem;
import dev.bedrockmotion.client.runtime.BmConfig;
import dev.bedrockmotion.client.runtime.BmRuntime;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BmConfigScreen extends Screen {

    private static final double[] PARTICLE_SIZES = {0.25, 0.5, 1.0, 2.0};
    private static final int WIDTH = 150;
    private static final int GAP = 10;
    private static final int ROW = 24;

    private final Screen parent;

    private record Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {}

    public BmConfigScreen(Screen parent) {
        super(Component.literal("BedrockMotion"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<Toggle> toggles = new ArrayList<>();
        toggles.add(new Toggle("Bedrock packs (needs reload)", () -> BmConfig.enabled, v -> BmConfig.enabled = v));
        toggles.add(new Toggle("Replace player model", () -> BmRuntime.replacePlayers, v -> BmRuntime.replacePlayers = v));
        for (String name : BmConfig.FEATURES) {
            toggles.add(new Toggle(name, () -> BmRuntime.features.get(name), v -> BmRuntime.features.set(name, v)));
        }
        int left = this.width / 2 - WIDTH - GAP / 2;
        int top = this.height / 6;
        Button title = Button.builder(this.title, b -> {}).bounds(this.width / 2 - WIDTH - GAP / 2, top - ROW, WIDTH * 2 + GAP, 20).build();
        title.active = false;
        addRenderableWidget(title);
        int index = 0;
        for (Toggle toggle : toggles) {
            int x = left + (index % 2) * (WIDTH + GAP);
            int y = top + (index / 2) * ROW;
            addRenderableWidget(Button.builder(label(toggle), b -> {
                toggle.set().accept(!toggle.get().getAsBoolean());
                b.setMessage(label(toggle));
            }).bounds(x, y, WIDTH, 20).build());
            index++;
        }
        int x = left + (index % 2) * (WIDTH + GAP);
        int y = top + (index / 2) * ROW;
        addRenderableWidget(Button.builder(particleLabel(), b -> {
            ParticleSystem.sizeFactor = nextParticleSize(ParticleSystem.sizeFactor);
            b.setMessage(particleLabel());
        }).bounds(x, y, WIDTH, 20).build());
        int bottom = top + ((index + 2) / 2) * ROW + 12;
        addRenderableWidget(Button.builder(Component.literal("Reload packs"), b -> {
            BmConfig.save();
            Minecraft.getInstance().reloadResourcePacks();
        }).bounds(left, bottom, WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(left + WIDTH + GAP, bottom, WIDTH, 20).build());
    }

    @Override
    public void onClose() {
        BmConfig.save();
        show(parent);
    }

    public static void show(Screen screen) {
        Minecraft minecraft = Minecraft.getInstance();
        for (String name : new String[] {"setScreenAndShow", "setScreen"}) {
            for (Method method : Minecraft.class.getMethods()) {
                if (!method.getName().equals(name) || method.getParameterCount() != 1) continue;
                if (!method.getParameterTypes()[0].isAssignableFrom(Screen.class)) continue;
                try {
                    method.invoke(minecraft, screen);
                    return;
                } catch (ReflectiveOperationException | RuntimeException e) {
                    BedrockMotionClient.LOGGER.warn("BedrockMotion: could not open screen: {}", e.getMessage());
                }
            }
        }
    }

    private static Component label(Toggle toggle) {
        return Component.literal(toggle.label() + ": " + (toggle.get().getAsBoolean() ? "ON" : "OFF"));
    }

    private static Component particleLabel() {
        return Component.literal("Particle size: " + ParticleSystem.sizeFactor + "x");
    }

    private static double nextParticleSize(double current) {
        for (double size : PARTICLE_SIZES) {
            if (size > current + 1.0e-9) return size;
        }
        return PARTICLE_SIZES[0];
    }
}

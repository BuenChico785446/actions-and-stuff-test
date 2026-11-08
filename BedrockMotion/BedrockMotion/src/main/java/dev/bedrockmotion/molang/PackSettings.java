package dev.bedrockmotion.molang;

public interface PackSettings {

    PackSettings NONE = new PackSettings() {
        @Override
        public boolean selected(String name, String option) {
            return false;
        }

        @Override
        public boolean enabled(String name) {
            return false;
        }
    };

    boolean selected(String name, String option);

    boolean enabled(String name);
}

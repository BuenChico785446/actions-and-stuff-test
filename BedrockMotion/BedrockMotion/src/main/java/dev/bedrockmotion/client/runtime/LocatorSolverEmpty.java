package dev.bedrockmotion.client.runtime;

import dev.bedrockmotion.molang.Struct;

final class LocatorSolverEmpty {

    private LocatorSolverEmpty() {}

    static Struct struct() {
        Struct s = new Struct();
        s.fields.put("x", 0.0);
        s.fields.put("y", 0.0);
        s.fields.put("z", 0.0);
        return s;
    }
}

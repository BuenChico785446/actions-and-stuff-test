package dev.bedrockmotion.molang;

public final class Values {

    public static final Double ZERO = 0.0;
    public static final Double ONE = 1.0;

    private Values() {}

    public static double num(Object o) {
        if (o instanceof Double d) return d;
        if (o instanceof Boolean b) return b ? 1 : 0;
        return 0;
    }

    public static boolean truthy(Object o) {
        if (o instanceof Double d) return d != 0;
        if (o instanceof String s) return !s.isEmpty();
        if (o instanceof Boolean b) return b;
        return o != null;
    }

    public static Double bool(boolean b) {
        return b ? ONE : ZERO;
    }

    public static boolean equal(Object a, Object b) {
        if (a instanceof String x) return b instanceof String y && x.equalsIgnoreCase(y);
        if (b instanceof String) return false;
        if (a instanceof Struct || b instanceof Struct || a instanceof Env || b instanceof Env) return a == b;
        return num(a) == num(b);
    }
}

package dev.bedrockmotion.client.runtime;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

final class BmReflect {

    private static final Map<String, Method> CACHE = new HashMap<>();
    private static final Method MISSING;

    static {
        try {
            MISSING = BmReflect.class.getDeclaredMethod("missing");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private BmReflect() {}

    private static void missing() {}

    static Object call(Object target, String name) {
        if (target == null) return null;
        String key = target.getClass().getName() + "#" + name;
        Method m = CACHE.get(key);
        if (m == null) {
            try {
                m = target.getClass().getMethod(name);
            } catch (NoSuchMethodException | SecurityException e) {
                m = MISSING;
            }
            CACHE.put(key, m);
        }
        if (m == MISSING) return null;
        try {
            return m.invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            CACHE.put(key, MISSING);
            return null;
        }
    }

    static double flag(Object target, String name) {
        Object o = call(target, name);
        if (o instanceof Boolean b) return b ? 1.0 : 0.0;
        if (o instanceof Number n) return n.doubleValue();
        return o != null ? 1.0 : 0.0;
    }
}

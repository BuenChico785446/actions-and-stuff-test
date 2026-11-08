package dev.bedrockmotion.client.runtime;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Missing methods are cached and read as 0 so version differences in Minecraft cannot break the build. */
final class BmReflect {

    private static final Method MISSING;
    private static final ClassValue<Map<String, Method>> CACHE = new ClassValue<>() {
        @Override
        protected Map<String, Method> computeValue(Class<?> type) {
            return new HashMap<>();
        }
    };

    static {
        try {
            MISSING = BmReflect.class.getDeclaredMethod("missing");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private BmReflect() {}

    private static void missing() {}

    private static Method find(Object target, String name, Class<?>... types) {
        String key = types.length == 0 ? name : name + "/" + types.length;
        Map<String, Method> cache = CACHE.get(target.getClass());
        Method m = cache.get(key);
        if (m == null) {
            try {
                m = target.getClass().getMethod(name, types);
            } catch (NoSuchMethodException | SecurityException e) {
                m = MISSING;
            }
            cache.put(key, m);
        }
        return m;
    }

    private static Object invoke(Object target, String name, Method m, Object... args) {
        if (m == MISSING) return null;
        try {
            return m.invoke(target, args);
        } catch (ReflectiveOperationException | RuntimeException e) {
            CACHE.get(target.getClass()).put(args.length == 0 ? name : name + "/" + args.length, MISSING);
            return null;
        }
    }

    static Object call(Object target, String name) {
        if (target == null) return null;
        return invoke(target, name, find(target, name));
    }

    static Object callFloat(Object target, String name, float arg) {
        if (target == null) return null;
        return invoke(target, name, find(target, name, float.class), arg);
    }

    private static double toNumber(Object o) {
        if (o instanceof Boolean b) return b ? 1.0 : 0.0;
        if (o instanceof Number n) return n.doubleValue();
        return o != null ? 1.0 : 0.0;
    }

    static double flag(Object target, String name) {
        return toNumber(call(target, name));
    }

    static double firstFlag(Object target, String... names) {
        if (target == null) return 0.0;
        for (String name : names) {
            Method m = find(target, name);
            if (m == MISSING) continue;
            return toNumber(invoke(target, name, m));
        }
        return 0.0;
    }

    static double number(Object target, String name) {
        Object o = call(target, name);
        return o instanceof Number n ? n.doubleValue() : 0.0;
    }

    static double numberFloat(Object target, String name, float arg) {
        Object o = callFloat(target, name, arg);
        return o instanceof Number n ? n.doubleValue() : 0.0;
    }
}

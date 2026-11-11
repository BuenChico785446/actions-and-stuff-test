package dev.bedrockmotion.client.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Missing methods are cached and read as 0 so version differences in Minecraft cannot break the build. */
final class BmReflect {

    private static final Method MISSING;
    private static final ClassValue<Map<String, Method>> CACHE = new ClassValue<>() {
        @Override
        protected Map<String, Method> computeValue(Class<?> type) {
            return new HashMap<>();
        }
    };

    private static final ClassValue<Map<String, Optional<Field>>> FIELDS = new ClassValue<>() {
        @Override
        protected Map<String, Optional<Field>> computeValue(Class<?> type) {
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

    static boolean has(Object target, String name) {
        return target != null && find(target, name) != MISSING;
    }

    static Object field(Object target, String name) {
        if (target == null) return null;
        Map<String, Optional<Field>> cache = FIELDS.get(target.getClass());
        Optional<Field> field = cache.get(name);
        if (field == null) {
            field = Optional.empty();
            for (Class<?> type = target.getClass(); type != null && field.isEmpty(); type = type.getSuperclass()) {
                try {
                    Field f = type.getDeclaredField(name);
                    f.setAccessible(true);
                    field = Optional.of(f);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    field = Optional.empty();
                }
            }
            cache.put(name, field);
        }
        if (field.isEmpty()) return null;
        try {
            return field.get().get(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            cache.put(name, Optional.empty());
            return null;
        }
    }

    static double fieldNumber(Object target, String name) {
        return field(target, name) instanceof Number n ? n.doubleValue() : 0.0;
    }

    static boolean started(Object target, String... animationFields) {
        for (String name : animationFields) {
            if (flag(field(target, name), "isStarted") > 0) return true;
        }
        return false;
    }

    static String enumName(Object value) {
        if (value instanceof Optional<?> o) value = o.orElse(null);
        if (value == null) return "";
        if (call(value, "unwrapKey") instanceof Optional<?> key && key.isPresent()) value = key.get();
        String text = value instanceof Enum<?> e ? e.name() : String.valueOf(value);
        int slash = text.lastIndexOf('/');
        if (slash >= 0) text = text.substring(slash + 1);
        int colon = text.lastIndexOf(':');
        if (colon >= 0) text = text.substring(colon + 1);
        int end = text.length();
        while (end > 0 && !Character.isLetterOrDigit(text.charAt(end - 1))) end--;
        return text.substring(0, end).trim().toLowerCase(Locale.ROOT);
    }

    static Object callWith(Object target, String name, Object... args) {
        if (target == null) return null;
        String key = name + "#" + args.length;
        Map<String, Method> cache = CACHE.get(target.getClass());
        Method m = cache.get(key);
        if (m == null) {
            m = MISSING;
            for (Method candidate : target.getClass().getMethods()) {
                if (candidate.getName().equals(name) && accepts(candidate.getParameterTypes(), args)) {
                    m = candidate;
                    break;
                }
            }
            cache.put(key, m);
        }
        if (m == MISSING) return null;
        try {
            return m.invoke(target, args);
        } catch (ReflectiveOperationException | RuntimeException e) {
            cache.put(key, MISSING);
            return null;
        }
    }

    private static boolean accepts(Class<?>[] types, Object[] args) {
        if (types.length != args.length) return false;
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null ? types[i].isPrimitive() : !boxed(types[i]).isInstance(args[i])) return false;
        }
        return true;
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == boolean.class) return Boolean.class;
        return Long.class;
    }
}

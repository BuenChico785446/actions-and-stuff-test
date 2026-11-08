package dev.bedrockmotion.client.runtime;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

final class BmKinetic {

    private static Object componentType;
    private static boolean searched;
    private static final Map<Class<?>, Method> GETTERS = new ConcurrentHashMap<>();

    private BmKinetic() {}

    static double[] read(Object entity) {
        Object stack = BmReflect.call(entity, "getMainHandItem");
        Object weapon = stack == null ? null : component(stack);
        if (weapon == null) return null;
        return new double[] {
            BmReflect.number(weapon, "delayTicks"),
            duration(weapon, "dismountConditions"),
            duration(weapon, "knockbackConditions"),
            duration(weapon, "damageConditions")
        };
    }

    private static double duration(Object weapon, String name) {
        Object value = BmReflect.call(weapon, name);
        Object condition = value instanceof Optional<?> optional ? optional.orElse(null) : value;
        return BmReflect.number(condition, "maxDurationTicks");
    }

    private static Object component(Object stack) {
        if (!searched) {
            searched = true;
            try {
                Class<?> components = Class.forName("net.minecraft.core.component.DataComponents");
                componentType = components.getField("KINETIC_WEAPON").get(null);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                componentType = null;
            }
        }
        if (componentType == null) return null;
        Method getter = GETTERS.computeIfAbsent(stack.getClass(), BmKinetic::findGetter);
        if (getter == null) return null;
        try {
            return getter.invoke(stack, componentType);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static Method findGetter(Class<?> type) {
        for (Method m : type.getMethods()) {
            if (m.getName().equals("get") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(componentType.getClass())) {
                return m;
            }
        }
        return null;
    }
}

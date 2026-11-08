package dev.bedrockmotion.molang;

@FunctionalInterface
public interface QueryFn {

    Object call(Env env, Object[] args);
}

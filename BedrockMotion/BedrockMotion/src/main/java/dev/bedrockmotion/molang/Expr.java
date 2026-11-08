package dev.bedrockmotion.molang;

public abstract class Expr {

    public abstract Object eval(Env env);

    public double num(Env env) {
        return Values.num(eval(env));
    }

    public boolean isConst() {
        return false;
    }
}

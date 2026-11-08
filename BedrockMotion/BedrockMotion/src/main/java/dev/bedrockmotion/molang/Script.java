package dev.bedrockmotion.molang;

import java.util.ArrayList;
import java.util.List;

public final class Script {

    private static final Script ZERO = constant(0);

    private final Expr root;
    private final String source;

    private Script(Expr root, String source) {
        this.root = root;
        this.source = source;
    }

    public static Script constant(double v) {
        return new Script(new Nodes.Const(v), Double.toString(v));
    }

    public static Script compile(String src, Symbols symbols, List<String> problems) {
        if (src == null || src.isBlank()) return ZERO;
        Script cached = symbols.scripts.get(src);
        if (cached != null) return cached;
        Script made = compileUncached(src, symbols, problems);
        Script prev = symbols.scripts.putIfAbsent(src, made);
        return prev != null ? prev : made;
    }

    private static Script compileUncached(String src, Symbols symbols, List<String> problems) {
        List<String> warnings = new ArrayList<>();
        try {
            Expr e = MolangParser.compile(src, symbols, warnings);
            if (problems != null) {
                for (String w : warnings) problems.add(w + " in: " + abbreviate(src));
            }
            return new Script(e, src);
        } catch (RuntimeException ex) {
            if (problems != null) problems.add(ex.getMessage() + " in: " + abbreviate(src));
            return new Script(new Nodes.Const(0), src);
        }
    }

    private static String abbreviate(String s) {
        return s.length() > 120 ? s.substring(0, 120) + "..." : s;
    }

    public String source() {
        return source;
    }

    public boolean isConstant() {
        return root.isConst();
    }

    public double constantValue() {
        return root.num(null);
    }

    public Object exec(Env env) {
        Object v = root.eval(env);
        if (env.ctl == Env.CTL_RETURN) v = env.ret;
        env.ctl = Env.CTL_NONE;
        env.ret = null;
        return v == null ? Values.ZERO : v;
    }

    public Object run(Env env) {
        env.beginScope();
        return exec(env);
    }

    public double runNum(Env env) {
        if (root instanceof Nodes.Const c) return c.v;
        return Values.num(run(env));
    }

    public double execNum(Env env) {
        if (root instanceof Nodes.Const c) return c.v;
        return Values.num(exec(env));
    }

    public String runString(Env env) {
        Object v = run(env);
        return v instanceof String s ? s : null;
    }

    public static void runAll(Env env, Script[] scripts) {
        env.beginScope();
        for (Script s : scripts) s.exec(env);
    }
}

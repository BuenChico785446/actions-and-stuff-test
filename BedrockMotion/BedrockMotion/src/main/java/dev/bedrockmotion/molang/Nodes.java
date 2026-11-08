package dev.bedrockmotion.molang;

final class Nodes {

    private Nodes() {}

    static final class Const extends Expr {
        final double v;
        final Double boxed;

        Const(double v) {
            this.v = v;
            this.boxed = v;
        }

        @Override
        public Object eval(Env env) {
            return boxed;
        }

        @Override
        public double num(Env env) {
            return v;
        }

        @Override
        public boolean isConst() {
            return true;
        }
    }

    static final class Str extends Expr {
        final String s;

        Str(String s) {
            this.s = s;
        }

        @Override
        public Object eval(Env env) {
            return s;
        }

        @Override
        public boolean isConst() {
            return true;
        }
    }

    static Object readPath(Object v, String[] path) {
        for (String key : path) {
            if (v instanceof Struct s) v = s.fields.get(key);
            else return null;
        }
        return v;
    }

    static Object writePath(Object root, String[] path, Object value) {
        Struct cur = root instanceof Struct s ? s : new Struct();
        Struct top = cur;
        for (int i = 0; i < path.length - 1; i++) {
            Object next = cur.fields.get(path[i]);
            Struct ns = next instanceof Struct s2 ? s2 : new Struct();
            cur.fields.put(path[i], ns);
            cur = ns;
        }
        cur.fields.put(path[path.length - 1], value);
        return top;
    }

    abstract static class Ref extends Expr {
        final String name;
        final String[] path;

        Ref(String name, String[] path) {
            this.name = name;
            this.path = path;
        }

        abstract Object get(Env env);

        abstract void set(Env env, Object value);

        @Override
        public Object eval(Env env) {
            Object v = get(env);
            return path.length == 0 ? v : readPath(v, path);
        }

        void assign(Env env, Object value) {
            if (path.length == 0) {
                set(env, value);
            } else {
                set(env, writePath(get(env), path, value));
            }
        }
    }

    static final class VarRef extends Ref {
        final Symbols symbols;
        final int id;

        VarRef(Symbols symbols, String name, String[] path) {
            super(name, path);
            this.symbols = symbols;
            this.id = symbols.varId(name);
        }

        private int idFor(Env env) {
            return env.symbols == symbols ? id : env.symbols.findVar(name);
        }

        @Override
        Object get(Env env) {
            int i = idFor(env);
            return i < 0 ? null : env.getVar(i);
        }

        @Override
        void set(Env env, Object value) {
            int i = env.symbols == symbols ? id : env.symbols.varId(name);
            env.setVar(i, value);
        }
    }

    static final class TempRef extends Ref {
        final int id;

        TempRef(Symbols symbols, String name, String[] path) {
            super(name, path);
            this.id = symbols.tempId(name);
        }

        @Override
        Object get(Env env) {
            return env.getTemp(id);
        }

        @Override
        void set(Env env, Object value) {
            env.setTemp(id, value);
        }
    }

    static final class ContextRef extends Expr {
        final String name;

        ContextRef(String name) {
            this.name = name;
        }

        @Override
        public Object eval(Env env) {
            return env.context.get(name);
        }
    }

    static final class QueryCall extends Expr {
        static final Object[] NO_ARGS = new Object[0];
        final QueryFn fn;
        final Expr[] args;

        QueryCall(QueryFn fn, Expr[] args) {
            this.fn = fn;
            this.args = args;
        }

        @Override
        public Object eval(Env env) {
            Object[] a = NO_ARGS;
            if (args.length > 0) {
                a = new Object[args.length];
                for (int i = 0; i < a.length; i++) a[i] = args[i].eval(env);
            }
            return fn.call(env, a);
        }
    }

    static final class ArrayRef extends Expr {
        final String name;
        final Expr index;

        ArrayRef(String name, Expr index) {
            this.name = name;
            this.index = index;
        }

        @Override
        public Object eval(Env env) {
            Object[] arr = env.arrays.get(name);
            if (arr == null || arr.length == 0) return index == null ? new Object[0] : Values.ZERO;
            if (index == null) return arr;
            int i = Math.floorMod((int) Math.floor(index.num(env)), arr.length);
            return arr[i];
        }
    }

    static final class This extends Expr {
        @Override
        public Object eval(Env env) {
            return env.thisValue;
        }

        @Override
        public double num(Env env) {
            return env.thisValue;
        }
    }

    static final class Neg extends Expr {
        final Expr a;

        Neg(Expr a) {
            this.a = a;
        }

        @Override
        public double num(Env env) {
            return -a.num(env);
        }

        @Override
        public Object eval(Env env) {
            return num(env);
        }
    }

    static final class Not extends Expr {
        final Expr a;

        Not(Expr a) {
            this.a = a;
        }

        @Override
        public Object eval(Env env) {
            return Values.bool(!Values.truthy(a.eval(env)));
        }
    }

    static final int ADD = 0, SUB = 1, MUL = 2, DIV = 3, LT = 4, LE = 5, GT = 6, GE = 7;

    static final class Arith extends Expr {
        final int op;
        final Expr l;
        final Expr r;

        Arith(int op, Expr l, Expr r) {
            this.op = op;
            this.l = l;
            this.r = r;
        }

        @Override
        public double num(Env env) {
            double a = l.num(env);
            double b = r.num(env);
            switch (op) {
                case ADD:
                    return a + b;
                case SUB:
                    return a - b;
                case MUL:
                    return a * b;
                case DIV:
                    return b == 0 ? 0 : a / b;
                case LT:
                    return a < b ? 1 : 0;
                case LE:
                    return a <= b ? 1 : 0;
                case GT:
                    return a > b ? 1 : 0;
                default:
                    return a >= b ? 1 : 0;
            }
        }

        @Override
        public Object eval(Env env) {
            return num(env);
        }
    }

    static final class Eq extends Expr {
        final boolean negate;
        final Expr l;
        final Expr r;

        Eq(boolean negate, Expr l, Expr r) {
            this.negate = negate;
            this.l = l;
            this.r = r;
        }

        @Override
        public Object eval(Env env) {
            boolean e = Values.equal(l.eval(env), r.eval(env));
            return Values.bool(e != negate);
        }
    }

    static final class And extends Expr {
        final Expr l;
        final Expr r;

        And(Expr l, Expr r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public Object eval(Env env) {
            return Values.bool(Values.truthy(l.eval(env)) && Values.truthy(r.eval(env)));
        }
    }

    static final class Or extends Expr {
        final Expr l;
        final Expr r;

        Or(Expr l, Expr r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public Object eval(Env env) {
            return Values.bool(Values.truthy(l.eval(env)) || Values.truthy(r.eval(env)));
        }
    }

    static final class Ternary extends Expr {
        final Expr cond;
        final Expr a;
        final Expr b;

        Ternary(Expr cond, Expr a, Expr b) {
            this.cond = cond;
            this.a = a;
            this.b = b;
        }

        @Override
        public Object eval(Env env) {
            if (Values.truthy(cond.eval(env))) return a.eval(env);
            return b == null ? Values.ZERO : b.eval(env);
        }
    }

    static final class Coalesce extends Expr {
        final Expr l;
        final Expr r;

        Coalesce(Expr l, Expr r) {
            this.l = l;
            this.r = r;
        }

        @Override
        public Object eval(Env env) {
            Object v = l.eval(env);
            return v != null ? v : r.eval(env);
        }
    }

    static final class Assign extends Expr {
        final Ref target;
        final Expr value;

        Assign(Ref target, Expr value) {
            this.target = target;
            this.value = value;
        }

        @Override
        public Object eval(Env env) {
            Object v = value.eval(env);
            if (v == null) v = Values.ZERO;
            target.assign(env, v);
            return v;
        }
    }

    static final class Seq extends Expr {
        final Expr[] stmts;
        final boolean valued;

        Seq(Expr[] stmts, boolean valued) {
            this.stmts = stmts;
            this.valued = valued;
        }

        @Override
        public Object eval(Env env) {
            Object last = Values.ZERO;
            for (Expr s : stmts) {
                last = s.eval(env);
                if (env.ctl != Env.CTL_NONE) return last;
            }
            return valued && last != null ? last : Values.ZERO;
        }
    }

    static final class Loop extends Expr {
        static final int MAX_ITERATIONS = 1024;
        final Expr count;
        final Expr body;

        Loop(Expr count, Expr body) {
            this.count = count;
            this.body = body;
        }

        @Override
        public Object eval(Env env) {
            int n = (int) Math.min(MAX_ITERATIONS, count.num(env));
            for (int i = 0; i < n; i++) {
                body.eval(env);
                if (env.ctl == Env.CTL_BREAK) {
                    env.ctl = Env.CTL_NONE;
                    break;
                }
                if (env.ctl == Env.CTL_CONTINUE) {
                    env.ctl = Env.CTL_NONE;
                } else if (env.ctl == Env.CTL_RETURN) {
                    break;
                }
            }
            return Values.ZERO;
        }
    }

    static final class ForEach extends Expr {
        final Ref target;
        final Expr array;
        final Expr body;

        ForEach(Ref target, Expr array, Expr body) {
            this.target = target;
            this.array = array;
            this.body = body;
        }

        @Override
        public Object eval(Env env) {
            Object a = array.eval(env);
            if (!(a instanceof Object[] items)) return Values.ZERO;
            int n = Math.min(items.length, Loop.MAX_ITERATIONS);
            for (int i = 0; i < n; i++) {
                target.assign(env, items[i]);
                body.eval(env);
                if (env.ctl == Env.CTL_BREAK) {
                    env.ctl = Env.CTL_NONE;
                    break;
                }
                if (env.ctl == Env.CTL_CONTINUE) {
                    env.ctl = Env.CTL_NONE;
                } else if (env.ctl == Env.CTL_RETURN) {
                    break;
                }
            }
            return Values.ZERO;
        }
    }

    static final class Flow extends Expr {
        final int kind;
        final Expr value;

        Flow(int kind, Expr value) {
            this.kind = kind;
            this.value = value;
        }

        @Override
        public Object eval(Env env) {
            if (kind == Env.CTL_RETURN) {
                Object v = value == null ? Values.ZERO : value.eval(env);
                env.ret = v == null ? Values.ZERO : v;
            }
            env.ctl = kind;
            return Values.ZERO;
        }
    }

    static final class Arrow extends Expr {
        final Expr left;
        final Expr right;

        Arrow(Expr left, Expr right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public Object eval(Env env) {
            Object o = left.eval(env);
            if (!(o instanceof Env other)) return Values.ZERO;
            Object v = right.eval(other);
            return v == null ? Values.ZERO : v;
        }
    }
}

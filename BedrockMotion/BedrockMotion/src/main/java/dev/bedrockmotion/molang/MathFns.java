package dev.bedrockmotion.molang;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

final class MathFns {

    private MathFns() {}

    interface D3 {
        double f(double a, double b, double c);
    }

    static final class M1 extends Expr {
        final Expr a;
        final DoubleUnaryOperator f;

        M1(Expr a, DoubleUnaryOperator f) {
            this.a = a;
            this.f = f;
        }

        @Override
        public double num(Env env) {
            return f.applyAsDouble(a.num(env));
        }

        @Override
        public Object eval(Env env) {
            return num(env);
        }
    }

    static final class M2 extends Expr {
        final Expr a;
        final Expr b;
        final DoubleBinaryOperator f;

        M2(Expr a, Expr b, DoubleBinaryOperator f) {
            this.a = a;
            this.b = b;
            this.f = f;
        }

        @Override
        public double num(Env env) {
            return f.applyAsDouble(a.num(env), b.num(env));
        }

        @Override
        public Object eval(Env env) {
            return num(env);
        }
    }

    static final class M3 extends Expr {
        final Expr a;
        final Expr b;
        final Expr c;
        final D3 f;

        M3(Expr a, Expr b, Expr c, D3 f) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.f = f;
        }

        @Override
        public double num(Env env) {
            return f.f(a.num(env), b.num(env), c.num(env));
        }

        @Override
        public Object eval(Env env) {
            return num(env);
        }
    }

    static final class Rand extends Expr {
        final Expr lo;
        final Expr hi;
        final boolean integer;

        Rand(Expr lo, Expr hi, boolean integer) {
            this.lo = lo;
            this.hi = hi;
            this.integer = integer;
        }

        @Override
        public double num(Env env) {
            double a = lo == null ? 0 : lo.num(env);
            double b = hi == null ? 1 : hi.num(env);
            if (integer) {
                long l = (long) Math.floor(Math.min(a, b));
                long h = (long) Math.floor(Math.max(a, b));
                return l + (long) Math.floor(env.random.nextDouble() * (h - l + 1));
            }
            return a + (b - a) * env.random.nextDouble();
        }

        @Override
        public Object eval(Env env) {
            return num(env);
        }
    }

    static final class DieRoll extends Expr {
        final Expr count;
        final Expr lo;
        final Expr hi;
        final boolean integer;

        DieRoll(Expr count, Expr lo, Expr hi, boolean integer) {
            this.count = count;
            this.lo = lo;
            this.hi = hi;
            this.integer = integer;
        }

        @Override
        public double num(Env env) {
            int n = (int) Math.min(1024, Math.max(0, count.num(env)));
            double a = lo.num(env);
            double b = hi.num(env);
            double sum = 0;
            for (int i = 0; i < n; i++) {
                double r = a + (b - a) * env.random.nextDouble();
                sum += integer ? Math.floor(r) : r;
            }
            return sum;
        }

        @Override
        public Object eval(Env env) {
            return num(env);
        }
    }

    static double rad(double d) {
        return d * (Math.PI / 180.0);
    }

    static double deg(double r) {
        return r * (180.0 / Math.PI);
    }

    static double minAngle(double a) {
        a = a % 360.0;
        if (a >= 180.0) a -= 360.0;
        if (a < -180.0) a += 360.0;
        return a;
    }

    static double round(double x) {
        return x < 0 ? -Math.floor(-x + 0.5) : Math.floor(x + 0.5);
    }

    static Expr build(String name, Expr[] a) {
        int n = a.length;
        switch (name) {
            case "pi":
                return new Nodes.Const(Math.PI);
            case "abs":
                return new M1(arg(a, 0), Math::abs);
            case "sin":
                return new M1(arg(a, 0), x -> Math.sin(rad(x)));
            case "cos":
                return new M1(arg(a, 0), x -> Math.cos(rad(x)));
            case "asin":
                return new M1(arg(a, 0), x -> deg(Math.asin(x)));
            case "acos":
                return new M1(arg(a, 0), x -> deg(Math.acos(x)));
            case "atan":
                return new M1(arg(a, 0), x -> deg(Math.atan(x)));
            case "atan2":
                return new M2(arg(a, 0), arg(a, 1), (y, x) -> deg(Math.atan2(y, x)));
            case "ceil":
                return new M1(arg(a, 0), Math::ceil);
            case "floor":
                return new M1(arg(a, 0), Math::floor);
            case "round":
                return new M1(arg(a, 0), MathFns::round);
            case "trunc":
                return new M1(arg(a, 0), x -> x < 0 ? Math.ceil(x) : Math.floor(x));
            case "sqrt":
                return new M1(arg(a, 0), Math::sqrt);
            case "exp":
                return new M1(arg(a, 0), Math::exp);
            case "ln":
                return new M1(arg(a, 0), Math::log);
            case "sign":
                return new M1(arg(a, 0), Math::signum);
            case "min_angle":
                return new M1(arg(a, 0), MathFns::minAngle);
            case "hermite_blend":
                return new M1(arg(a, 0), t -> 3 * t * t - 2 * t * t * t);
            case "pow":
                return new M2(arg(a, 0), arg(a, 1), Math::pow);
            case "min":
                return new M2(arg(a, 0), arg(a, 1), Math::min);
            case "max":
                return new M2(arg(a, 0), arg(a, 1), Math::max);
            case "mod":
                return new M2(arg(a, 0), arg(a, 1), (x, y) -> y == 0 ? 0 : x % y);
            case "copy_sign":
                return new M2(arg(a, 0), arg(a, 1), Math::copySign);
            case "clamp":
                return new M3(arg(a, 0), arg(a, 1), arg(a, 2), (v, lo, hi) -> Math.max(lo, Math.min(hi, v)));
            case "lerp":
                return new M3(arg(a, 0), arg(a, 1), arg(a, 2), (x, y, t) -> x + (y - x) * t);
            case "inverse_lerp":
                return new M3(arg(a, 0), arg(a, 1), arg(a, 2), (x, y, v) -> y == x ? 0 : (v - x) / (y - x));
            case "lerprotate":
                return new M3(arg(a, 0), arg(a, 1), arg(a, 2), (x, y, t) -> x + minAngle(y - x) * t);
            case "random":
                return new Rand(n > 0 ? a[0] : null, n > 1 ? a[1] : null, false);
            case "random_integer":
                return new Rand(n > 0 ? a[0] : null, n > 1 ? a[1] : null, true);
            case "die_roll":
                return new DieRoll(arg(a, 0), arg(a, 1), arg(a, 2), false);
            case "die_roll_integer":
                return new DieRoll(arg(a, 0), arg(a, 1), arg(a, 2), true);
            default:
                break;
        }
        if (name.startsWith("ease_")) {
            DoubleUnaryOperator e = Ease.byName(name.substring(5));
            if (e != null) {
                return new M3(arg(a, 0), arg(a, 1), arg(a, 2), (x, y, t) -> x + (y - x) * e.applyAsDouble(t));
            }
        }
        return null;
    }

    private static Expr arg(Expr[] a, int i) {
        return i < a.length ? a[i] : new Nodes.Const(0);
    }
}

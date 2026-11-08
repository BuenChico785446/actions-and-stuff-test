package dev.bedrockmotion;

import dev.bedrockmotion.json.Json;
import dev.bedrockmotion.molang.Env;
import dev.bedrockmotion.molang.Script;
import dev.bedrockmotion.molang.Symbols;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MolangSelfTest {

    private static int failures;
    private static final Symbols SYMBOLS = new Symbols();

    public static void main(String[] args) throws Exception {
        basics();
        control();
        if (args.length > 0) corpus(Path.of(args[0]));
        System.out.println(failures == 0 ? "MolangSelfTest: all tests passed" : "MolangSelfTest: " + failures + " errors");
        if (failures > 0) System.exit(1);
    }

    private static double eval(String src) {
        List<String> problems = new ArrayList<>();
        Script s = Script.compile(src, SYMBOLS, problems);
        if (!problems.isEmpty()) System.out.println("  Problem: " + problems);
        Env env = new Env(SYMBOLS);
        env.animTime = 2;
        return s.runNum(env);
    }

    private static void check(String src, double expected) {
        double v = eval(src);
        if (Math.abs(v - expected) > 1e-9) {
            failures++;
            System.out.println("ERROR: " + src + " -> " + v + ", expected " + expected);
        }
    }

    private static void basics() {
        check("1+2*3", 7);
        check("(1+2)*3", 9);
        check("10/4", 2.5);
        check("1/0", 0);
        check("-3+1", -2);
        check("1.0E-4*10000", 1);
        check("Math.sin(90)", 1);
        check("math.cos(180)", -1);
        check("Math.atan2(1,1)", 45);
        check("Math.clamp(5,0,3)", 3);
        check("Math.lerp(10,20,0.5)", 15);
        check("Math.lerprotate(350,10,0.5)", 360);
        check("Math.min_angle(270)", -90);
        check("Math.mod(7,3)", 1);
        check("Math.round(2.5)", 3);
        check("Math.round(-2.5)", -3);
        check("Math.hermite_blend(0.5)", 0.5);
        check("Math.ease_out_cubic(0,10,1)", 10);
        check("Math.pi", Math.PI);
        check("1<2&&2<3", 1);
        check("1>2||0", 0);
        check("!0", 1);
        check("1==1", 1);
        check("'abc'=='ABC'", 1);
        check("'abc'!='abd'", 1);
        check("1?5:6", 5);
        check("0?5:6", 6);
        check("0?5", 0);
        check("1?2:3?4:5", 2);
        check("q.anim_time*2", 4);
        check("query.anim_time+1", 3);
        check("v.unset??7", 7);
        check("(v.unset??7)+1", 8);
        check("v.a=3;v.a*2", 6);
        check("v.a=3;return v.a*2;", 6);
        check("v.a=3;v.a*2;", 0);
        check("t.x=2;t.y=t.x*t.x;return t.y;", 4);
        check("v.s.x=4;v.s.y=5;return v.s.x+v.s.y;", 9);
        check("v.str='hello';v.str=='HELLO'", 1);
        check("v.a=1;v.a=v.a?5:6;v.a", 5);
        check("Math.max(1,Math.min(5,3))", 3);
        check("Math.abs(-4.5)", 4.5);
        check("Math.trunc(-2.7)", -2);
        check("Math.floor(2.7)+Math.ceil(2.1)", 5);
        check("Math.sqrt(16)+Math.pow(2,3)", 12);
        check("Math.exp(0)+Math.ln(1)", 1);
        check("Math.sign(-3)", -1);
    }

    private static void control() {
        check("t.s=0;loop(5,{t.s=t.s+2;});return t.s;", 10);
        check("t.i=0;loop(100,{t.i=t.i+1;t.i>=7?{break;};});return t.i;", 7);
        check("t.i=0;t.s=0;loop(6,{t.i=t.i+1;t.i<4?{continue;};t.s=t.s+t.i;});return t.s;", 15);
        check("1?{return 9;};return 1;", 9);
        check("t.n=0;loop(5000,{t.n=t.n+1;});return t.n;", 1024);
        check("t.x=1;t.x==1?{t.y=10;}:{t.y=20;};return t.y;", 10);
        check("t.a=2;t.b=t.a*3;t.a>1?{t.b=t.b+1;};t.b", 7);
        check("Math.random(5,5)", 5);
        check("Math.random_integer(3,3)", 3);
        double r = eval("Math.random(0,1)");
        if (r < 0 || r >= 1) {
            failures++;
            System.out.println("ERROR: random out of range " + r);
        }
        check("v.k=Math.random_integer(0,2);v.k>=0&&v.k<=2", 1);
    }

    @SuppressWarnings("unchecked")
    private static void corpus(Path file) throws Exception {
        List<Object> all = Json.arr(Json.parse(Files.readString(file)));
        Symbols symbols = new Symbols();
        int bad = 0;
        long t0 = System.nanoTime();
        List<Script> scripts = new ArrayList<>();
        for (Object o : all) {
            List<String> problems = new ArrayList<>();
            scripts.add(Script.compile((String) o, symbols, problems));
            for (String p : problems) {
                bad++;
                if (bad <= 15) System.out.println("  " + p);
            }
        }
        long t1 = System.nanoTime();
        System.out.printf("Corpus: %d expressions, %d problems, %d v variables, %d t variables, %d ms%n",
                all.size(), bad, symbols.varCount(), symbols.tempCount(), (t1 - t0) / 1_000_000);
        Env env = new Env(symbols);
        env.entity.position[1] = 64;
        int thrown = 0;
        long t2 = System.nanoTime();
        for (Script s : scripts) {
            try {
                s.run(env);
            } catch (RuntimeException ex) {
                thrown++;
                if (thrown <= 10) System.out.println("  Runtime error: " + ex + " in " + s.source().substring(0, Math.min(80, s.source().length())));
            }
        }
        long t3 = System.nanoTime();
        System.out.printf("Corpus executed: %d runtime errors, %d ms%n", thrown, (t3 - t2) / 1_000_000);
        if (bad > 0 || thrown > 0) failures++;
    }
}

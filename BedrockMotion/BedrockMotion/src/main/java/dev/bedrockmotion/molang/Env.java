package dev.bedrockmotion.molang;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class Env {

    public static final int CTL_NONE = 0;
    public static final int CTL_BREAK = 1;
    public static final int CTL_CONTINUE = 2;
    public static final int CTL_RETURN = 3;

    public final Symbols symbols;
    public EntitySnapshot entity = new EntitySnapshot();
    public final Map<String, Object> context = new HashMap<>();
    public final Random random = new Random();
    public Map<String, Object[]> arrays = Map.of();
    public WorldProbe probe = WorldProbe.NONE;
    public BoneSolver bones;
    public PackSettings settings = PackSettings.NONE;
    public Object[] vars = new Object[0];
    public Env owner;
    public Set<String> ownerPublic = Set.of();
    public double animTime;
    public double lifeTime;
    public double deltaTime;
    public double stateTime;
    public double frameAlpha;
    public double timeStamp;
    public double animationFrame;
    public double keyFrameLerpTime;
    public boolean anyAnimationFinished;
    public boolean allAnimationsFinished;
    public double thisValue;
    public int ctl;
    public Object ret;
    private Object[] temps = new Object[0];
    private int[] tempGen = new int[0];
    private int gen = 1;

    public Env(Symbols symbols) {
        this.symbols = symbols;
        this.vars = new Object[Math.max(16, symbols.varCount())];
    }

    public Object getVar(int id) {
        return id < vars.length ? vars[id] : null;
    }

    public void setVar(int id, Object value) {
        if (id >= vars.length) {
            Object[] n = new Object[Math.max(id + 1, symbols.varCount())];
            System.arraycopy(vars, 0, n, 0, vars.length);
            vars = n;
        }
        vars[id] = value;
    }

    public void beginScope() {
        gen++;
        ctl = CTL_NONE;
        ret = null;
    }

    public Object getTemp(int id) {
        return id < tempGen.length && tempGen[id] == gen ? temps[id] : null;
    }

    public void setTemp(int id, Object value) {
        if (id >= temps.length) {
            int size = Math.max(id + 1, Math.max(symbols.tempCount(), temps.length * 2));
            Object[] nt = new Object[size];
            int[] ng = new int[size];
            System.arraycopy(temps, 0, nt, 0, temps.length);
            System.arraycopy(tempGen, 0, ng, 0, tempGen.length);
            temps = nt;
            tempGen = ng;
        }
        temps[id] = value;
        tempGen[id] = gen;
    }
}

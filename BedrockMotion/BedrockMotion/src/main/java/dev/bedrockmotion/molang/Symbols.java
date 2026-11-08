package dev.bedrockmotion.molang;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Symbols {

    private final Map<String, Integer> vars = new HashMap<>();
    private final Map<String, Integer> temps = new HashMap<>();
    final Map<String, Script> scripts = new ConcurrentHashMap<>();

    public synchronized int varId(String name) {
        Integer id = vars.get(name);
        if (id == null) {
            id = vars.size();
            vars.put(name, id);
        }
        return id;
    }

    public synchronized int tempId(String name) {
        Integer id = temps.get(name);
        if (id == null) {
            id = temps.size();
            temps.put(name, id);
        }
        return id;
    }

    public synchronized int findVar(String name) {
        Integer id = vars.get(name);
        return id == null ? -1 : id;
    }

    public synchronized int varCount() {
        return vars.size();
    }

    public synchronized int tempCount() {
        return temps.size();
    }
}

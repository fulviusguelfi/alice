package com.projetoalice.alice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Ring buffer de eventos comportamentais da Alice — rule switches, movements,
 * goals, chat, combate, falhas de LLM. Permite que a Alice responda perguntas
 * tipo "o que você estava fazendo?" e injeta contexto no prompt do LLM.
 * Thread-safe via synchronized — rule engine e log de movement rodam na server
 * thread, mas LLM worker le de async.
 */
public class BehaviorJournal {

    public static final int CAPACITY = 50;

    public enum Type { RULE, MOVEMENT, GOAL, COMBAT, CHAT, LLM, SYSTEM, STUCK }

    public static final class Event {
        public final long tick;
        public final long epochMs;
        public final Type type;
        public final String what;
        public final String why;

        public Event(long tick, long epochMs, Type type, String what, String why) {
            this.tick = tick;
            this.epochMs = epochMs;
            this.type = type;
            this.what = what == null ? "" : what;
            this.why = why == null ? "" : why;
        }
    }

    private final Deque<Event> buffer = new ArrayDeque<>(CAPACITY);
    private long tickCounter;
    private String lastRuleFired;
    private String lastMovement;

    public synchronized void incrementTick() {
        tickCounter++;
    }

    public synchronized long currentTick() {
        return tickCounter;
    }

    public synchronized void record(Type type, String what, String why) {
        if (buffer.size() >= CAPACITY) buffer.removeFirst();
        buffer.addLast(new Event(tickCounter, System.currentTimeMillis(), type, what, why));
    }

    /**
     * Record a rule switch only when the rule actually changes — avoids spam
     * when the same rule keeps firing every tick.
     */
    public synchronized void recordRuleSwitch(String newRule, String why) {
        if (java.util.Objects.equals(newRule, lastRuleFired)) return;
        lastRuleFired = newRule;
        record(Type.RULE, newRule == null ? "<none>" : newRule, why);
    }

    /**
     * Record a movement change only when the movement type or destination differs.
     */
    public synchronized void recordMovement(String movementType, String src, String dest, double cost) {
        String key = movementType + " " + src + "->" + dest;
        if (key.equals(lastMovement)) return;
        lastMovement = key;
        record(Type.MOVEMENT, movementType,
                String.format("de %s para %s (custo %.2f)", src, dest, cost));
    }

    public synchronized List<Event> recent(int n) {
        List<Event> out = new ArrayList<>();
        int skip = Math.max(0, buffer.size() - n);
        int i = 0;
        for (Event e : buffer) {
            if (i++ < skip) continue;
            out.add(e);
        }
        return out;
    }

    /**
     * PT-BR summary — used both for chat introspection and for LLM prompt injection.
     */
    public synchronized String summaryPtBr(int n) {
        List<Event> items = recent(n);
        if (items.isEmpty()) return "Nao fiz nada ainda — acabei de acordar.";
        StringBuilder sb = new StringBuilder();
        long now = System.currentTimeMillis();
        for (Event e : items) {
            long ageSec = (now - e.epochMs) / 1000;
            sb.append("- ha ").append(ageSec).append("s [").append(e.type.name().toLowerCase())
                    .append("] ").append(e.what);
            if (!e.why.isEmpty()) sb.append(" (porque: ").append(e.why).append(")");
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * Compact summary for LLM system prompt — one-line per event, last 10.
     */
    public synchronized String compactForLlm() {
        List<Event> items = recent(10);
        if (items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        long now = System.currentTimeMillis();
        for (Event e : items) {
            long ageSec = (now - e.epochMs) / 1000;
            sb.append(ageSec).append("s: ").append(e.type.name().toLowerCase())
                    .append("=").append(e.what);
            if (!e.why.isEmpty()) sb.append(" [").append(e.why).append("]");
            sb.append(" | ");
        }
        return sb.toString();
    }

    public synchronized void clear() {
        buffer.clear();
        lastRuleFired = null;
        lastMovement = null;
    }
}

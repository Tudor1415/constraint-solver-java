package csp.model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo log for domain changes.
 *
 * <p>The search opens a new level before each decision. The first time a variable's
 * domain is modified within a level, a copy of the old domain is pushed. Undoing a
 * level restores exactly those domains, and nothing else, whatever the solver pruned.
 */
public final class Trail {

    private record Entry(Variable variable, Domain oldDomain, int oldStamp) {
    }

    private final Deque<Entry> entries = new ArrayDeque<>();
    private final Deque<Integer> marks = new ArrayDeque<>();
    private int level;

    /** Opens a new level (call before each decision). */
    public void push() {
        marks.push(entries.size());
        level++;
    }

    /** Undoes every change made since the matching {@link #push()}. */
    public void pop() {
        int mark = marks.pop();
        while (entries.size() > mark) {
            Entry e = entries.pop();
            e.variable().restore(e.oldDomain(), e.oldStamp());
        }
        level--;
    }

    public int level() {
        return level;
    }

    /** Called by a variable before it modifies its domain. */
    void save(Variable v) {
        if (v.stamp() != level) {
            entries.push(new Entry(v, v.domain().copy(), v.stamp()));
            v.setStamp(level);
        }
    }
}

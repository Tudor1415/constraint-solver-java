package csp.model;

/** x &lt; y. Only the bounds matter: x &lt; max(y) and y &gt; min(x). */
public final class LessThan extends Constraint {

    public LessThan(Variable x, Variable y) {
        super(x, y);
    }

    @Override
    public boolean holds(int a, int b) {
        return a < b;
    }

    @Override
    public boolean isPossible() {
        return !x.domain().isEmpty() && !y.domain().isEmpty() && x.domain().min() < y.domain().max();
    }

    @Override
    public boolean filter() {
        boolean changed = filterTowards(x);
        return filterTowards(y) | changed;
    }

    @Override
    public boolean filterTowards(Variable target) {
        Variable source = other(target);
        if (source.domain().isEmpty()) {
            return false;
        }
        if (target == x) {
            int ymax = y.domain().max();
            return ymax == Integer.MIN_VALUE ? wipeOut(x) : x.removeAbove(ymax - 1);
        }
        int xmin = x.domain().min();
        return xmin == Integer.MAX_VALUE ? wipeOut(y) : y.removeBelow(xmin + 1);
    }

    /** No value is below Integer.MIN_VALUE or above Integer.MAX_VALUE: the domain becomes empty. */
    private static boolean wipeOut(Variable v) {
        return v.removeAll();
    }

    @Override
    protected String symbol() {
        return "<";
    }
}

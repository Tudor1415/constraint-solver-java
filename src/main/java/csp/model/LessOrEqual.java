package csp.model;

/** x ≤ y. Only the bounds matter: x ≤ max(y) and y ≥ min(x). */
public final class LessOrEqual extends Constraint {

    public LessOrEqual(Variable x, Variable y) {
        super(x, y);
    }

    @Override
    public boolean holds(int a, int b) {
        return a <= b;
    }

    @Override
    public boolean isPossible() {
        return !x.domain().isEmpty() && !y.domain().isEmpty() && x.domain().min() <= y.domain().max();
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
        return target == x ? x.removeAbove(y.domain().max()) : y.removeBelow(x.domain().min());
    }

    @Override
    protected String symbol() {
        return "≤";
    }
}

package csp.model;

/** x ≠ y. A value loses its last partner only when the other variable is fixed to it. */
public final class NotEqual extends Constraint {

    public NotEqual(Variable x, Variable y) {
        super(x, y);
    }

    @Override
    public boolean holds(int a, int b) {
        return a != b;
    }

    @Override
    public boolean isPossible() {
        if (x.domain().isEmpty() || y.domain().isEmpty()) {
            return false;
        }
        return !(x.isInstantiated() && y.isInstantiated() && x.value() == y.value());
    }

    @Override
    public boolean filter() {
        boolean changed = filterTowards(x);
        return filterTowards(y) | changed;
    }

    @Override
    public boolean filterTowards(Variable target) {
        Variable source = other(target);
        return source.isInstantiated() && target.remove(source.value());
    }

    @Override
    protected String symbol() {
        return "≠";
    }
}

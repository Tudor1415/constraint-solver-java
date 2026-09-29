package csp.model;

/** x = y. */
public final class Equal extends Constraint {

    public Equal(Variable x, Variable y) {
        super(x, y);
    }

    @Override
    public boolean holds(int a, int b) {
        return a == b;
    }

    @Override
    public boolean isPossible() {
        Domain small = x.domain().size() <= y.domain().size() ? x.domain() : y.domain();
        Domain large = small == x.domain() ? y.domain() : x.domain();
        for (int v : small.values()) {
            if (large.contains(v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean filter() {
        boolean changed = x.retainAll(y.domain());
        return y.retainAll(x.domain()) | changed;
    }

    @Override
    public boolean filterTowards(Variable target) {
        return target.retainAll(other(target).domain());
    }

    @Override
    protected String symbol() {
        return "=";
    }
}

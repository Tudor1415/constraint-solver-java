package csp.model;

import java.util.List;

/**
 * A relation that must hold between two variables {@code x} and {@code y}.
 *
 * <p>Subclasses define the relation and how to filter domains with it.
 */
public abstract class Constraint {

    protected final Variable x;
    protected final Variable y;
    private int id = -1; // position in its Csp, set when added

    protected Constraint(Variable x, Variable y) {
        if (x == y) {
            throw new IllegalArgumentException("a binary constraint needs two distinct variables");
        }
        this.x = x;
        this.y = y;
    }

    public final List<Variable> variables() {
        return List.of(x, y);
    }

    /** Position of the constraint in its problem (used by propagation queues). */
    public final int id() {
        return id;
    }

    final void setId(int id) {
        this.id = id;
    }

    /** The other variable of the constraint. */
    public final Variable other(Variable v) {
        return v == x ? y : x;
    }

    /** Does the pair (a, b) satisfy the relation, with a for x and b for y? */
    public abstract boolean holds(int a, int b);

    /** True iff both variables are instantiated. */
    public final boolean allInstantiated() {
        return x.isInstantiated() && y.isInstantiated();
    }

    /** True iff both variables are instantiated and their values satisfy the relation. */
    public final boolean isSatisfied() {
        return allInstantiated() && holds(x.value(), y.value());
    }

    /**
     * True iff some pair of values still in the two domains satisfies the relation,
     * i.e. the constraint can still be satisfied. A necessary condition only.
     */
    public abstract boolean isPossible();

    /**
     * Removes the values of x and y that have no partner in the other domain
     * (arc consistency). Returns true if a domain changed; a domain may become empty.
     */
    public abstract boolean filter();

    /**
     * Removes the values of {@code target} that have no partner in the other variable's
     * domain (one direction of {@link #filter()}). Returns true if the domain changed.
     */
    public abstract boolean filterTowards(Variable target);

    /** Symbol used by {@link #toString()}. */
    protected abstract String symbol();

    @Override
    public String toString() {
        return x.name() + " " + symbol() + " " + y.name();
    }
}

package csp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A constraint satisfaction problem: variables with finite domains and binary constraints.
 *
 * <pre>{@code
 * Csp csp = new Csp();
 * Variable a = csp.variable("a", 0, 2), b = csp.variable("b", 0, 2);
 * csp.lt(a, b);
 * }</pre>
 */
public final class Csp {

    private final List<Variable> variables = new ArrayList<>();
    private final List<Constraint> constraints = new ArrayList<>();
    private final Trail trail = new Trail();

    /** Creates a variable with domain {min, ..., max}. */
    public Variable variable(String name, int min, int max) {
        Variable v = new Variable(name, variables.size(), new Domain(min, max), trail);
        variables.add(v);
        return v;
    }

    public Constraint add(Constraint c) {
        for (Variable v : c.variables()) { // validate everything before changing anything
            if (v.index() >= variables.size() || variables.get(v.index()) != v) {
                throw new IllegalArgumentException(v.name() + " does not belong to this problem");
            }
        }
        for (Variable v : c.variables()) {
            v.addConstraint(c);
        }
        c.setId(constraints.size());
        constraints.add(c);
        return c;
    }

    public Constraint eq(Variable a, Variable b) {
        return add(new Equal(a, b));
    }

    public Constraint neq(Variable a, Variable b) {
        return add(new NotEqual(a, b));
    }

    public Constraint lt(Variable a, Variable b) {
        return add(new LessThan(a, b));
    }

    public Constraint leq(Variable a, Variable b) {
        return add(new LessOrEqual(a, b));
    }

    /** a &gt; b, stored as b &lt; a. */
    public Constraint gt(Variable a, Variable b) {
        return add(new LessThan(b, a));
    }

    /** a ≥ b, stored as b ≤ a. */
    public Constraint geq(Variable a, Variable b) {
        return add(new LessOrEqual(b, a));
    }

    public List<Variable> variables() {
        return Collections.unmodifiableList(variables);
    }

    public List<Constraint> constraints() {
        return Collections.unmodifiableList(constraints);
    }

    public Trail trail() {
        return trail;
    }

    /** True iff every variable is instantiated. */
    public boolean allInstantiated() {
        return variables.stream().allMatch(Variable::isInstantiated);
    }

    /** True iff every variable is instantiated and every constraint is satisfied. */
    public boolean isSolution() {
        return allInstantiated() && constraints.stream().allMatch(Constraint::isSatisfied);
    }

    /** The current values, in variable order (all variables must be instantiated). */
    public int[] values() {
        return variables.stream().mapToInt(Variable::value).toArray();
    }

    @Override
    public String toString() {
        return variables.stream().map(Variable::toString).collect(Collectors.joining(", ", "variables: ", "\n"))
                + constraints.stream().map(Constraint::toString).collect(Collectors.joining(", ", "constraints: ", ""));
    }
}

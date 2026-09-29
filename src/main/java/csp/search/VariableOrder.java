package csp.search;

/** Which unassigned variable to branch on next. */
public enum VariableOrder {
    /** The first unassigned variable, in the order the variables were declared. */
    INPUT,
    /** The unassigned variable with the fewest remaining values (ties: declaration order). */
    SMALLEST_DOMAIN
}

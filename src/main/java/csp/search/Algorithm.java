package csp.search;

/** The search algorithms, from the most naive to the most informed. */
public enum Algorithm {
    /** Enumerates every complete assignment and ignores the constraints. */
    BRUTE_FORCE("Brute force"),
    /** Enumerates every complete assignment and keeps those satisfying all constraints. */
    GENERATE_AND_TEST("Generate and test"),
    /** Checks a constraint as soon as both of its variables are assigned. */
    BACKTRACKING("Backtracking"),
    /** Also checks that each constraint of the new variable can still be satisfied by the remaining values. */
    LOOK_AHEAD("Backtracking + look-ahead"),
    /** Removes from the neighbours' domains the values incompatible with the new assignment. */
    FORWARD_CHECKING("Forward checking"),
    /** Maintains arc consistency: propagates removals until no domain can shrink further. */
    MAC("Full propagation (MAC)");

    private final String label;

    Algorithm(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

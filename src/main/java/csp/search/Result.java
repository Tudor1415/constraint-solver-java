package csp.search;

/**
 * Outcome of a search.
 *
 * @param solutions     solutions found (all of them, unless the search was stopped)
 * @param nodes         assignments tried (the size of the search tree)
 * @param checks        constraint evaluations (tests and filtering calls)
 * @param millis        wall-clock time in milliseconds
 * @param complete      true if the whole search tree was explored
 * @param timedOut      true if the time limit stopped the search (false when the solution limit did)
 * @param firstSolution values of the first solution found, in variable order, or null
 */
public record Result(Algorithm algorithm, VariableOrder order, long solutions, long nodes, long checks,
                     double millis, boolean complete, boolean timedOut, int[] firstSolution) {
}

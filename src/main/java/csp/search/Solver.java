package csp.search;

import csp.model.Constraint;
import csp.model.Csp;
import csp.model.Trail;
import csp.model.Variable;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Depth-first search for the solutions of a {@link Csp}.
 *
 * <p>All algorithms share the same skeleton: pick an unassigned variable, try each
 * value of its domain in increasing order, check the partial assignment, recurse,
 * then undo every domain change with the {@link Trail}. They differ only in the
 * check made after each assignment (see {@link Algorithm}).
 *
 * <p>The search keeps its own record of which variables it has assigned. A domain
 * reduced to one value by filtering does not count as an assignment, so every
 * constraint is checked with both of its variables assigned by the search itself.
 */
public final class Solver {

    private static final int CLOCK_EVERY = 1 << 12; // look at the clock every 4096 nodes

    private final Algorithm algorithm;
    private final VariableOrder order;

    private Csp csp;
    private boolean[] assigned;
    private long maxSolutions;
    private long deadline;
    private long solutions, nodes, checks;
    private boolean stopped, timedOut;
    private int[] firstSolution;

    public Solver(Algorithm algorithm, VariableOrder order) {
        this.algorithm = algorithm;
        this.order = order;
    }

    public Solver(Algorithm algorithm) {
        this(algorithm, VariableOrder.INPUT);
    }

    /** Finds all solutions. */
    public Result solve(Csp csp) {
        return solve(csp, 0, 0);
    }

    /**
     * Searches for solutions.
     *
     * @param maxSolutions stop after this many solutions (0: find them all)
     * @param timeLimitMillis stop after this much time (0: no limit)
     */
    public Result solve(Csp csp, long maxSolutions, long timeLimitMillis) {
        this.csp = csp;
        this.assigned = new boolean[csp.variables().size()];
        this.maxSolutions = maxSolutions;
        this.solutions = this.nodes = this.checks = 0;
        this.stopped = this.timedOut = false;
        this.firstSolution = null;
        long start = System.nanoTime();
        this.deadline = timeLimitMillis > 0 ? start + timeLimitMillis * 1_000_000L : Long.MAX_VALUE;

        Trail trail = csp.trail();
        trail.push();
        try {
            boolean consistent = algorithm != Algorithm.MAC || propagate(new ArrayDeque<>(csp.constraints()));
            if (consistent) {
                search(0);
            }
        } finally {
            trail.pop(); // the problem is left exactly as it was given
        }

        double millis = (System.nanoTime() - start) / 1e6;
        return new Result(algorithm, order, solutions, nodes, checks, millis, !stopped, timedOut, firstSolution);
    }

    private void search(int depth) {
        if (depth == assigned.length) {
            if (algorithm == Algorithm.GENERATE_AND_TEST && !allSatisfied()) {
                return;
            }
            if (solutions++ == 0) {
                firstSolution = csp.values();
            }
            if (maxSolutions > 0 && solutions >= maxSolutions) {
                stopped = true;
            }
            return;
        }
        Variable v = select();
        assigned[v.index()] = true;
        Trail trail = csp.trail();
        for (int value : v.domain().values()) {
            if (stopped) {
                break;
            }
            if (nodes % CLOCK_EVERY == 0 && System.nanoTime() > deadline) {
                stopped = timedOut = true;
                break;
            }
            nodes++;
            trail.push();
            try {
                v.instantiate(value);
                if (check(v)) {
                    search(depth + 1);
                }
            } finally {
                trail.pop();
            }
        }
        assigned[v.index()] = false;
    }

    private Variable select() {
        Variable best = null;
        for (Variable v : csp.variables()) {
            if (assigned[v.index()]) {
                continue;
            }
            if (order == VariableOrder.INPUT) {
                return v;
            }
            if (best == null || v.domain().size() < best.domain().size()) {
                best = v;
            }
        }
        return best;
    }

    /** The check made right after v has been assigned: true if the search may go deeper. */
    private boolean check(Variable v) {
        switch (algorithm) {
            case BRUTE_FORCE, GENERATE_AND_TEST:
                return true;
            case BACKTRACKING:
                for (Constraint c : v.constraints()) {
                    if (assigned[c.other(v).index()]) {
                        checks++;
                        if (!c.isSatisfied()) {
                            return false;
                        }
                    }
                }
                return true;
            case LOOK_AHEAD:
                for (Constraint c : v.constraints()) {
                    checks++;
                    if (!c.isPossible()) {
                        return false;
                    }
                }
                return true;
            case FORWARD_CHECKING:
                for (Constraint c : v.constraints()) {
                    Variable u = c.other(v);
                    checks++;
                    if (assigned[u.index()]) {
                        if (!c.isSatisfied()) {
                            return false;
                        }
                    } else {
                        c.filterTowards(u);
                        if (u.domain().isEmpty()) {
                            return false;
                        }
                    }
                }
                return true;
            case MAC:
                return propagate(new ArrayDeque<>(v.constraints()));
            default:
                throw new AssertionError(algorithm);
        }
    }

    /**
     * AC-3: filters constraints until no domain changes. When a domain shrinks, the
     * other constraints on that variable are queued again. Returns false on a wipe-out.
     */
    private boolean propagate(ArrayDeque<Constraint> queue) {
        List<Constraint> all = csp.constraints();
        boolean[] queued = new boolean[all.size()];
        for (Constraint c : queue) {
            queued[c.id()] = true;
        }
        while (!queue.isEmpty()) {
            Constraint c = queue.poll();
            queued[c.id()] = false;
            checks++;
            for (Variable target : c.variables()) {
                if (c.filterTowards(target)) {
                    if (target.domain().isEmpty()) {
                        return false;
                    }
                    for (Constraint d : target.constraints()) {
                        if (d != c && !queued[d.id()]) {
                            queued[d.id()] = true;
                            queue.add(d);
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean allSatisfied() {
        for (Constraint c : csp.constraints()) {
            checks++;
            if (!c.isSatisfied()) {
                return false;
            }
        }
        return true;
    }
}

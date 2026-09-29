package csp.problems;

import csp.model.Csp;
import csp.model.Variable;

import java.util.Map;

/**
 * The problems of the IMT Atlantique constraint-programming course, with the solution
 * counts published by the course (used as ground truth by the tests).
 */
public final class CourseProblems {

    private CourseProblems() {
    }

    /** Three variables in {0, 1, 2}, no constraint: 27 solutions. */
    public static Csp csp1() {
        Csp csp = new Csp();
        for (int i = 0; i < 3; i++) {
            csp.variable("x" + i, 0, 2);
        }
        return csp;
    }

    /** csp1 with x0 &lt; x1 and x0 ≠ x2: 6 solutions. */
    public static Csp csp2() {
        Csp csp = csp1();
        Variable[] x = csp.variables().toArray(Variable[]::new);
        csp.lt(x[0], x[1]);
        csp.neq(x[0], x[2]);
        return csp;
    }

    /** x0 ∈ [2, 6], x1 ∈ [4, 7], x2 ∈ [6, 9], x2 ≤ x1 ≤ x0: 1 solution (6, 6, 6). */
    public static Csp csp3() {
        Csp csp = new Csp();
        Variable x0 = csp.variable("x0", 2, 6);
        Variable x1 = csp.variable("x1", 4, 7);
        Variable x2 = csp.variable("x2", 6, 9);
        csp.leq(x2, x1);
        csp.leq(x1, x0);
        return csp;
    }

    /** "Problem 2013": 10 variables in [1, valMax], 17 constraints. */
    public static Csp problem2013(int valMax) {
        Csp csp = new Csp();
        Variable[] x = new Variable[10];
        for (int i = 0; i < 10; i++) {
            x[i] = csp.variable("x" + i, 1, valMax);
        }
        csp.leq(x[9], x[0]);
        csp.leq(x[9], x[2]);
        csp.lt(x[9], x[4]);
        csp.lt(x[0], x[1]);
        csp.lt(x[0], x[4]);
        csp.leq(x[0], x[7]);
        csp.leq(x[1], x[6]);
        csp.neq(x[2], x[4]);
        csp.neq(x[2], x[5]);
        csp.neq(x[4], x[5]);
        csp.lt(x[4], x[6]);
        csp.lt(x[4], x[3]);
        csp.neq(x[7], x[8]);
        csp.leq(x[5], x[8]);
        csp.neq(x[6], x[3]);
        csp.lt(x[8], x[6]);
        csp.leq(x[3], x[8]);
        return csp;
    }

    /** The course's additional test problem: 10 variables in [1, valMax], 15 constraints. */
    public static Csp problem00(int valMax) {
        Csp csp = new Csp();
        Variable[] x = new Variable[10];
        for (int i = 0; i < 10; i++) {
            x[i] = csp.variable("x" + i, 1, valMax);
        }
        csp.lt(x[0], x[5]);
        csp.leq(x[0], x[6]);
        csp.gt(x[0], x[7]);
        csp.leq(x[1], x[6]);
        csp.lt(x[2], x[3]);
        csp.neq(x[2], x[4]);
        csp.geq(x[3], x[8]);
        csp.leq(x[4], x[6]);
        csp.neq(x[4], x[7]);
        csp.neq(x[4], x[9]);
        csp.leq(x[5], x[9]);
        csp.lt(x[6], x[8]);
        csp.lt(x[6], x[9]);
        csp.lt(x[7], x[9]);
        csp.gt(x[8], x[9]);
        return csp;
    }

    /** Solution counts published by the course, by valMax. */
    public static final Map<Integer, Long> PROBLEM_2013_SOLUTIONS = Map.of(
            3, 0L, 4, 36L, 5, 744L, 6, 6_859L, 7, 40_761L, 8, 182_028L, 9, 663_300L, 10, 2_073_951L);

    public static final Map<Integer, Long> PROBLEM_00_SOLUTIONS = Map.of(
            3, 0L, 4, 4L, 5, 100L, 6, 1_020L, 7, 6_460L, 8, 30_148L, 9, 113_476L, 10, 363_748L);
}

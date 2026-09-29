package csp;

import csp.model.Constraint;
import csp.model.Csp;
import csp.model.Domain;
import csp.model.Equal;
import csp.model.LessOrEqual;
import csp.model.LessThan;
import csp.model.NotEqual;
import csp.model.Variable;
import csp.problems.CourseProblems;
import csp.problems.Sudoku;
import csp.search.Algorithm;
import csp.search.Result;
import csp.search.Solver;
import csp.search.VariableOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

/** Dependency-free test suite: {@code java -cp out csp.Tests}. Exits non-zero on the first failure. */
public final class Tests {

    private static int passed;

    public static void main(String[] args) {
        domainOperations();
        trailRestoresDomains();
        constraintsMatchBruteForce();
        smallCourseProblems();
        courseBenchmarksMatchPublishedCounts();
        solverLeavesProblemUnchanged();
        sudokuPuzzlesHaveOneValidSolution();
        timeLimitStopsTheSearch();
        extremeValuesDoNotOverflow();
        foreignVariableIsRejectedWithoutSideEffects();
        stoppedSearchCountsOnlyTriedValues();
        System.out.println("All " + passed + " checks passed.");
    }

    // ------------------------------------------------------------------ tiny assertion helpers

    private static void check(boolean condition, String what) {
        if (!condition) {
            System.err.println("FAILED: " + what);
            System.exit(1);
        }
        passed++;
    }

    private static void equal(long expected, long actual, String what) {
        check(expected == actual, what + ": expected " + expected + ", got " + actual);
    }

    // ------------------------------------------------------------------ model

    static void domainOperations() {
        Csp csp = new Csp();
        Variable v = csp.variable("v", -2, 6);
        csp.trail().push();
        check(v.domain().min() == -2 && v.domain().max() == 6 && v.domain().size() == 9, "initial bounds");
        check(v.remove(0) && !v.remove(0), "remove reports changes");
        check(v.removeBelow(1) && v.domain().min() == 1, "removeBelow");
        check(v.removeAbove(4) && v.domain().max() == 4, "removeAbove");
        check(Arrays.equals(v.domain().values(), new int[]{1, 2, 3, 4}), "values in order: " + v.domain());
        check(!v.domain().contains(-100) && !v.domain().contains(100), "contains out of range");
        Domain copy = v.domain().copy();
        v.instantiate(3);
        check(v.isInstantiated() && v.value() == 3 && copy.size() == 4, "instantiate does not touch copies");
        csp.trail().pop();
        check(v.domain().size() == 9, "trail restores the original domain");
    }

    static void trailRestoresDomains() {
        Csp csp = new Csp();
        Variable a = csp.variable("a", 0, 9), b = csp.variable("b", 0, 9);
        csp.trail().push();
        a.removeAbove(5);
        csp.trail().push();
        a.remove(2);
        b.instantiate(7);
        csp.trail().push();
        a.remove(3);
        csp.trail().pop();
        check(a.domain().toString().equals("{0, 1, 3, 4, 5}") && b.value() == 7, "pop undoes one level only");
        csp.trail().pop();
        check(a.domain().size() == 6 && b.domain().size() == 10, "pop undoes the second level");
        csp.trail().pop();
        check(a.domain().size() == 10, "pop undoes the first level");
    }

    /** isPossible and filter of every constraint agree with brute force on random domains with holes. */
    static void constraintsMatchBruteForce() {
        List<BiFunction<Variable, Variable, Constraint>> kinds = List.of(Equal::new, NotEqual::new, LessThan::new,
                LessOrEqual::new);
        Random rng = new Random(42);
        for (int trial = 0; trial < 2000; trial++) {
            Csp csp = new Csp();
            Variable x = csp.variable("x", 0, 6), y = csp.variable("y", 0, 6);
            csp.trail().push();
            for (Variable v : List.of(x, y)) {
                for (int k = 0; k <= 6; k++) {
                    if (rng.nextInt(3) == 0) {
                        v.remove(k);
                    }
                }
            }
            Constraint c = csp.add(kinds.get(trial % kinds.size()).apply(x, y));
            int[] dx = x.domain().values(), dy = y.domain().values();
            boolean anyPair = false;
            List<Integer> supportedX = new ArrayList<>(), supportedY = new ArrayList<>();
            for (int a : dx) {
                for (int b : dy) {
                    if (c.holds(a, b)) {
                        anyPair = true;
                        if (!supportedX.contains(a)) supportedX.add(a);
                        if (!supportedY.contains(b)) supportedY.add(b);
                    }
                }
            }
            check(c.isPossible() == anyPair, c + " isPossible on " + x.domain() + ", " + y.domain());
            String before = x.domain() + ", " + y.domain();
            csp.trail().push();
            for (Variable target : List.of(x, y)) { // the one-sided filtering used by forward checking and MAC
                c.filterTowards(target);
                List<Integer> expected = target == x ? supportedX : supportedY;
                int[] want = expected.stream().mapToInt(i -> i).sorted().toArray();
                boolean sourceWasEmpty = c.other(target).domain().isEmpty();
                check(sourceWasEmpty || Arrays.equals(target.domain().values(), want)
                        || (!anyPair && target.domain().isEmpty()),
                        c + " filterTowards(" + target.name() + ") on " + before);
                csp.trail().pop();
                csp.trail().push();
            }
            csp.trail().pop();
            c.filter();
            if (!anyPair) { // no valid pair: filtering must wipe out a domain, and the search then backtracks
                check(x.domain().isEmpty() || y.domain().isEmpty(), c + " filter detects failure on " + before);
            } else {
                supportedY.sort(null);
                check(Arrays.equals(x.domain().values(), supportedX.stream().mapToInt(i -> i).toArray())
                        && Arrays.equals(y.domain().values(), supportedY.stream().mapToInt(i -> i).toArray()),
                        c + " filter keeps exactly the supported values of " + before);
            }
            csp.trail().pop();
        }
    }

    // ------------------------------------------------------------------ solvers

    static void smallCourseProblems() {
        for (VariableOrder order : VariableOrder.values()) {
            for (Algorithm alg : Algorithm.values()) {
                Solver s = new Solver(alg, order);
                equal(27, s.solve(CourseProblems.csp1()).solutions(), alg + " on csp1");
                equal(alg == Algorithm.BRUTE_FORCE ? 27 : 6, s.solve(CourseProblems.csp2()).solutions(), alg + " on csp2");
                Result r3 = s.solve(CourseProblems.csp3());
                if (alg != Algorithm.BRUTE_FORCE) {
                    equal(1, r3.solutions(), alg + " on csp3");
                    check(Arrays.equals(r3.firstSolution(), new int[]{6, 6, 6}), alg + " finds (6, 6, 6)");
                }
            }
        }
    }

    static void courseBenchmarksMatchPublishedCounts() {
        for (int valMax = 3; valMax <= 10; valMax++) {
            for (Algorithm alg : Algorithm.values()) {
                boolean tooSlow = (alg == Algorithm.GENERATE_AND_TEST && valMax > 5)
                        || (EnumSet.of(Algorithm.BACKTRACKING, Algorithm.LOOK_AHEAD).contains(alg) && valMax > 8);
                if (alg == Algorithm.BRUTE_FORCE || tooSlow) {
                    continue; // brute force ignores constraints; the others take too long for a test at these sizes
                }
                for (VariableOrder order : VariableOrder.values()) {
                    Solver s = new Solver(alg, order);
                    equal(CourseProblems.PROBLEM_2013_SOLUTIONS.get(valMax),
                            s.solve(CourseProblems.problem2013(valMax)).solutions(), alg + "/" + order + " problem 2013, valMax " + valMax);
                    equal(CourseProblems.PROBLEM_00_SOLUTIONS.get(valMax),
                            s.solve(CourseProblems.problem00(valMax)).solutions(), alg + "/" + order + " problem 00, valMax " + valMax);
                }
            }
        }
    }

    static void solverLeavesProblemUnchanged() {
        Csp csp = CourseProblems.problem2013(6);
        String before = csp.toString();
        new Solver(Algorithm.MAC).solve(csp);
        new Solver(Algorithm.FORWARD_CHECKING, VariableOrder.SMALLEST_DOMAIN).solve(csp, 3, 0);
        check(csp.toString().equals(before), "domains are restored after a search");
        equal(6_859, new Solver(Algorithm.BACKTRACKING).solve(csp).solutions(), "the problem can be solved again");
    }

    static void sudokuPuzzlesHaveOneValidSolution() {
        for (String[] p : Sudoku.PUZZLES) {
            check(p[1].length() == 81, p[0] + " has 81 cells");
            for (Algorithm alg : List.of(Algorithm.FORWARD_CHECKING, Algorithm.MAC)) {
                Result r = new Solver(alg, VariableOrder.SMALLEST_DOMAIN).solve(Sudoku.build(p[1]), 2, 60_000);
                check(r.complete(), p[0] + " search finished");
                equal(1, r.solutions(), p[0] + " has exactly one solution (" + alg + ")");
                check(Sudoku.isValidSolution(p[1], r.firstSolution()), p[0] + " solution is valid");
            }
        }
    }

    static void timeLimitStopsTheSearch() {
        Result r = new Solver(Algorithm.GENERATE_AND_TEST).solve(CourseProblems.problem2013(10), 0, 200);
        check(!r.complete() && r.timedOut() && r.millis() < 2_000, "time limit stops a search (" + r.millis() + " ms)");
        Result first = new Solver(Algorithm.MAC).solve(CourseProblems.problem2013(10), 1, 0);
        check(first.solutions() == 1 && !first.complete() && !first.timedOut(), "solution limit is not a time-out");
    }

    static void extremeValuesDoNotOverflow() {
        for (Algorithm alg : List.of(Algorithm.BACKTRACKING, Algorithm.LOOK_AHEAD, Algorithm.FORWARD_CHECKING, Algorithm.MAC)) {
            Csp lt = new Csp();
            lt.lt(lt.variable("x", Integer.MAX_VALUE, Integer.MAX_VALUE), lt.variable("y", Integer.MIN_VALUE, Integer.MIN_VALUE));
            equal(0, new Solver(alg).solve(lt).solutions(), alg + ": MAX < MIN has no solution");
            Csp le = new Csp();
            le.leq(le.variable("x", Integer.MAX_VALUE - 1, Integer.MAX_VALUE), le.variable("y", Integer.MIN_VALUE, Integer.MIN_VALUE + 1));
            equal(0, new Solver(alg).solve(le).solutions(), alg + ": large <= small has no solution");
            Csp ok = new Csp();
            ok.lt(ok.variable("x", Integer.MAX_VALUE - 2, Integer.MAX_VALUE), ok.variable("y", Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
            equal(3, new Solver(alg).solve(ok).solutions(), alg + ": x < y near MAX_VALUE");
        }
    }

    static void foreignVariableIsRejectedWithoutSideEffects() {
        Csp a = new Csp(), b = new Csp();
        Variable x = a.variable("x", 0, 2);
        Variable other = b.variable("o", 0, 2);
        b.variable("o2", 0, 2);
        boolean rejected = false;
        try {
            a.lt(x, other);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        check(rejected && x.constraints().isEmpty() && other.constraints().isEmpty() && a.constraints().isEmpty(),
                "a constraint on a foreign variable is rejected and changes nothing");
        equal(3, new Solver(Algorithm.MAC).solve(a).solutions(), "the problem is intact");
    }

    static void stoppedSearchCountsOnlyTriedValues() {
        Csp csp = new Csp();
        csp.variable("v", 0, 1);
        Result r = new Solver(Algorithm.BACKTRACKING).solve(csp, 1, 0);
        equal(1, r.nodes(), "one value tried before stopping at the first solution");
        equal(2, new Solver(Algorithm.BACKTRACKING).solve(csp).nodes(), "both values tried when finding all solutions");
    }
}

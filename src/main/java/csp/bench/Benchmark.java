package csp.bench;

import csp.model.Csp;
import csp.problems.CourseProblems;
import csp.problems.Sudoku;
import csp.search.Algorithm;
import csp.search.Result;
import csp.search.Solver;
import csp.search.VariableOrder;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Runs every solver on the course problems and on Sudoku puzzles and writes
 * {@code results/scaling.csv} and {@code results/sudoku.csv}.
 *
 * <pre>java -cp out csp.bench.Benchmark [timeLimitSeconds]</pre>
 */
public final class Benchmark {

    private static final List<Algorithm> ALGORITHMS = List.of(Algorithm.GENERATE_AND_TEST, Algorithm.BACKTRACKING,
            Algorithm.LOOK_AHEAD, Algorithm.FORWARD_CHECKING, Algorithm.MAC);
    private static final int REPEATS = 3;       // timed repetitions for short runs (median reported)
    private static final double SHORT_MS = 5_000;

    public static void main(String[] args) throws IOException {
        long limitMs = (args.length > 0 ? Long.parseLong(args[0]) : 60) * 1000;
        Files.createDirectories(Path.of("results"));
        warmUp();
        scaling(limitMs);
        sudoku(limitMs);
    }

    private static void warmUp() {
        for (Algorithm a : ALGORITHMS) {
            new Solver(a).solve(CourseProblems.problem2013(5), 0, 2_000);
        }
    }

    /** Course problems 2013 and 00 at every size, all solutions. */
    private static void scaling(long limitMs) throws IOException {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of("results/scaling.csv")))) {
            out.println("problem,valmax,algorithm,order,solutions,nodes,checks,millis,complete");
            for (String problem : List.of("2013", "00")) {
                for (VariableOrder order : VariableOrder.values()) {
                    Set<Algorithm> timedOut = EnumSet.noneOf(Algorithm.class);
                    for (int valMax = 3; valMax <= 10; valMax++) {
                        final int v = valMax;
                        Supplier<Csp> build = () -> problem.equals("2013") ? CourseProblems.problem2013(v)
                                : CourseProblems.problem00(v);
                        for (Algorithm alg : ALGORITHMS) {
                            if (timedOut.contains(alg)) { // larger instances cannot be faster: record, don't rerun
                                out.printf(Locale.ROOT, "%s,%d,%s,%s,,,,,false%n", problem, valMax, alg, order);
                                continue;
                            }
                            Result r = run(new Solver(alg, order), build, 0, limitMs);
                            if (!r.complete()) {
                                timedOut.add(alg);
                            }
                            out.printf(Locale.ROOT, "%s,%d,%s,%s,%d,%d,%d,%.3f,%b%n", problem, valMax, alg, order,
                                    r.solutions(), r.nodes(), r.checks(), r.millis(), r.complete());
                            out.flush();
                            System.out.printf(Locale.ROOT, "problem %s n=%d %-17s %-15s %9d sol %12d nodes %10.1f ms%s%n",
                                    problem, valMax, alg, order, r.solutions(), r.nodes(), r.millis(),
                                    r.complete() ? "" : "  (time limit)");
                        }
                    }
                }
            }
        }
    }

    /** Each puzzle solved completely (a second solution is searched for, proving uniqueness). */
    private static void sudoku(long limitMs) throws IOException {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of("results/sudoku.csv")))) {
            out.println("puzzle,givens,algorithm,order,solutions,nodes,checks,millis,complete");
            for (String[] p : Sudoku.PUZZLES) {
                long givens = p[1].chars().filter(c -> c >= '1' && c <= '9').count();
                for (VariableOrder order : VariableOrder.values()) {
                    for (Algorithm alg : ALGORITHMS.subList(1, ALGORITHMS.size())) {
                        Result r = run(new Solver(alg, order), () -> Sudoku.build(p[1]), 2, limitMs);
                        out.printf(Locale.ROOT, "%s,%d,%s,%s,%d,%d,%d,%.3f,%b%n", p[0], givens, alg, order,
                                r.solutions(), r.nodes(), r.checks(), r.millis(), r.complete());
                        out.flush();
                        System.out.printf(Locale.ROOT, "%-20s %-17s %-15s %12d nodes %10.1f ms%s%n", p[0], alg, order,
                                r.nodes(), r.millis(), r.complete() ? "" : "  (time limit)");
                    }
                }
            }
        }
    }

    /** One run; short runs are repeated and the median time is kept (counts are deterministic). */
    private static Result run(Solver solver, Supplier<Csp> build, long maxSolutions, long limitMs) {
        Result first = solver.solve(build.get(), maxSolutions, limitMs);
        if (!first.complete() || first.millis() > SHORT_MS) {
            return first;
        }
        double[] ms = new double[REPEATS];
        ms[0] = first.millis();
        for (int i = 1; i < REPEATS; i++) {
            ms[i] = solver.solve(build.get(), maxSolutions, limitMs).millis();
        }
        Arrays.sort(ms);
        return new Result(first.algorithm(), first.order(), first.solutions(), first.nodes(), first.checks(),
                ms[REPEATS / 2], true, false, first.firstSolution());
    }
}

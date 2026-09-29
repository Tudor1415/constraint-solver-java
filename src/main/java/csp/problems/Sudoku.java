package csp.problems;

import csp.model.Csp;
import csp.model.Variable;

/**
 * Sudoku as a constraint problem: one variable per cell with values 1-9, and a
 * "≠" constraint between every two cells of the same row, column or 3×3 box
 * (810 constraints). Given digits become one-value domains.
 */
public final class Sudoku {

    /**
     * Well-known puzzles, from easy to very hard ('.' = empty cell). Each has exactly one
     * solution (checked by the tests). Sources: the Wikipedia "Sudoku" article, Peter
     * Norvig's "Solving Every Sudoku Puzzle" collections (top95, hardest), and Arto Inkala (2012).
     */
    public static final String[][] PUZZLES = {
        {"Wikipedia example",
         "53..7....6..195....98....6.8...6...34..8.3..17...2...6.6....28....419..5....8..79"},
        {"Norvig top95 #1",
         "4.....8.5.3..........7......2.....6.....8.4......1.......6.3.7.5..2.....1.4......"},
        {"Norvig hardest #1",
         "85...24..72......9..4.........1.7..23.5...9...4...........8..7..17..........36.4."},
        {"Norvig hardest #2",
         "..53.....8......2..7..1.5..4....53...1..7...6..32...8..6.5....9..4....3......97.."},
        {"Inkala 2012",
         "8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4.."},
    };

    private Sudoku() {
    }

    /** Builds the problem from 81 characters, row by row: digits 1-9, anything else for an empty cell. */
    public static Csp build(String grid) {
        if (grid.length() != 81) {
            throw new IllegalArgumentException("a sudoku grid has 81 cells, got " + grid.length());
        }
        Csp csp = new Csp();
        Variable[] cell = new Variable[81];
        for (int i = 0; i < 81; i++) {
            char ch = grid.charAt(i);
            int d = ch >= '1' && ch <= '9' ? ch - '0' : 0;
            cell[i] = csp.variable("r" + (i / 9 + 1) + "c" + (i % 9 + 1), d == 0 ? 1 : d, d == 0 ? 9 : d);
        }
        for (int i = 0; i < 81; i++) {
            for (int j = i + 1; j < 81; j++) {
                if (sameUnit(i, j)) {
                    csp.neq(cell[i], cell[j]);
                }
            }
        }
        return csp;
    }

    static boolean sameUnit(int i, int j) {
        int ri = i / 9, ci = i % 9, rj = j / 9, cj = j % 9;
        return ri == rj || ci == cj || (ri / 3 == rj / 3 && ci / 3 == cj / 3);
    }

    /** Checks a filled grid against the rules and the givens of the puzzle. */
    public static boolean isValidSolution(String puzzle, int[] values) {
        for (int i = 0; i < 81; i++) {
            char ch = puzzle.charAt(i);
            if (values[i] < 1 || values[i] > 9 || (ch >= '1' && ch <= '9' && values[i] != ch - '0')) {
                return false;
            }
            for (int j = i + 1; j < 81; j++) {
                if (sameUnit(i, j) && values[i] == values[j]) {
                    return false;
                }
            }
        }
        return true;
    }
}

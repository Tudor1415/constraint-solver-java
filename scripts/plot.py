"""The four README figures, from results/scaling.csv and results/sudoku.csv.

    python scripts/plot.py        # writes figures/fig1..fig4 (.png)
"""

import csv
from collections import defaultdict
from pathlib import Path

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import matplotlib.ticker as mticker  # noqa: E402
import numpy as np  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
FIG = ROOT / "figures"
FIG.mkdir(exist_ok=True)

INK, INK2, MUTED, GRID = "#1f1f1e", "#52514e", "#a3a29c", "#e6e5e0"
ALGS = ["GENERATE_AND_TEST", "BACKTRACKING", "LOOK_AHEAD", "FORWARD_CHECKING", "MAC"]
NAME = {"GENERATE_AND_TEST": "Try everything", "BACKTRACKING": "Backtracking",
        "LOOK_AHEAD": "Backtracking + look-ahead", "FORWARD_CHECKING": "Forward checking",
        "MAC": "Full propagation"}
COLOR = {"GENERATE_AND_TEST": INK2, "BACKTRACKING": "#4a3aa7", "LOOK_AHEAD": "#2a78d6",
         "FORWARD_CHECKING": "#eb6834", "MAC": "#1baf7a"}
STYLE = {"GENERATE_AND_TEST": (0, (4, 2)), "LOOK_AHEAD": (0, (1.5, 1.5))}  # look-ahead overlaps backtracking

plt.rcParams.update({
    "figure.facecolor": "white", "axes.facecolor": "white", "savefig.facecolor": "white",
    "font.family": "DejaVu Sans", "font.size": 12, "axes.titlesize": 13, "axes.titleweight": "bold",
    "axes.titlelocation": "left", "axes.titlepad": 8, "axes.labelsize": 12, "axes.labelcolor": INK2,
    "axes.edgecolor": MUTED, "axes.spines.top": False, "axes.spines.right": False,
    "axes.grid": True, "grid.color": GRID, "grid.linewidth": 0.8, "axes.axisbelow": True,
    "xtick.color": INK2, "ytick.color": INK2, "xtick.labelsize": 11, "ytick.labelsize": 11,
    "legend.fontsize": 10.5, "legend.frameon": False, "lines.linewidth": 2.6, "text.color": INK,
})


def read(name):
    with open(ROOT / "results" / name) as f:
        return list(csv.DictReader(f))


def save(fig, name):
    fig.savefig(FIG / f"{name}.png", dpi=140, bbox_inches="tight")
    plt.close(fig)
    print("wrote", FIG / f"{name}.png")


def plain_log(axis):
    axis.set_major_formatter(mticker.FuncFormatter(lambda v, _: f"{v:,.0f}" if v >= 1 else f"{v:g}"))


def growth(rows, metric, ylabel, title, name, scale=1.0):
    """One line per algorithm: metric against problem size, on the course's 'problem 2013'."""
    fig, ax = plt.subplots(figsize=(13, 2.6))
    for alg in ALGS:
        pts = [(int(r["valmax"]), float(r[metric]) * scale) for r in rows
               if r["problem"] == "2013" and r["order"] == "INPUT" and r["algorithm"] == alg and r["complete"] == "true"]
        if not pts:
            continue
        zero = [px for px, py in pts if py == 0]  # solved by propagation alone, without trying any value
        pts = [(px, py) for px, py in pts if py > 0]
        x, y = zip(*sorted(pts))
        if zero:
            ax.annotate(f"(size {zero[0]}: solved with no try)", (x[0], y[0]), xytext=(10, -12), textcoords="offset points",
                        fontsize=9.5, color=COLOR[alg], ha="left")
        ax.plot(x, y, marker="o", ms=5, color=COLOR[alg], ls=STYLE.get(alg, "-"), label=NAME[alg])
        failed = sorted(int(r["valmax"]) for r in rows if r["problem"] == "2013" and r["order"] == "INPUT"
                        and r["algorithm"] == alg and r["complete"] == "false")
        if failed:  # mark the first size that hit the time limit, next to the last size that finished
            ax.plot(failed[0], y[-1], marker="X", ms=10, color=COLOR[alg], ls="none")
            ax.annotate(f"size {failed[0]}: gave up (> 60 s)", (failed[0], y[-1]), xytext=(10, -4),
                        textcoords="offset points", fontsize=10, color=COLOR[alg])
    ax.set_yscale("log")
    ax.set_xlabel("values allowed per variable (problem size)")
    ax.set_ylabel(ylabel)
    ax.set_xticks(range(3, 11))
    ax.set_title(title)
    ax.legend(loc="center left", bbox_to_anchor=(1.01, 0.5))
    save(fig, name)


def fig3_sudoku(rows):
    """Time to solve (and prove unique) each Sudoku, per solver, smallest-domain-first order."""
    puzzles = list(dict.fromkeys(r["puzzle"] for r in rows))
    algs = ALGS[1:]
    fig, ax = plt.subplots(figsize=(13, 2.7))
    width = 0.2
    x = np.arange(len(puzzles))
    gave_up = {}
    for k, alg in enumerate(algs):
        vals, done = [], []
        for p in puzzles:
            r = next(r for r in rows if r["puzzle"] == p and r["algorithm"] == alg and r["order"] == "SMALLEST_DOMAIN")
            vals.append(float(r["millis"]) / 1000)
            done.append(r["complete"] == "true")
        pos = x + (k - 1.5) * width
        ax.bar(pos, vals, width * 0.92, color=COLOR[alg], label=NAME[alg])
        for i, (px, v, d) in enumerate(zip(pos, vals, done)):
            if not d:
                gave_up.setdefault(i, []).append((px, v))
    for bars in gave_up.values():
        ax.text(np.mean([b[0] for b in bars]), max(b[1] for b in bars) * 1.3, "gave up (> 60 s)",
                ha="center", va="bottom", fontsize=10, color=INK)
    ax.set_yscale("log")
    plain_log(ax.yaxis)
    ax.set_xticks(x, puzzles)
    ax.set_ylabel("time to solve (s)")
    ax.set_title("Sudoku: crossing out impossible values solves hard puzzles in milliseconds")
    ax.text(0.99, 0.97, "most constrained cell first", transform=ax.transAxes, ha="right", va="top",
            fontsize=10, color=INK2)
    ax.legend(loc="upper left", ncol=4)
    ax.set_ylim(top=ax.get_ylim()[1] * 40)
    save(fig, "fig3_sudoku")


def fig4_order(scaling, sudoku):
    """Nodes explored with the fixed order vs 'most constrained variable first'."""
    cases = []
    for p in dict.fromkeys(r["puzzle"] for r in sudoku):
        cases.append((p, [r for r in sudoku if r["puzzle"] == p]))
    cases.append(("Course problem\n(size 10)", [r for r in scaling if r["problem"] == "2013" and r["valmax"] == "10"]))
    fig, axes = plt.subplots(1, 2, figsize=(13, 2.7), sharey=True)
    for ax, alg in zip(axes, ["FORWARD_CHECKING", "MAC"]):
        ratio, labels = [], []
        for label, rows in cases:
            def nodes(order):
                r = next(r for r in rows if r["algorithm"] == alg and r["order"] == order)
                return float(r["nodes"]) if r["complete"] == "true" else np.nan
            ratio.append(nodes("INPUT") / nodes("SMALLEST_DOMAIN"))
            labels.append(label)
        y = np.arange(len(labels))
        ax.barh(y, ratio, color=COLOR[alg], height=0.62)
        for yi, v in zip(y, ratio):
            if np.isfinite(v):
                ax.text(v, yi, f"  ×{v:.1f}" if v < 10 else f"  ×{v:,.0f}", va="center", fontsize=10.5)
            else:
                ax.text(1.15, yi, "fixed order gave up (> 60 s)", va="center", fontsize=10, color=INK2)
        ax.set_xscale("log")
        ax.set_xlim(1, 1500)
        plain_log(ax.xaxis)
        ax.set_yticks(y, labels)
        ax.invert_yaxis()
        ax.grid(axis="y", visible=False)
        ax.set_xlabel("fewer options explored (times)")
        ax.set_title(NAME[alg])
    fig.suptitle("Deciding the most constrained variable first saves a lot of work", x=0.02, ha="left",
                 fontsize=13, fontweight="bold")
    fig.tight_layout()
    save(fig, "fig4_order")


if __name__ == "__main__":
    scaling, sudoku = read("scaling.csv"), read("sudoku.csv")
    growth(scaling, "nodes", "options explored",
           "Checking the rules early avoids exploring hopeless options (course problem, fixed order)", "fig1_work")
    growth(scaling, "millis", "time to find all solutions (s)",
           "…which makes solving about 2,000 times faster (course problem, fixed order)", "fig2_time", scale=1e-3)
    fig3_sudoku(sudoku)
    fig4_order(scaling, sudoku)

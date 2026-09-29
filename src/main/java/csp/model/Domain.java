package csp.model;

import java.util.BitSet;

/**
 * A finite set of integer values, stored as a bit set.
 *
 * <p>Domains are only modified through {@link Variable}, which records the old
 * domain on the {@link Trail} first, so that every change can be undone when the
 * search backtracks.
 */
public final class Domain {

    private final int offset; // value v is stored at bit (v - offset), so negative values work too
    private final BitSet bits;

    /** The domain {min, min + 1, ..., max}. */
    public Domain(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("empty range [" + min + ", " + max + "]");
        }
        long width = (long) max - min + 1;
        if (width > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("range too wide for a bit set: [" + min + ", " + max + "]");
        }
        this.offset = min;
        this.bits = new BitSet((int) width);
        this.bits.set(0, (int) width);
    }

    private Domain(int offset, BitSet bits) {
        this.offset = offset;
        this.bits = bits;
    }

    public Domain copy() {
        return new Domain(offset, (BitSet) bits.clone());
    }

    public int size() {
        return bits.cardinality();
    }

    public boolean isEmpty() {
        return bits.isEmpty();
    }

    public boolean isSingleton() {
        return size() == 1;
    }

    public boolean contains(int v) {
        long i = (long) v - offset; // long arithmetic: no overflow at the extremes of int
        return i >= 0 && i < bits.length() && bits.get((int) i);
    }

    /** Smallest value. The domain must not be empty. */
    public int min() {
        requireNonEmpty();
        return bits.nextSetBit(0) + offset;
    }

    /** Largest value. The domain must not be empty. */
    public int max() {
        requireNonEmpty();
        return bits.length() - 1 + offset;
    }

    /** The values in increasing order. A fresh array, so the domain may change while it is iterated. */
    public int[] values() {
        return bits.stream().map(i -> i + offset).toArray();
    }

    // ------------------------------------------------------------------ mutations (package-private)

    /** Removes v; returns true if the domain changed. */
    boolean remove(int v) {
        if (!contains(v)) {
            return false;
        }
        bits.clear((int) ((long) v - offset));
        return true;
    }

    /** Removes every value below v; returns true if the domain changed. */
    boolean removeBelow(int v) {
        int i = (int) Math.min((long) v - offset, bits.length());
        if (i <= 0 || bits.nextSetBit(0) >= i) {
            return false;
        }
        bits.clear(0, i);
        return true;
    }

    /** Removes every value above v; returns true if the domain changed. */
    boolean removeAbove(int v) {
        long from = Math.max((long) v - offset + 1, 0);
        if (from >= bits.length()) {
            return false;
        }
        bits.clear((int) from, bits.length());
        return true;
    }

    /** Keeps only the values also in {@code other}; returns true if the domain changed. */
    boolean retainAll(Domain other) {
        BitSet kept = new BitSet();
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
            if (other.contains(i + offset)) {
                kept.set(i);
            }
        }
        if (kept.equals(bits)) {
            return false;
        }
        bits.clear();
        bits.or(kept);
        return true;
    }

    void clear() {
        bits.clear();
    }

    /** Keeps only v (which must be in the domain). */
    void assign(int v) {
        if (!contains(v)) {
            throw new IllegalArgumentException(v + " is not in " + this);
        }
        bits.clear();
        bits.set((int) ((long) v - offset));
    }

    private void requireNonEmpty() {
        if (bits.isEmpty()) {
            throw new IllegalStateException("empty domain");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        int[] vs = values();
        for (int k = 0; k < vs.length; k++) {
            sb.append(k == 0 ? "" : ", ").append(vs[k]);
        }
        return sb.append('}').toString();
    }
}

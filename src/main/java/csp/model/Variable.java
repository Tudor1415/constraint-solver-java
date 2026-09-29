package csp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A named unknown with a finite domain of integer values. */
public final class Variable {

    private final String name;
    private final int index; // position in its Csp, used by the solvers
    private final Trail trail;
    private final List<Constraint> constraints = new ArrayList<>();
    private Domain domain;
    private int stamp = -1; // trail level at which the current domain was saved

    Variable(String name, int index, Domain domain, Trail trail) {
        this.name = name;
        this.index = index;
        this.domain = domain;
        this.trail = trail;
    }

    public String name() {
        return name;
    }

    public int index() {
        return index;
    }

    public Domain domain() {
        return domain;
    }

    /** The constraints on this variable. */
    public List<Constraint> constraints() {
        return Collections.unmodifiableList(constraints);
    }

    public boolean isInstantiated() {
        return domain.isSingleton();
    }

    /** The value of an instantiated variable. */
    public int value() {
        if (!isInstantiated()) {
            throw new IllegalStateException(name + " is not instantiated: " + domain);
        }
        return domain.min();
    }

    // ------------------------------------------------------------------ changes, recorded on the trail

    /** Reduces the domain to {v}. */
    public void instantiate(int v) {
        trail.save(this);
        domain.assign(v);
    }

    public boolean remove(int v) {
        if (!domain.contains(v)) {
            return false;
        }
        trail.save(this);
        return domain.remove(v);
    }

    public boolean removeBelow(int v) {
        if (domain.isEmpty() || domain.min() >= v) {
            return false;
        }
        trail.save(this);
        return domain.removeBelow(v);
    }

    public boolean removeAbove(int v) {
        if (domain.isEmpty() || domain.max() <= v) {
            return false;
        }
        trail.save(this);
        return domain.removeAbove(v);
    }

    /** Empties the domain (a dead end); returns true if it was not already empty. */
    public boolean removeAll() {
        if (domain.isEmpty()) {
            return false;
        }
        trail.save(this);
        domain.clear();
        return true;
    }

    public boolean retainAll(Domain other) {
        Domain probe = domain.copy();
        if (!probe.retainAll(other)) {
            return false;
        }
        trail.save(this);
        return domain.retainAll(other);
    }

    // ------------------------------------------------------------------ used by Csp and Trail

    void addConstraint(Constraint c) {
        constraints.add(c);
    }

    int stamp() {
        return stamp;
    }

    void setStamp(int s) {
        stamp = s;
    }

    void restore(Domain old, int oldStamp) {
        domain = old;
        stamp = oldStamp;
    }

    @Override
    public String toString() {
        return name + " ∈ " + domain;
    }
}

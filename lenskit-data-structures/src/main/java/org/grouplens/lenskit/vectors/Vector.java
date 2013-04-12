package org.grouplens.lenskit.vectors;

import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Arrays;

import static java.lang.Math.sqrt;

/**
 * A real vector.  This is a read-only view of a vector of doubles.  It is backed by an array,
 * and contains values associated with indices [0,{@link #dim()}).
 *
 * @compat Experimental — this interface may change in future versions of LensKit.
 */
public abstract class Vector implements Serializable {
    private static final long serialVersionUID = 1L;

    final double[] data;

    private transient volatile Double norm;
    private transient volatile Double sum;

    Vector(double[] d) {
        data = d;
    }

    void markModified() {
        norm = null;
        sum = null;
    }

    /**
     * Get the value from the vector at the specified position.
     * @param i The index into the vector.
     * @return The value at index {@code i}.
     * @throws IllegalArgumentException if {@code i} is not in the range [0,{@link #dim()}).
     */
    public final double get(int i) {
        Preconditions.checkElementIndex(i, data.length);
        return data[i];
    }

    /**
     * Get the dimension of this vector (the number of elements).
     * @return The number of elements in the vector.
     */
    public final int dim() {
        return data.length;
    }

    /**
     * Get the L2 (Euclidean) norm of this vector.
     * @return The Euclidean length of the vector.
     */
    public final double norm() {
        if (norm == null) {
            double ssq = 0;
            final int sz = data.length;
            for (double v : data) {
                ssq += v * v;
            }
            norm = sqrt(ssq);
        }
        return norm;
    }

    /**
     * Get the sum of this vector.
     * @return The sum of the elements of the vector.
     */
    public final double sum() {
        if (sum == null) {
            double s = 0;
            for (double v : data) {
                s += v;
            }
            sum = s;
        }
        return sum;
    }

    /**
     * Compute the dot product of this vector with another.
     * @param other The other vector.
     * @return The dot product of this vector and {@code other}.
     * @throws IllegalArgumentException if {@code other.dim() != this.dim()}.
     */
    public final double dot(Vector other) {
        final int sz = data.length;
        Preconditions.checkArgument(sz == other.dim());
        double s = 0;
        for (int i = 0; i < sz; i++) {
            s += data[i] * other.data[i];
        }
        return s;
    }

    /**
     * Get an immutable vector with this vector's contents.  If the vector is already immutable,
     * it is not changed.
     * @return The immutable vector.
     */
    public ImmutableVector immutable() {
        return new ImmutableVector(Arrays.copyOf(data, data.length));
    }

    /**
     * Get a mutable copy of this vector.
     * @return A mutable copy of this vector.
     */
    public MutableVector mutableCopy() {
        return new MutableVector(Arrays.copyOf(data, data.length));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Vector) {
            return Arrays.equals(data, ((Vector) o).data);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}

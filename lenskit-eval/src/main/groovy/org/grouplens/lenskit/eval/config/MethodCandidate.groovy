package org.grouplens.lenskit.eval.config

/**
 * Interface for candidates for setting activations.
 * @author Michael Ekstrand
 */
public interface MethodCandidate {
    /**
     * Set the setting.
     * @param target
     */
    void invoke(Object target)
}

package org.grouplens.lenskit.transform.quantize;

import org.grouplens.lenskit.RatingPredictor;
import org.grouplens.lenskit.core.AbstractItemScorer;
import org.grouplens.lenskit.data.Event;
import org.grouplens.lenskit.data.UserHistory;
import org.grouplens.lenskit.data.dao.DataAccessObject;
import org.grouplens.lenskit.vectors.MutableSparseVector;
import org.grouplens.lenskit.vectors.VectorEntry;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * A rating predictor wrapper that quantizes predictions.
 * @author Michael Ekstrand
 */
public class QuantizedRatingPredictor extends AbstractItemScorer implements RatingPredictor {
    private RatingPredictor basePredictor;
    private Quantizer quantizer;

    /**
     * Construct a new quantized predictor.
     * @param dao The DAO.
     * @param base The base predictor.
     * @param q The quantizer.
     */
    @Inject
    public QuantizedRatingPredictor(DataAccessObject dao, RatingPredictor base, Quantizer q) {
        super(dao);
        basePredictor = base;
        quantizer = q;
    }

    @Override
    public void score(long user, @Nonnull MutableSparseVector scores) {
        basePredictor.score(user, scores);
        for (VectorEntry e: scores.fast()) {
            scores.set(e, quantizer.apply(e.getValue()));
        }
    }

    @Override
    public void score(@Nonnull UserHistory<? extends Event> profile, @Nonnull MutableSparseVector scores) {
        basePredictor.score(profile, scores);
        for (VectorEntry e: scores.fast()) {
            scores.set(e, quantizer.apply(e.getValue()));
        }
    }
}

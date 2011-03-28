// Configure the gradient descent SVD to behave mostly like FunkSVD
rec.name = "FunkSVD"
rec.module = org.grouplens.lenskit.svd.GradientDescentSVDModule
// mean damping is currently broken - see #41
// rec.module.core.meanDamping = 25
rec.module.core.baseline = org.grouplens.lenskit.baseline.ItemUserMeanPredictor
rec.module.clampingFunction = org.grouplens.lenskit.svd.RatingRangeClamp
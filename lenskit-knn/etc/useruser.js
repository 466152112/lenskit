 rec.name = "UserUser"
 rec.module = org.grouplens.lenskit.knn.user.UserRecommenderModule
 rec.module.variant = org.grouplens.lenskit.knn.user.SimpleUserUserRatingRecommender
 rec.module.knn.similarityDamping = 50
 rec.module.core.baseline = org.grouplens.lenskit.baseline.ItemUserMeanPredictor

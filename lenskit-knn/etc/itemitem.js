/* Configuration script to run a pretty good item-item recommender. */
rec.name = "ItemItem"
rec.module = org.grouplens.lenskit.knn.item.ItemRecommenderModule
rec.module.knn.similarityDamping = 50
rec.module.core.baseline = org.grouplens.lenskit.baseline.ItemUserMeanPredictor

package com.tangem.features.yield.supply.impl

import com.tangem.core.ui.coil.ImagePreloader
import com.tangem.domain.stories.GetStoryContentUseCase
import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.utils.coroutines.runSuspendCatching
import javax.inject.Inject

/**
 * Warms the in-memory `StoriesStore` cache (and Coil image cache) for the yield-boost story.
 *
 * Called proactively from yield-supply models so that when the user taps "Learn more" /
 * the active-boost row, [com.tangem.feature.stories.impl.model.StoriesModel] hits cache
 * instead of waiting for the 1-second network fetch.
 */
internal class YieldBoostStoryPreloader @Inject constructor(
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val imagePreloader: ImagePreloader,
) {

    suspend fun preload() {
        runSuspendCatching {
            getStoryContentUseCase
                .invokeSync(id = StoryContentIds.STORY_FIRST_TIME_YIELD_PROMO.id, refresh = true)
                .onRight { story -> story?.getImageUrls()?.forEach(imagePreloader::preload) }
        }
    }
}
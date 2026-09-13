package com.tangem.features.storiesv2.impl.prefetch

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.features.storiesv2.StoriesV2Prefetcher
import com.tangem.features.storiesv2.StoryV2Type
import com.tangem.features.storiesv2.impl.analytics.StoriesV2Events
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorReason
import com.tangem.features.storiesv2.impl.analytics.StoryV2ErrorStage
import com.tangem.features.storiesv2.impl.content.StoryV2ContentRepository
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Warms a story outside the player: by the time the player exists the viewer is already looking at it, and any work
 * left to that moment is work they watch happen. So this lives on an application scope, triggered by the story
 * becoming *known* rather than by the tap that opens it.
 *
 * For a bundled story that is resolving and validating the composition once, so opening the player is a cache read.
 * A remote story adds posters first and full assets second, and only for the stories this particular viewer can be
 * shown — the control group of an experiment must not pay for the treatment's video.
 */
@Singleton
internal class DefaultStoriesV2Prefetcher @Inject constructor(
    private val contentRepository: StoryV2ContentRepository,
    private val analytics: AnalyticsEventHandler,
    private val appScope: AppCoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) : StoriesV2Prefetcher {

    private val warmed = ConcurrentHashMap.newKeySet<StoryV2Type>()

    override fun prefetch(story: StoryV2Type) {
        if (!warmed.add(story)) return

        appScope.launch(dispatchers.io) {
            val composition = runSuspendCatching { contentRepository.getComposition(story) }.getOrNull()

            if (composition == null) {
                warmed.remove(story)
                analytics.send(
                    StoriesV2Events.StoryErrorOccurred(
                        storyId = null,
                        slideId = null,
                        errorStage = StoryV2ErrorStage.ASSET_PREFETCH.analyticsValue,
                        errorReason = StoryV2ErrorReason.NOT_FOUND.analyticsValue,
                        source = null,
                    ),
                )
            }
        }
    }
}
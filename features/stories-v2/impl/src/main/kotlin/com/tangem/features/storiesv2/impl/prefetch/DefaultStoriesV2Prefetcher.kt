package com.tangem.features.storiesv2.impl.prefetch

import com.tangem.features.storiesv2.StoriesV2Prefetcher
import com.tangem.features.storiesv2.StoryV2Type
import com.tangem.features.storiesv2.impl.content.StoryV2ContentRepository
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Warms a story outside the player: by the time the player exists the viewer is already looking at it, and any work
 * left to that moment is work they watch happen. So this lives on an application scope, triggered by the story
 * becoming *known* rather than by the tap that opens it.
 *
 * For a bundled story that is resolving and validating the composition once. A remote story is the whole delivery:
 * the response and every file, so opening it is a cache read. Only call it for a viewer who can be shown the story —
 * the control group of an experiment must not pay for the treatment's video.
 *
 * Repeated calls are cheap: the repository shares a load in flight and keeps a ready one. Failures are reported by
 * the content layer, where the stage that failed is known.
 */
@Singleton
internal class DefaultStoriesV2Prefetcher @Inject constructor(
    private val contentRepository: StoryV2ContentRepository,
    private val appScope: AppCoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) : StoriesV2Prefetcher {

    override fun prefetch(story: StoryV2Type) {
        appScope.launch(dispatchers.io) { contentRepository.load(story) }
    }
}
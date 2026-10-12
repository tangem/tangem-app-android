package com.tangem.features.collectibles.impl.stories.ui.preview

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.collectibles.impl.stories.ui.state.CollectiblesStoriesUM
import com.tangem.features.collectibles.impl.stories.ui.state.CollectiblesStorySlideUM
import kotlinx.collections.immutable.persistentListOf

/**
 * Fixtures for the stories `@Preview`s. Not used in production code.
 */
internal object CollectiblesStoriesPreviewData {

    val state: CollectiblesStoriesUM = CollectiblesStoriesUM(
        slides = persistentListOf(
            CollectiblesStorySlideUM(
                title = TextReference.Res(R.string.collectibles_stories_graded_title),
                subtitle = TextReference.Res(R.string.collectibles_stories_graded_subtitle),
                continueButtonText = TextReference.Res(R.string.common_next),
            ),
            CollectiblesStorySlideUM(
                title = TextReference.Res(R.string.collectibles_stories_ownership_title),
                subtitle = TextReference.Res(R.string.collectibles_stories_ownership_subtitle),
                continueButtonText = TextReference.Res(R.string.collectibles_stories_browse_packs_button),
            ),
        ),
        currentIndex = 0,
        onNextSlideClick = {},
        onPreviousSlideClick = {},
        onContinueClick = {},
        onCloseClick = {},
    )

    val lastSlideState: CollectiblesStoriesUM = state.copy(currentIndex = state.slides.lastIndex)
}
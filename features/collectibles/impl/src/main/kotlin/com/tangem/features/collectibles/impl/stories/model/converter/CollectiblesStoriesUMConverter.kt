package com.tangem.features.collectibles.impl.stories.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.collectibles.impl.stories.ui.state.CollectiblesStoriesUM
import com.tangem.features.collectibles.impl.stories.ui.state.CollectiblesStorySlideUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class CollectiblesStoriesUMConverter :
    Converter<CollectiblesStoriesUMConverter.Callbacks, CollectiblesStoriesUM> {

    override fun convert(value: Callbacks): CollectiblesStoriesUM {
        return CollectiblesStoriesUM(
            slides = persistentListOf(
                slide(
                    title = TextReference.Res(R.string.collectibles_stories_graded_title),
                    subtitle = TextReference.Res(R.string.collectibles_stories_graded_subtitle),
                ),
                slide(
                    title = TextReference.Res(R.string.collectibles_stories_security_title),
                    subtitle = TextReference.Res(R.string.collectibles_stories_security_subtitle),
                ),
                slide(
                    title = TextReference.Res(R.string.collectibles_stories_buyer_title),
                    subtitle = TextReference.Res(R.string.collectibles_stories_buyer_subtitle),
                ),
                slide(
                    title = TextReference.Res(R.string.collectibles_stories_transparency_title),
                    subtitle = TextReference.Res(R.string.collectibles_stories_transparency_subtitle),
                ),
                slide(
                    title = TextReference.Res(R.string.collectibles_stories_ownership_title),
                    subtitle = TextReference.Res(R.string.collectibles_stories_ownership_subtitle),
                    buttonText = TextReference.Res(R.string.collectibles_stories_browse_packs_button),
                ),
            ),
            currentIndex = 0,
            onNextSlideClick = value.onNextSlideClick,
            onPreviousSlideClick = value.onPreviousSlideClick,
            onContinueClick = value.onContinueClick,
            onCloseClick = value.onCloseClick,
        )
    }

    private fun slide(
        title: TextReference,
        subtitle: TextReference,
        buttonText: TextReference = TextReference.Res(R.string.common_next),
    ) = CollectiblesStorySlideUM(
        title = title,
        subtitle = subtitle,
        continueButtonText = buttonText,
    )

    data class Callbacks(
        val onNextSlideClick: () -> Unit,
        val onPreviousSlideClick: () -> Unit,
        val onContinueClick: () -> Unit,
        val onCloseClick: () -> Unit,
    )
}
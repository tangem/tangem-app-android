package com.tangem.features.collectibles.impl.stories.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * State of the Collectibles stories screen.
 *
 * @property currentIndex index of the displayed slide; also drives the button label and the hold on the last frame.
 * @property legal legal line above the button, the same for all slides.
 * @property onNextSlideClick tap on the right half of the screen; does nothing on the last slide.
 * @property onPreviousSlideClick tap on the left half of the screen; does nothing on the first slide.
 * @property onContinueClick main button click: moves forward, and from the last slide exits the flow.
 * @property onCloseClick closing the stories.
 */
@Immutable
internal data class CollectiblesStoriesUM(
    val slides: ImmutableList<CollectiblesStorySlideUM>,
    val currentIndex: Int,
    val legal: TextReference,
    val onNextSlideClick: () -> Unit,
    val onPreviousSlideClick: () -> Unit,
    val onContinueClick: () -> Unit,
    val onCloseClick: () -> Unit,
)
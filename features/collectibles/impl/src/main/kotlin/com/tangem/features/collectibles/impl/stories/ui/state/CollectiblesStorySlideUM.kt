package com.tangem.features.collectibles.impl.stories.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/**
 * A single slide of the Collectibles stories screen.
 *
 * @property title slide headline.
 * @property subtitle supporting text under the headline.
 * @property continueButtonText label of the main button while this slide is shown.
 */
@Immutable
internal data class CollectiblesStorySlideUM(
    val title: TextReference,
    val subtitle: TextReference,
    val continueButtonText: TextReference,
)
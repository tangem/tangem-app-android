package com.tangem.features.collectibles.impl.nftdetails.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface CollectiblesNftDetailsUM {

    data object Loading : CollectiblesNftDetailsUM

    /**
     * @property buyback the offer while its window is open; null for a card in the collection or once it expires
     * @property cardAspect the front face's height to width; null until the faces are preloaded, hiding the case
     */
    data class Content(
        val frontImageUrl: String?,
        val backImageUrl: String?,
        val title: String,
        val subtitle: String,
        val details: ImmutableList<DetailRowUM>,
        val buyback: BuybackUM?,
        val cardAspect: Float?,
    ) : CollectiblesNftDetailsUM

    data class BuybackUM(val timeLeft: TextReference, val buttonText: TextReference)

    data class DetailRowUM(val label: TextReference, val value: String)
}
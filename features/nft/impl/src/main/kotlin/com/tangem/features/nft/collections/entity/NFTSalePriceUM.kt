package com.tangem.features.nft.collections.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class NFTSalePriceUM {
    data object Loading : NFTSalePriceUM()
    data object Failed : NFTSalePriceUM()
    data class Content(val price: TextReference) : NFTSalePriceUM()
}
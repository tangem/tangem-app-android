package com.tangem.features.nft.collections.entity

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class NFTSalePriceUM {
    data object Loading : NFTSalePriceUM()
    data object Failed : NFTSalePriceUM()
    data class Content(val price: String) : NFTSalePriceUM()
}
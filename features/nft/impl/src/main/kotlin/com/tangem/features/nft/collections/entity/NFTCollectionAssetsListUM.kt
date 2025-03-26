package com.tangem.features.nft.collections.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class NFTCollectionAssetsListUM {
    data object Collapsed : NFTCollectionAssetsListUM()

    @Immutable
    sealed class Expanded : NFTCollectionAssetsListUM() {
        data class Loading(val itemsCount: Int) : Expanded()
        data class Failed(val onRetryClick: () -> Unit) : Expanded()
        data class Content(val items: ImmutableList<NFTCollectionAssetUM>) : Expanded()
    }
}
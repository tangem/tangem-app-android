package com.tangem.features.nft.collections.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed class NFTCollectionAssetsListUM {
    data object Init : NFTCollectionAssetsListUM()
    data class Loading(val itemsCount: Int) : NFTCollectionAssetsListUM()
    data class Failed(val onRetryClick: () -> Unit) : NFTCollectionAssetsListUM()
    data class Content(val items: ImmutableList<NFTCollectionAssetUM>) : NFTCollectionAssetsListUM()
}
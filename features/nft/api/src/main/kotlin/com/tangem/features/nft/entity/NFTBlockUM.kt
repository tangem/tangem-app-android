package com.tangem.features.nft.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed interface NFTBlockUM {

    data object Hidden : NFTBlockUM

    data object Loading : NFTBlockUM

    data class Empty(
        val onItemClick: () -> Unit,
    ) : NFTBlockUM

    data object Failed : NFTBlockUM

    @Immutable
    data class Content(
        val previews: ImmutableList<CollectionPreview>,
        val collectionsCount: Int,
        val allAssetsCount: Int,
        val noCollectionAssetsCount: Int,
        val isFlickering: Boolean,
        val onItemClick: () -> Unit,
    ) : NFTBlockUM {

        @Immutable
        sealed class CollectionPreview {
            data class Image(val url: String) : CollectionPreview()
            data object More : CollectionPreview()
        }
    }
}
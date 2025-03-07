package com.tangem.core.ui.components.nft.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed class WalletNFTItemUM {

    data object Hidden : WalletNFTItemUM()

    data object Loading : WalletNFTItemUM()

    data object Empty : WalletNFTItemUM()

    data object Failed : WalletNFTItemUM()

    data class Content(
        val previews: ImmutableList<CollectionPreview>,
        val collectionsCount: Int,
        val assetsCount: Int,
        val isFlickering: Boolean,
    ) : WalletNFTItemUM() {

        sealed class CollectionPreview {
            data class Image(val url: String) : CollectionPreview()
            data object More : CollectionPreview()
        }
    }
}
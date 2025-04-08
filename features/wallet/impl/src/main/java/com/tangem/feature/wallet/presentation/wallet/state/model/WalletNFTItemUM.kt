package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
sealed class WalletNFTItemUM {

    data object Hidden : WalletNFTItemUM()

    data object Loading : WalletNFTItemUM()

    data class Empty(
        val onItemClick: () -> Unit,
    ) : WalletNFTItemUM()

    data object Failed : WalletNFTItemUM()

    data class Content(
        val previews: ImmutableList<CollectionPreview>,
        val collectionsCount: Int,
        val assetsCount: Int,
        val isFlickering: Boolean,
        val onItemClick: () -> Unit,
    ) : WalletNFTItemUM() {

        @Immutable
        sealed class CollectionPreview {
            data class Image(val url: String) : CollectionPreview()
            data object More : CollectionPreview()
        }
    }
}
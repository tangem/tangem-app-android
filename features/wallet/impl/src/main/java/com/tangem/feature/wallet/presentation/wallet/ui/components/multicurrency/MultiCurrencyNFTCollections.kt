package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNFTItemUM
import com.tangem.feature.wallet.presentation.wallet.ui.components.WalletNFTItem

private const val NFT_COLLECTIONS_CONTENT_TYPE = "NFTCollections"

internal fun LazyListScope.nftCollections(state: WalletNFTItemUM, modifier: Modifier = Modifier) {
    item(key = NFT_COLLECTIONS_CONTENT_TYPE, contentType = NFT_COLLECTIONS_CONTENT_TYPE) {
        WalletNFTItem(
            modifier = modifier,
            state = state,
        )
    }
}
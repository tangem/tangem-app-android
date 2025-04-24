package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.nft.models.*
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNFTItemUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import kotlinx.collections.immutable.toPersistentList

internal class SetNFTCollectionsTransformer(
    userWalletId: UserWalletId,
    private val nftCollections: List<NFTCollections>,
    private val onItemClick: () -> Unit,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState = when (prevState) {
        is WalletState.MultiCurrency.Content -> prevState.copy(
            nftState = when {
                nftCollections.allLoadedCollectionsEmpty() ->
                    WalletNFTItemUM.Empty(onItemClick)
                else -> createContentNFTItemUM(onItemClick)
            },
        )
        is WalletState.SingleCurrency.Content,
        is WalletState.Visa.Content,
        is WalletState.MultiCurrency.Locked,
        is WalletState.SingleCurrency.Locked,
        is WalletState.Visa.Locked,
        is WalletState.Visa.AccessTokenLocked,
        -> prevState
    }

    private fun createContentNFTItemUM(onItemClick: () -> Unit): WalletNFTItemUM.Content {
        val collectionsContent = nftCollections
            .map { it.content }
            .filterIsInstance<NFTCollections.Content.Collections>()

        val collections = collectionsContent
            .mapNotNull { it.collections }
            .flatten()

        return WalletNFTItemUM.Content(
            previews = if (collections.size > NFT_COLLECTIONS_MAX_PREVIEWS_COUNT) {
                collections
                    .take(NFT_COLLECTIONS_MAX_PREVIEWS_COUNT - 1)
                    .map { WalletNFTItemUM.Content.CollectionPreview.Image(it.logoUrl.orEmpty()) }
                    .plus(WalletNFTItemUM.Content.CollectionPreview.More)
                    .toPersistentList()
            } else {
                collections
                    .take(NFT_COLLECTIONS_MAX_PREVIEWS_COUNT)
                    .map { WalletNFTItemUM.Content.CollectionPreview.Image(it.logoUrl.orEmpty()) }
                    .toPersistentList()
            },
            collectionsCount = collections.size,
            assetsCount = collections
                .sumOf { it.count },
            isFlickering = false,
            onItemClick = onItemClick,
        )
    }

    companion object {
        private const val NFT_COLLECTIONS_MAX_PREVIEWS_COUNT = 4
    }
}
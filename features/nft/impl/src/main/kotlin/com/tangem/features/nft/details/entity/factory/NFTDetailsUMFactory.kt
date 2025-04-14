package com.tangem.features.nft.details.entity.factory

import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTSalePrice
import com.tangem.features.nft.details.entity.NFTAssetUM
import com.tangem.features.nft.details.entity.NFTDetailsUM
import com.tangem.features.nft.impl.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class NFTDetailsUMFactory(
    private val onBackClick: () -> Unit,
    private val onReadMoreClick: () -> Unit,
    private val onSeeAllTraitsClick: () -> Unit,
    private val onExploreClick: () -> Unit,
    private val onSendClick: () -> Unit,
) {

    fun getInitialState(nftAsset: NFTAsset): NFTDetailsUM = NFTDetailsUM(
        nftAsset = nftAsset.transform(),
        onBackClick = onBackClick,
        onReadMoreClick = onReadMoreClick,
        onSeeAllTraitsClick = onSeeAllTraitsClick,
        onExploreClick = onExploreClick,
        onSendClick = onSendClick,
    )

    private fun NFTAsset.transform(): NFTAssetUM = NFTAssetUM(
        name = name.orEmpty(),
        media = media?.let {
            NFTAssetUM.Media.Content(
                mimetype = it.mimetype,
                url = it.url,
            )
        } ?: NFTAssetUM.Media.Empty,
        topInfo = when {
            salePrice is NFTSalePrice.Empty && description.isNullOrEmpty() && rarity == null -> {
                NFTAssetUM.TopInfo.Empty
            }
            else -> {
                val hasSalePrice = salePrice !is NFTSalePrice.Empty
                val hasDescription = !description.isNullOrEmpty()
                val rarity = rarity
                NFTAssetUM.TopInfo.Content(
                    title = if (hasSalePrice) {
                        resourceReference(R.string.nft_details_last_sale_price)
                    } else {
                        null
                    },
                    salePrice = NFTAssetUM.SalePrice.Empty,
                    description = description,
                    rarity = if (rarity != null) {
                        NFTAssetUM.Rarity.Content(
                            rank = rarity.rank,
                            label = rarity.label,
                            showDivider = hasSalePrice || hasDescription,
                        )
                    } else {
                        NFTAssetUM.Rarity.Empty
                    },
                )
            }
        },
        traits = traits.take(MAX_TRAITS_COUNT).map {
            NFTAssetUM.BlockItem(
                title = stringReference(it.name),
                value = it.value,
            )
        }.toImmutableList(),
        baseInfoItems = buildBaseInfoItems(),
    )

    private fun NFTAsset.buildBaseInfoItems() = when (val id = id) {
        is NFTAsset.Identifier.EVM -> persistentListOf(
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_token_standard),
                value = contractType,
            ),
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_contract_address),
                value = id.tokenAddress,
                valueTextEllipsis = TextEllipsis.Middle,
            ),
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_token_id),
                value = id.tokenId,
            ),
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_chain),
                value = network.name,
            ),
        )
        is NFTAsset.Identifier.TON -> persistentListOf(
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_token_address),
                value = id.tokenAddress,
                valueTextEllipsis = TextEllipsis.Middle,
            ),
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_chain),
                value = network.name,
            ),
        )
        is NFTAsset.Identifier.Solana -> persistentListOf(
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_token_address),
                value = id.tokenAddress,
                valueTextEllipsis = TextEllipsis.Middle,
            ),
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_chain),
                value = network.name,
            ),
        )
        is NFTAsset.Identifier.Unknown -> persistentListOf()
    }

    companion object {
        private const val MAX_TRAITS_COUNT = 6
    }
}
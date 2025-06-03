package com.tangem.features.nft.details.entity.factory

import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.nft.models.NFTSalePrice
import com.tangem.features.nft.details.entity.NFTAssetUM
import com.tangem.features.nft.details.entity.NFTDetailsUM
import com.tangem.features.nft.impl.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Suppress("LongParameterList")
internal class NFTDetailsUMFactory(
    private val appCurrency: AppCurrency,
    private val onBackClick: () -> Unit,
    private val onReadMoreClick: () -> Unit,
    private val onSeeAllTraitsClick: () -> Unit,
    private val onExploreClick: () -> Unit,
    private val onSendClick: () -> Unit,
    private val onRefresh: () -> Unit,
    private val onInfoBlockClick: (title: TextReference, text: TextReference) -> Unit,
) {

    fun getInitialState(nftAsset: NFTAsset): NFTDetailsUM = NFTDetailsUM(
        nftAsset = nftAsset.transform(),
        pullToRefreshConfig = PullToRefreshConfig(
            isRefreshing = false,
            onRefresh = { onRefresh() },
        ),
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
            !hasSalePrice() && description.isNullOrEmpty() && rarity == null -> {
                NFTAssetUM.TopInfo.Empty
            }
            else -> {
                val hasSalePrice = hasSalePrice()
                val hasDescription = !description.isNullOrEmpty()
                val rarity = rarity
                NFTAssetUM.TopInfo.Content(
                    title = if (hasSalePrice) {
                        resourceReference(R.string.nft_details_last_sale_price)
                    } else {
                        null
                    },
                    salePrice = toSalePrice(),
                    description = description,
                    rarity = if (rarity != null) {
                        NFTAssetUM.Rarity.Content(
                            rank = rarity.rank,
                            label = rarity.label,
                            showDivider = hasSalePrice || hasDescription,
                            onLabelClick = {
                                onInfoBlockClick(
                                    resourceReference(R.string.nft_details_rarity_label),
                                    resourceReference(R.string.nft_details_info_rarity_label),
                                )
                            },
                            onRankClick = {
                                onInfoBlockClick(
                                    resourceReference(R.string.nft_details_rarity_rank),
                                    resourceReference(R.string.nft_details_info_rarity_rank),
                                )
                            },
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
                showInfoButton = false,
            )
        }.toImmutableList(),
        showAllTraitsButton = traits.size > MAX_TRAITS_COUNT,
        baseInfoItems = buildBaseInfoItems(),
    )

    private fun NFTAsset.hasSalePrice() = salePrice !is NFTSalePrice.Empty && salePrice !is NFTSalePrice.Error

    private fun NFTAsset.toSalePrice() = when (val price = salePrice) {
        is NFTSalePrice.Empty,
        is NFTSalePrice.Error,
        -> NFTAssetUM.SalePrice.Empty
        is NFTSalePrice.Loading -> NFTAssetUM.SalePrice.Loading
        is NFTSalePrice.Value -> NFTAssetUM.SalePrice.Content(
            isFlickering = false,
            cryptoPrice = stringReference(
                price.value.format {
                    crypto(
                        symbol = price.symbol,
                        decimals = price.decimals,
                    )
                },
            ),
            fiatPrice = stringReference(
                price.fiatValue.format {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                    )
                },
            ),
        )
    }

    @Suppress("LongMethod")
    private fun NFTAsset.buildBaseInfoItems() = when (val id = id) {
        is NFTAsset.Identifier.EVM -> persistentListOf(
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_token_standard),
                value = contractType,
                showInfoButton = true,
                onClick = {
                    onInfoBlockClick(
                        resourceReference(R.string.nft_details_token_standard),
                        resourceReference(R.string.nft_details_info_token_standard),
                    )
                },
            ),
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_contract_address),
                value = id.tokenAddress,
                valueTextEllipsis = TextEllipsis.Middle,
                showInfoButton = true,
                onClick = {
                    onInfoBlockClick(
                        resourceReference(R.string.nft_details_contract_address),
                        resourceReference(R.string.nft_details_info_contract_address),
                    )
                },
            ),
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_token_id),
                value = id.tokenId.toString(),
                valueTextEllipsis = TextEllipsis.Middle,
                showInfoButton = true,
                onClick = {
                    onInfoBlockClick(
                        resourceReference(R.string.nft_details_token_id),
                        resourceReference(R.string.nft_details_info_token_id),
                    )
                },
            ),
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_chain),
                value = network.name,
                showInfoButton = true,
                onClick = {
                    onInfoBlockClick(
                        resourceReference(R.string.nft_details_chain),
                        resourceReference(R.string.nft_details_info_chain),
                    )
                },
            ),
        )
        is NFTAsset.Identifier.TON -> persistentListOf(
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_token_address),
                value = id.tokenAddress,
                valueTextEllipsis = TextEllipsis.Middle,
                showInfoButton = true,
                onClick = {
                    onInfoBlockClick(
                        resourceReference(R.string.nft_details_token_address),
                        resourceReference(R.string.nft_details_info_token_address),
                    )
                },
            ),
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_chain),
                value = network.name,
                showInfoButton = true,
                onClick = {
                    onInfoBlockClick(
                        resourceReference(R.string.nft_details_chain),
                        resourceReference(R.string.nft_details_info_chain),
                    )
                },
            ),
        )
        is NFTAsset.Identifier.Solana -> persistentListOf(
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_token_address),
                value = id.tokenAddress,
                valueTextEllipsis = TextEllipsis.Middle,
                showInfoButton = true,
                onClick = {
                    onInfoBlockClick(
                        resourceReference(R.string.nft_details_token_address),
                        resourceReference(R.string.nft_details_info_token_address),
                    )
                },
            ),
            NFTAssetUM.BlockItem(
                title = resourceReference(R.string.nft_details_chain),
                value = network.name,
                showInfoButton = true,
                onClick = {
                    onInfoBlockClick(
                        resourceReference(R.string.nft_details_chain),
                        resourceReference(R.string.nft_details_info_chain),
                    )
                },
            ),
        )
        is NFTAsset.Identifier.Unknown -> persistentListOf()
    }

    companion object {
        private const val MAX_TRAITS_COUNT = 6
    }
}
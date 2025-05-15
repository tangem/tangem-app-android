package com.tangem.features.nft.collections.entity.transformer

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.nft.models.*
import com.tangem.features.nft.collections.entity.*
import com.tangem.features.nft.impl.R
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Suppress("LongParameterList")
internal class UpdateDataStateTransformer(
    private val nftCollections: List<NFTCollections>,
    private val onReceiveClick: () -> Unit,
    private val onRetryClick: () -> Unit,
    private val onExpandCollectionClick: (NFTCollection) -> Unit,
    private val onRetryAssetsClick: (NFTCollection) -> Unit,
    private val onAssetClick: (NFTAsset, String) -> Unit,
    private val initialSearchBarFactory: () -> SearchBarUM,
    private val collectionIdProvider: NFTCollection.() -> String,
) : Transformer<NFTCollectionsStateUM> {

    override fun transform(prevState: NFTCollectionsStateUM): NFTCollectionsStateUM {
        val content = when {
            nftCollections.allCollectionsFailed() ->
                NFTCollectionsUM.Failed(onRetryClick, onReceiveClick)
            nftCollections.anyCollectionFailed() && nftCollections.allLoadedCollectionsEmpty() ->
                NFTCollectionsUM.Failed(onRetryClick, onReceiveClick)
            nftCollections.allCollectionsLoaded() && nftCollections.allCollectionsEmpty() ->
                NFTCollectionsUM.Empty(onReceiveClick)
            !nftCollections.allCollectionsLoaded() && nftCollections.allCollectionsEmpty() ->
                NFTCollectionsUM.Loading(
                    onReceiveClick = onReceiveClick,
                    search = SearchBarUM(
                        placeholderText = resourceReference(R.string.common_search),
                        query = "",
                        isActive = false,
                        onQueryChange = { },
                        onActiveChange = { },
                    ),
                )
            else -> {
                NFTCollectionsUM.Content(
                    search = if (prevState.content is NFTCollectionsUM.Content) {
                        prevState.content.search
                    } else {
                        initialSearchBarFactory()
                    },
                    collections = nftCollections
                        .map { it.content }
                        .asSequence()
                        .filterIsInstance<NFTCollections.Content.Collections>()
                        .map { it.collections.orEmpty().transform(prevState) }
                        .flatten()
                        .toPersistentList(),
                    warnings = transformNotifications(),
                    onReceiveClick = onReceiveClick,
                )
            }
        }
        return prevState.copy(
            content = content,
            pullToRefreshConfig = when (content) {
                is NFTCollectionsUM.Loading -> prevState.pullToRefreshConfig.copy(
                    isRefreshing = false,
                )
                else -> prevState.pullToRefreshConfig
            },
        )
    }

    private fun List<NFTCollection>.transform(state: NFTCollectionsStateUM): ImmutableList<NFTCollectionUM> = map {
        NFTCollectionUM(
            id = it.collectionIdProvider(),
            networkIconId = getActiveIconRes(it.network.id.value),
            name = it.name.orEmpty(),
            description = TextReference.PluralRes(
                R.plurals.nft_collections_count,
                it.count,
                wrappedList(it.count),
            ),
            logoUrl = it.logoUrl,
            assets = it.transformAssets(),
            onExpandClick = {
                onExpandCollectionClick(it)
            },
            isExpanded = it.isExpanded(state),
        )
    }.toPersistentList()

    private fun transformNotifications(): ImmutableList<NFTCollectionsWarningUM> = buildList {
        if (nftCollections.anyCollectionFailed()) {
            add(
                NFTCollectionsWarningUM(
                    id = "loading troubles",
                    config = NotificationConfig(
                        title = TextReference.Res(R.string.nft_collections_warning_title),
                        subtitle = TextReference.Res(R.string.nft_collections_warning_subtitle),
                        iconResId = R.drawable.ic_alert_triangle_20,
                    ),
                ),
            )
        }
    }.toPersistentList()

    private fun NFTCollection.transformAssets(): NFTCollectionAssetsListUM = when (val assets = this.assets) {
        is NFTCollection.Assets.Empty -> NFTCollectionAssetsListUM.Init
        is NFTCollection.Assets.Loading -> NFTCollectionAssetsListUM.Loading(count)
        is NFTCollection.Assets.Failed -> NFTCollectionAssetsListUM.Failed { onRetryAssetsClick(this) }
        is NFTCollection.Assets.Value -> NFTCollectionAssetsListUM.Content(
            items = assets
                .items
                .map { it.transform(name.orEmpty()) }
                .toPersistentList(),
        )
    }

    private fun NFTAsset.transform(collectionName: String): NFTCollectionAssetUM {
        return NFTCollectionAssetUM(
            id = id.toString(),
            name = name.orEmpty(),
            imageUrl = media?.url,
            price = when (val salePrice = salePrice) {
                is NFTSalePrice.Empty -> NFTSalePriceUM.Failed
                is NFTSalePrice.Loading -> NFTSalePriceUM.Loading
                is NFTSalePrice.Error -> NFTSalePriceUM.Failed
                is NFTSalePrice.Value -> NFTSalePriceUM.Content(
                    price = stringReference(
                        salePrice.value.format {
                            crypto(
                                symbol = salePrice.symbol,
                                decimals = salePrice.decimals,
                            )
                        },
                    ),
                )
            },
            onItemClick = {
                onAssetClick(this, collectionName)
            },
        )
    }

    private fun NFTCollection.isExpanded(state: NFTCollectionsStateUM): Boolean =
        (state.content as? NFTCollectionsUM.Content)
            ?.collections
            ?.firstOrNull { it.id == this.collectionIdProvider() }
            ?.isExpanded
            ?: false
}
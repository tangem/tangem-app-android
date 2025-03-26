package com.tangem.features.nft.collections.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.TangemSearchBarDefaults
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.collections.entity.*
import com.tangem.features.nft.collections.entity.NFTCollectionAssetUM
import com.tangem.features.nft.collections.entity.NFTCollectionAssetsListUM
import com.tangem.features.nft.collections.entity.NFTCollectionUM
import com.tangem.features.nft.collections.entity.NFTCollectionsUM
import com.tangem.features.nft.collections.entity.NFTSalePriceUM
import com.tangem.features.nft.impl.R
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongMethod")
@Composable
internal fun NFTCollectionsContent(state: NFTCollectionsUM.Content, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.secondary)
            .padding(
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
    ) {
        Column(
            modifier = Modifier,
        ) {
            SearchBar(
                state = state.search,
                colors = TangemSearchBarDefaults.secondaryTextFieldColors,
            )
            state.warnings.fastForEach {
                NFTCollectionWarning(
                    modifier = Modifier
                        .padding(top = TangemTheme.dimens.spacing16),
                    state = it,
                )
            }
            LazyColumn(
                modifier = Modifier
                    .padding(
                        top = TangemTheme.dimens.spacing16,
                    )
                    .clip(TangemTheme.shapes.roundedCornersXMedium)
                    .background(TangemTheme.colors.background.primary),
                state = listState,
            ) {
                state.collections.fastForEach { collection ->
                    item(key = collection.id) {
                        NFTCollection(
                            modifier = Modifier.fillMaxWidth(),
                            state = collection,
                        )
                    }
                    when (val assets = collection.assets) {
                        is NFTCollectionAssetsListUM.Collapsed -> Unit
                        is NFTCollectionAssetsListUM.Expanded.Loading -> {
                            assetsListLoading(
                                collectionId = collection.id,
                                content = assets,
                            )
                        }
                        is NFTCollectionAssetsListUM.Expanded.Failed -> {
                            assetsListFailed(
                                collectionId = collection.id,
                                content = assets,
                            )
                        }
                        is NFTCollectionAssetsListUM.Expanded.Content -> {
                            assetsListContent(
                                collectionId = collection.id,
                                content = assets,
                            )
                        }
                    }
                }
            }
        }
        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            text = stringResourceSafe(R.string.nft_collections_receive),
            onClick = { },
        )
    }
}

private fun LazyListScope.assetsListLoading(
    collectionId: String,
    content: NFTCollectionAssetsListUM.Expanded.Loading,
) {
    val itemsCount = content.itemsCount
    val rowCount = (itemsCount + 1) / 2
    repeat(rowCount) { rowIndex ->
        item(
            key = "loading_${collectionId}_$rowIndex",
        ) {
            val paddingValues = PaddingValues(
                start = TangemTheme.dimens.spacing6,
                top = TangemTheme.dimens.spacing6,
                end = TangemTheme.dimens.spacing6,
                bottom = TangemTheme.dimens.spacing20,
            )
            Row(
                modifier = Modifier
                    .padding(TangemTheme.dimens.spacing6)
                    .animateItem(),
            ) {
                NFTCollectionAssetLoading(
                    modifier = Modifier
                        .weight(1f)
                        .padding(paddingValues),
                )
                if (rowIndex == rowCount - 1 && itemsCount % 2 != 0) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(paddingValues),
                    )
                } else {
                    NFTCollectionAssetLoading(
                        modifier = Modifier
                            .weight(1f)
                            .padding(paddingValues),
                    )
                }
            }
        }
    }
}

private fun LazyListScope.assetsListFailed(collectionId: String, content: NFTCollectionAssetsListUM.Expanded.Failed) {
    item(
        key = "failed_$collectionId",
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TangemTheme.dimens.size142),
            contentAlignment = Alignment.Center,
        ) {
            UnableToLoadData(
                onRetryClick = content.onRetryClick,
            )
        }
    }
}

private fun LazyListScope.assetsListContent(
    collectionId: String,
    content: NFTCollectionAssetsListUM.Expanded.Content,
) {
    val items = content.items
    val itemsCount = items.size
    val rowCount = (itemsCount + 1) / 2
    repeat(rowCount) { rowIndex ->
        val item1 = items[rowIndex * 2]
        val item2 = items.getOrNull(rowIndex * 2 + 1)
        item(
            key = "content_${collectionId}_${item1.id}_${item2?.id}",
        ) {
            val paddingValues = PaddingValues(
                start = TangemTheme.dimens.spacing6,
                top = TangemTheme.dimens.spacing6,
                end = TangemTheme.dimens.spacing6,
                bottom = TangemTheme.dimens.spacing20,
            )
            Row(
                modifier = Modifier
                    .padding(TangemTheme.dimens.spacing6)
                    .animateItem(),
            ) {
                NFTCollectionAsset(
                    modifier = Modifier
                        .weight(1f)
                        .padding(paddingValues),
                    state = item1,
                )
                if (item2 == null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(paddingValues),
                    )
                } else {
                    NFTCollectionAsset(
                        modifier = Modifier
                            .weight(1f)
                            .padding(paddingValues),
                        state = item2,
                    )
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTCollectionsContent() {
    TangemThemePreview {
        NFTCollectionsContent(
            state = NFTCollectionsUM.Content(
                search = SearchBarUM(
                    placeholderText = resourceReference(R.string.common_search),
                    query = "",
                    onQueryChange = {},
                    isActive = false,
                    onActiveChange = { },
                ),
                collections = persistentListOf(
                    NFTCollectionUM(
                        id = "item1",
                        name = "Nethers",
                        logoUrl = "",
                        networkIconId = R.drawable.img_eth_22,
                        description = TextReference.Str("3 items"),
                        assets = NFTCollectionAssetsListUM.Collapsed,
                        onExpandClick = { },
                    ),
                    NFTCollectionUM(
                        id = "item2",
                        name = "Nethers",
                        logoUrl = "",
                        networkIconId = R.drawable.img_eth_22,
                        description = TextReference.Str("3 items"),
                        assets = NFTCollectionAssetsListUM.Expanded.Loading(
                            itemsCount = 1,
                        ),
                        onExpandClick = { },
                    ),
                    NFTCollectionUM(
                        id = "item3",
                        name = "Nethers",
                        logoUrl = "",
                        networkIconId = R.drawable.img_eth_22,
                        description = TextReference.Str("3 items"),
                        assets = NFTCollectionAssetsListUM.Expanded.Failed(
                            onRetryClick = { },
                        ),
                        onExpandClick = { },
                    ),
                    NFTCollectionUM(
                        id = "item4",
                        name = "Nethers",
                        logoUrl = "",
                        networkIconId = R.drawable.img_eth_22,
                        description = TextReference.Str("3 items"),
                        assets = NFTCollectionAssetsListUM.Expanded.Content(
                            items = persistentListOf(
                                NFTCollectionAssetUM(
                                    id = "item1",
                                    name = "Nethers #0854",
                                    imageUrl = "img",
                                    price = NFTSalePriceUM.Content("0.05 ETH"),
                                    onItemClick = { },
                                ),
                                NFTCollectionAssetUM(
                                    id = "item2",
                                    name = "Nethers #0855",
                                    imageUrl = "img",
                                    price = NFTSalePriceUM.Loading,
                                    onItemClick = { },
                                ),
                                NFTCollectionAssetUM(
                                    id = "item3",
                                    name = "Nethers #0856",
                                    imageUrl = "img",
                                    price = NFTSalePriceUM.Failed,
                                    onItemClick = { },
                                ),
                            ),
                        ),
                        onExpandClick = { },
                    ),
                ),
                warnings = persistentListOf(
                    NFTCollectionsWarningUM(
                        config = NotificationConfig(
                            title = TextReference.Res(R.string.nft_collections_warning_title),
                            subtitle = TextReference.Res(R.string.nft_collections_warning_subtitle),
                            iconResId = R.drawable.ic_alert_triangle_20,
                        ),
                    ),
                ),
                onReceiveClick = { },
            ),
        )
    }
}

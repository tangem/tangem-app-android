package com.tangem.features.nft.collections.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
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
import com.tangem.features.nft.impl.R
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongMethod")
@Composable
internal fun NFTCollectionsContent(content: NFTCollectionsUM.Content, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    val bottomPadding = TangemTheme.dimens.spacing60

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
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            SearchBar(
                state = content.search,
                colors = TangemSearchBarDefaults.secondaryTextFieldColors,
            )
            if (content.collections.isEmpty()) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding),
                ) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.Center),
                        text = stringResourceSafe(id = R.string.nft_empty_search),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.tertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                content.warnings.fastForEach {
                    key(it.id) {
                        NFTCollectionWarning(
                            modifier = Modifier
                                .padding(top = TangemTheme.dimens.spacing16),
                            state = it,
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = TangemTheme.dimens.spacing16,
                            bottom = bottomPadding,
                        )
                        .clip(TangemTheme.shapes.roundedCornersXMedium)
                        .background(TangemTheme.colors.background.primary),
                    state = listState,
                ) {
                    content.collections.fastForEach { collection ->
                        item(key = collection.id) {
                            NFTCollection(
                                modifier = Modifier.fillMaxWidth(),
                                state = collection,
                            )
                        }
                        when (val assets = collection.assets) {
                            is NFTCollectionAssetsListUM.Init -> Unit
                            is NFTCollectionAssetsListUM.Loading -> {
                                assetsListLoading(
                                    collectionId = collection.id,
                                    content = assets,
                                    expanded = collection.isExpanded,
                                )
                            }
                            is NFTCollectionAssetsListUM.Failed -> {
                                assetsListFailed(
                                    collectionId = collection.id,
                                    content = assets,
                                    expanded = collection.isExpanded,
                                )
                            }
                            is NFTCollectionAssetsListUM.Content -> {
                                assetsListContent(
                                    collectionId = collection.id,
                                    content = assets,
                                    expanded = collection.isExpanded,
                                )
                            }
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
            onClick = content.onReceiveClick,
        )
    }
}

private fun LazyListScope.assetsListLoading(
    collectionId: String,
    content: NFTCollectionAssetsListUM.Loading,
    expanded: Boolean,
) {
    val itemsCount = content.itemsCount
    val rowCount = (itemsCount + 1) / 2
    repeat(rowCount) { rowIndex ->
        item(
            key = "loading_${collectionId}_$rowIndex",
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val paddingValues = PaddingValues(
                    start = TangemTheme.dimens.spacing6,
                    top = TangemTheme.dimens.spacing6,
                    end = TangemTheme.dimens.spacing6,
                    bottom = TangemTheme.dimens.spacing20,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(TangemTheme.dimens.spacing6),
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
}

private fun LazyListScope.assetsListFailed(
    collectionId: String,
    content: NFTCollectionAssetsListUM.Failed,
    expanded: Boolean,
) {
    item(
        key = "failed_$collectionId",
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
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
}

private fun LazyListScope.assetsListContent(
    collectionId: String,
    content: NFTCollectionAssetsListUM.Content,
    expanded: Boolean,
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
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val paddingValues = PaddingValues(
                    start = TangemTheme.dimens.spacing6,
                    top = TangemTheme.dimens.spacing6,
                    end = TangemTheme.dimens.spacing6,
                    bottom = TangemTheme.dimens.spacing20,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(TangemTheme.dimens.spacing6),
                ) {
                    NFTCollectionAsset(
                        modifier = Modifier
                            .weight(1f)
                            .clip(TangemTheme.shapes.roundedCornersXMedium)
                            .clickable { item1.onItemClick() }
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
                                .clip(TangemTheme.shapes.roundedCornersXMedium)
                                .clickable { item2.onItemClick() }
                                .padding(paddingValues),
                            state = item2,
                        )
                    }
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
            content = NFTCollectionsUM.Content(
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
                        assets = NFTCollectionAssetsListUM.Content(persistentListOf()),
                        isExpanded = false,
                        onExpandClick = { },
                    ),
                    NFTCollectionUM(
                        id = "item2",
                        name = "Nethers",
                        logoUrl = "",
                        networkIconId = R.drawable.img_eth_22,
                        description = TextReference.Str("3 items"),
                        assets = NFTCollectionAssetsListUM.Loading(
                            itemsCount = 1,
                        ),
                        isExpanded = true,
                        onExpandClick = { },
                    ),
                    NFTCollectionUM(
                        id = "item3",
                        name = "Nethers",
                        logoUrl = "",
                        networkIconId = R.drawable.img_eth_22,
                        description = TextReference.Str("3 items"),
                        assets = NFTCollectionAssetsListUM.Failed(
                            onRetryClick = { },
                        ),
                        isExpanded = true,
                        onExpandClick = { },
                    ),
                    NFTCollectionUM(
                        id = "item4",
                        name = "Nethers",
                        logoUrl = "",
                        networkIconId = R.drawable.img_eth_22,
                        description = TextReference.Str("3 items"),
                        assets = NFTCollectionAssetsListUM.Content(
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
                        isExpanded = true,
                        onExpandClick = { },
                    ),
                ),
                warnings = persistentListOf(
                    NFTCollectionsWarningUM(
                        id = "loading troubles",
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
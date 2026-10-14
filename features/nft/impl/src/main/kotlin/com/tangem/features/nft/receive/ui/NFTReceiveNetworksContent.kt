package com.tangem.features.nft.receive.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.rows.ChainRow
import com.tangem.core.ui.components.rows.model.ChainRowUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.nft.impl.R
import com.tangem.features.nft.receive.entity.NFTNetworkUM
import com.tangem.features.nft.receive.entity.NFTReceiveUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun NFTReceiveNetworksContent(state: NFTReceiveUM.Networks.Content, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(
                vertical = 8.dp,
            ),
        state = listState,
        contentPadding = PaddingValues(
            horizontal = 16.dp,
        ),
    ) {
        if (state.availableItems.isNotEmpty()) {
            item(
                key = "available_title",
            ) {
                Title(resourceReference(R.string.nft_receive_available_section_title))
            }
            networks(state.availableItems)
        }
        if (state.unavailableItems.isNotEmpty()) {
            item(
                key = "not_available_title",
            ) {
                Title(resourceReference(R.string.common_not_added))
            }
            networks(state.unavailableItems)
        }
    }
}

private fun LazyListScope.networks(items: List<NFTNetworkUM>) {
    itemsIndexed(
        items = items,
        key = { _, item -> item.id },
    ) { index, network ->
        val isLastItem = index == items.size - 1
        ChainRow(
            modifier = Modifier
                .composed {
                    if (isLastItem) {
                        padding(
                            bottom = 8.dp,
                        )
                            .clip(
                                RoundedCornerShape(
                                    bottomStart = 16.dp,
                                    bottomEnd = 16.dp,
                                ),
                            )
                    } else {
                        this
                    }
                }
                .background(TangemTheme.colors.background.primary)
                .clickable { network.onItemClick() },
            model = network.chainRowUM,
        )
    }
}

@Composable
private fun Title(text: TextReference, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = 8.dp,
            )
            .background(
                color = TangemTheme.colors.background.primary,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                ),
            )
            .padding(
                start = 12.dp,
                top = 12.dp,
                end = 12.dp,
                bottom = 4.dp,
            ),
        text = text.resolveReference(),
        style = TangemTheme.typography.subtitle2,
        color = TangemTheme.colors.text.tertiary,
    )
}

@Suppress("LongMethod")
@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NFTReceiveNetworksUM() {
    TangemThemePreview {
        NFTReceiveNetworksContent(
            state = NFTReceiveUM.Networks.Content(
                availableItems = persistentListOf(
                    NFTNetworkUM(
                        id = "item1",
                        chainRowUM = ChainRowUM(
                            name = "Ethereum",
                            type = "",
                            icon = CurrencyIconState.CoinIcon(
                                url = null,
                                fallbackResId = R.drawable.img_eth_22,
                                isGrayscale = false,
                                shouldShowCustomBadge = false,
                            ),
                            shouldShowCustom = false,
                        ),
                        onItemClick = { },
                    ),
                    NFTNetworkUM(
                        id = "item2",
                        chainRowUM = ChainRowUM(
                            name = "Polygon",
                            type = "",
                            icon = CurrencyIconState.CoinIcon(
                                url = null,
                                fallbackResId = R.drawable.img_polygon_22,
                                isGrayscale = false,
                                shouldShowCustomBadge = false,
                            ),
                            shouldShowCustom = false,
                        ),
                        onItemClick = { },
                    ),
                    NFTNetworkUM(
                        id = "item3",
                        chainRowUM = ChainRowUM(
                            name = "BSC",
                            type = "",
                            icon = CurrencyIconState.CoinIcon(
                                url = null,
                                fallbackResId = R.drawable.img_bsc_22,
                                isGrayscale = false,
                                shouldShowCustomBadge = false,
                            ),
                            shouldShowCustom = false,
                        ),
                        onItemClick = { },
                    ),
                    NFTNetworkUM(
                        id = "item4",
                        chainRowUM = ChainRowUM(
                            name = "Avalanche",
                            type = "",
                            icon = CurrencyIconState.CoinIcon(
                                url = null,
                                fallbackResId = R.drawable.img_avalanche_22,
                                isGrayscale = false,
                                shouldShowCustomBadge = false,
                            ),
                            shouldShowCustom = false,
                        ),
                        onItemClick = { },
                    ),
                    NFTNetworkUM(
                        id = "item5",
                        chainRowUM = ChainRowUM(
                            name = "Avalanche",
                            type = "",
                            icon = CurrencyIconState.CoinIcon(
                                url = null,
                                fallbackResId = R.drawable.img_avalanche_22,
                                isGrayscale = false,
                                shouldShowCustomBadge = true,
                            ),
                            shouldShowCustom = true,
                        ),
                        onItemClick = { },
                    ),
                ),
                unavailableItems = persistentListOf(),
            ),
        )
    }
}
package com.tangem.features.nft.receive.ui

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.stringResourceSafe
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
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.primary)
            .animateContentSize(),
        state = listState,
    ) {
        item(
            key = "title",
        ) {
            Title()
        }
        items(
            items = state.items,
            key = NFTNetworkUM::id,
        ) { network ->
            NFTNetwork(
                state = network,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun Title(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .padding(
                start = TangemTheme.dimens.spacing12,
                top = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing4,
            ),
        text = stringResourceSafe(R.string.nft_receive_choose_network),
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
                items = persistentListOf(
                    NFTNetworkUM(
                        id = "item1",
                        name = "Ethereum",
                        iconRes = R.drawable.img_eth_22,
                        onItemClick = { },
                    ),
                    NFTNetworkUM(
                        id = "item2",
                        name = "Polygon",
                        iconRes = R.drawable.img_polygon_22,
                        onItemClick = { },
                    ),
                    NFTNetworkUM(
                        id = "item3",
                        name = "BSC",
                        iconRes = R.drawable.img_bsc_22,
                        onItemClick = { },
                    ),
                    NFTNetworkUM(
                        id = "item4",
                        name = "Avalanche",
                        iconRes = R.drawable.img_avalanche_22,
                        onItemClick = { },
                    ),
                ),
            ),
        )
    }
}
package com.tangem.features.addressbook.selectnetworks.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.addressbook.selectnetworks.ui.state.SelectNetworksUM
import com.tangem.features.addressbook.selectnetworks.ui.state.SelectNetworksUM.NetworkItemUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SelectNetworksContent(state: SelectNetworksUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors3.bg.primary)
            .systemBarsPadding(),
    ) {
        TangemTopBar(
            title = resourceReference(R.string.common_choose_network),
            startContent = {
                TangemButton(
                    iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_back_24),
                    onClick = state.onBackClick,
                    size = TangemButton.Size.X11,
                    variant = TangemButton.Variant.Material,
                )
            },
        )
        TangemSearch(
            state = state.searchBar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        val doneButtonVerticalPadding = 12.dp
        val doneButtonAreaHeight = 48.dp + doneButtonVerticalPadding * 2
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = doneButtonAreaHeight)
                    .background(
                        color = TangemTheme.colors3.bg.secondary,
                        shape = RoundedCornerShape(24.dp),
                    ),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                item {
                    Text(
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                        text = stringResourceSafe(R.string.common_available_networks),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.secondary,
                    )
                }
                items(items = state.networks, key = NetworkItemUM::id) { item ->
                    NetworkRow(item = item)
                }
            }
            TangemButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = doneButtonVerticalPadding)
                    .imePadding(),
                onClick = state.doneButton.onClick,
                isEnabled = state.doneButton.isEnabled,
                size = TangemButton.Size.X12,
                text = state.doneButton.text,
            )
        }
    }
}

@Composable
private fun NetworkRow(item: NetworkItemUM) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickableSingle(onClick = item.onCheckedChange)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(id = item.iconResId),
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
        )
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = item.name,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = item.symbol,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        SpacerWMax()
        TangemCheckmark(
            checked = item.isSelected,
            onCheckedChange = { item.onCheckedChange() },
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_SelectNetworksContent() {
    TangemThemePreviewRedesign {
        SelectNetworksContent(
            state = SelectNetworksUM(
                searchBar = TangemSearch.State(
                    placeholderText = resourceReference(R.string.common_search),
                    query = "",
                    onQueryChange = {},
                    isActive = false,
                    onActiveChange = {},
                    onCloseClick = {},
                ),
                networks = persistentListOf(
                    NetworkItemUM(
                        id = "ethereum",
                        name = "Ethereum",
                        symbol = "ETH",
                        iconResId = R.drawable.img_eth_22,
                        isSelected = true,
                        onCheckedChange = {},
                    ),
                    NetworkItemUM(
                        id = "bsc",
                        name = "BNB Smart Chain",
                        iconResId = R.drawable.img_bsc_22,
                        isSelected = false,
                        symbol = "BNB",
                        onCheckedChange = {},
                    ),
                    NetworkItemUM(
                        id = "polygon",
                        name = "Polygon",
                        iconResId = R.drawable.img_polygon_22,
                        isSelected = true,
                        symbol = "POL",
                        onCheckedChange = {},
                    ),
                ),
                doneButton = TangemButtonUM(
                    text = TextReference.Res(R.string.common_done),
                    type = TangemButtonType.Primary,
                    isEnabled = true,
                    onClick = {},
                ),
                onBackClick = {},
            ),
        )
    }
}
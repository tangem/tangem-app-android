package com.tangem.features.addressbook.selectnetworks.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_search_24
import com.tangem.features.addressbook.selectnetworks.ui.state.SelectNetworksUM
import com.tangem.features.addressbook.selectnetworks.ui.state.SelectNetworksUM.NetworkItemUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun SelectNetworksContent(state: SelectNetworksUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors3.bg.primary)
            .navigationBarsPadding(),
    ) {
        TangemTopNavigation(
            title = resourceReference(R.string.common_choose_network),
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            blurBackground = false,
            onBack = state.onBackClick,
        )
        TangemSearch(
            state = state.searchBar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
        AnimatedContent(
            targetState = state.networks.isNotEmpty(),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { hasNetworks ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (hasNetworks) {
                    Content(state = state)
                } else {
                    NothingFoundContent(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun BoxScope.Content(state: SelectNetworksUM, modifier: Modifier = Modifier) {
    val doneButtonVerticalPadding = 12.dp
    val doneButtonAreaHeight = 48.dp + doneButtonVerticalPadding * 2
    LazyColumn(
        modifier = modifier
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
            NetworksHeader(
                selectAllButton = state.selectAllButton,
                onSelectAllClick = state.onSelectAllClick,
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

@Composable
private fun NothingFoundContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color = TangemTheme.colors3.bg.opaque.primary, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.ic_search_24,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            modifier = Modifier.padding(top = 32.dp),
            text = stringResourceSafe(R.string.address_book_search_no_results),
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.caption.medium,
        )
    }
}

@Composable
private fun NetworksHeader(
    selectAllButton: SelectNetworksUM.SelectAllButtonUM,
    onSelectAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticManager = LocalHapticManager.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(top = 16.dp, bottom = 4.dp),
            text = stringResourceSafe(R.string.common_available_networks),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        AnimatedContent(
            targetState = selectAllButton,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 400)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 400))
            },
            label = "SelectAllButton",
        ) { button ->
            if (!button.text.isNullOrEmpty()) {
                Text(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                hapticManager.perform(TangemHapticEffect.View.SegmentTick)
                                onSelectAllClick()
                            },
                        )
                        .padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
                    text = button.text.resolveReference(),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.brand,
                )
            }
        }
    }
}

@Composable
private fun NetworkRow(item: NetworkItemUM) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = item.onCheckedChange,
            )
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
                selectAllButton = SelectNetworksUM.SelectAllButtonUM.SelectAll,
                doneButton = TangemButtonUM(
                    text = TextReference.Res(R.string.common_done),
                    type = TangemButtonType.Primary,
                    isEnabled = true,
                    onClick = {},
                ),
                onBackClick = {},
                onSelectAllClick = {},
            ),
        )
    }
}
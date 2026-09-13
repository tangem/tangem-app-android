package com.tangem.features.jointaccount.supportednetworks.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.bottomsheets.LocalBottomSheetContentScrollable
import com.tangem.core.ui.components.bottomsheets.LocalTangemBottomSheetContentBottomInset
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.jointaccount.supportednetworks.ui.state.JointSupportedNetworksUM
import com.tangem.features.jointaccount.supportednetworks.ui.state.JointSupportedNetworksUM.NetworkItemUM
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun JointSupportedNetworksBottomSheet(state: JointSupportedNetworksUM) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopNavigation(
                title = resourceReference(CoreUiR.string.common_supported_networks),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = state.onDismiss,
            )
        },
        content = {
            JointSupportedNetworksContent(state = state)
        },
        footer = {
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
                text = resourceReference(CoreUiR.string.common_got_it),
                onClick = state.onGotItClick,
            )
        },
    )
}

@Composable
private fun JointSupportedNetworksContent(state: JointSupportedNetworksUM, modifier: Modifier = Modifier) {
    val bottomInset = LocalTangemBottomSheetContentBottomInset.current
    val contentScrollable = LocalBottomSheetContentScrollable.current
    val lazyListState = rememberLazyListState()

    if (contentScrollable != null) {
        val isScrollable by remember {
            derivedStateOf { lazyListState.canScrollForward || lazyListState.canScrollBackward }
        }

        LaunchedEffect(isScrollable) {
            contentScrollable.value = isScrollable
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(color = TangemTheme.colors3.bg.tertiary),
        contentPadding = PaddingValues(bottom = bottomInset.coerceAtLeast(16.dp)),
    ) {
        items(items = state.networks, key = NetworkItemUM::id) { item ->
            NetworkRow(item = item)
        }
    }
}

@Composable
private fun NetworkRow(item: NetworkItemUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        contentLead = TangemRowContentLead.Start,
        startSlot = {
            Image(
                painter = painterResource(id = item.iconResId),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
        },
        titleSlot = {
            TangemRowText(text = item.name, role = TangemRowTextRole.Title)
            TangemRowText(
                text = item.symbol,
                role = TangemRowTextRole.Subtitle,
                modifier = Modifier.align(Alignment.Bottom),
            )
        },
    )
}

// region Preview
@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JointSupportedNetworksContentPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.secondary)) {
            TangemTopNavigation(
                title = resourceReference(CoreUiR.string.common_supported_networks),
                contentAlign = TangemTopNavigation.ContentAlign.Center,
                blurBackground = false,
                onClose = {},
            )
            JointSupportedNetworksContent(
                state = JointSupportedNetworksUM(
                    networks = persistentListOf(
                        NetworkItemUM(
                            id = "ethereum",
                            name = "Ethereum",
                            symbol = "ETH",
                            iconResId = CoreUiR.drawable.img_eth_22,
                        ),
                        NetworkItemUM(
                            id = "polygon-pos",
                            name = "Polygon PoS",
                            symbol = "POL",
                            iconResId = CoreUiR.drawable.img_polygon_22,
                        ),
                        NetworkItemUM(
                            id = "binance-smart-chain",
                            name = "BNB Smart Chain",
                            symbol = "BNB",
                            iconResId = CoreUiR.drawable.img_bsc_22,
                        ),
                    ),
                    onDismiss = {},
                    onGotItClick = {},
                ),
            )
        }
    }
}
// endregion
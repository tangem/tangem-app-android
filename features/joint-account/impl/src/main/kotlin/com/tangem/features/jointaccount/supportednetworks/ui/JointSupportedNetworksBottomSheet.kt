package com.tangem.features.jointaccount.supportednetworks.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.jointaccount.supportednetworks.ui.state.JointSupportedNetworksUM
import com.tangem.features.jointaccount.supportednetworks.ui.state.JointSupportedNetworksUM.NetworkItemUM
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun JointSupportedNetworksBottomSheet(state: JointSupportedNetworksUM) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        scrollableContent = false,
        title = {
            TangemModal.TopNavigation(
                title = resourceReference(CoreUiR.string.common_supported_networks),
                onClose = state.onDismiss,
            )
        },
        content = {
            Box(modifier = Modifier.weight(weight = 1f, fill = false)) {
                JointSupportedNetworksContent(state = state)
                Footer(
                    onGotItClick = state.onGotItClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun JointSupportedNetworksContent(state: JointSupportedNetworksUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(color = TangemTheme.colors3.bg.tertiary),
        ) {
            state.networks.forEach { item ->
                NetworkRow(item = item)
            }
        }
        SpacerH(FOOTER_HEIGHT)
    }
}

@Composable
private fun Footer(onGotItClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemFooter(
        modifier = modifier,
        windowInsets = WindowInsets(0),
        contentPadding = PaddingValues(all = FOOTER_PADDING),
        blurBackground = false,
    ) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(CoreUiR.string.common_got_it),
            onClick = onGotItClick,
        )
    }
}

private val FOOTER_PADDING = 16.dp
private val FOOTER_HEIGHT = FOOTER_PADDING * 2 + 48.dp

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
            TangemModal.TopNavigation(
                title = resourceReference(CoreUiR.string.common_supported_networks),
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
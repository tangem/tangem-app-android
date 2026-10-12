package com.tangem.features.feed.earn.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.checkbox.TangemCheckmark
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.row.*
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.earn.impl.R
import com.tangem.features.feed.earn.ui.state.EarnFilterByNetworkBottomSheetContentUM
import com.tangem.features.feed.earn.ui.state.EarnFilterFooterUM
import com.tangem.features.feed.earn.ui.state.EarnFilterNetworkUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun EarnFilterByNetworkBottomSheet(config: TangemBottomSheetConfig) {
    TangemModal<EarnFilterByNetworkBottomSheetContentUM>(
        config = config,
        scrollableContent = false,
        title = {
            TangemModal.TopNavigation(
                title = resourceReference(R.string.earn_filter_networks),
                onClose = config.onDismissRequest,
            )
        },
        content = { Content(it) },
    )
}

@Composable
private fun ColumnScope.Content(content: EarnFilterByNetworkBottomSheetContentUM) {
    Column(
        modifier = Modifier
            .weight(weight = 1f, fill = false)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TangemTabNavigation(
            tabs = content.scopeTabs,
            variant = TangemTabItem.Variant.Transparent,
        )

        Box {
            NetworksBlock(
                networks = content.networks,
                hasFooter = content.footer != null,
            )

            if (content.footer != null) {
                Footer(
                    state = content.footer,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun NetworksBlock(
    networks: ImmutableList<EarnFilterNetworkUM>,
    hasFooter: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp),
            text = stringResourceSafe(id = R.string.earn_filter_networks),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        networks.fastForEach { item -> NetworkRow(item = item) }

        if (hasFooter) {
            SpacerH(FOOTER_HEIGHT)
        }
    }
}

@Composable
private fun NetworkRow(item: EarnFilterNetworkUM, modifier: Modifier = Modifier) {
    val network = item as? EarnFilterNetworkUM.Network
    val startSlot: (@Composable BoxScope.() -> Unit)? = if (network != null) {
        {
            Image(
                modifier = Modifier
                    .size(size = 40.dp)
                    .clip(shape = CircleShape),
                painter = painterResource(id = network.iconRes),
                contentDescription = network.symbol,
            )
        }
    } else {
        null
    }

    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        titleSlot = {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TangemRowText(
                    text = network?.name ?: resourceReference(R.string.common_all),
                    role = TangemRowTextRole.Title,
                )
                if (network?.symbol != null) {
                    Text(
                        text = network.symbol,
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.secondary,
                        lineHeight = (TangemTheme.typography3.caption.medium.lineHeight.value + 1).sp,
                    )
                }
            }
        },
        startSlot = startSlot,
        endSlot = {
            TangemCheckmark(
                checked = item.isSelected,
                onCheckedChange = { item.onClick() },
            )
        },
        onClick = item.onClick,
    )
}

@Composable
private fun Footer(state: EarnFilterFooterUM, modifier: Modifier = Modifier) {
    TangemFooter(
        modifier = modifier,
        // the insets and the padding repeat what this footer looked like before it became a TangemFooter
        windowInsets = WindowInsets(0),
        contentPadding = PaddingValues(all = FOOTER_PADDING),
        // inside a modal the fade samples the screen behind the sheet rather than the list under it,
        // so the blur costs a pass per frame and shows nothing
        blurBackground = false,
    ) {
        TangemButtonGroup {
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_reset),
                onClick = state.onReset,
            )
            TangemButton(
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_apply),
                onClick = state.onApply,
            )
        }
    }
}

private val FOOTER_PADDING = 16.dp

/** Scroll reserve under the last network: the footer's two [TangemButton.Size.X12] buttons and its padding. */
private val FOOTER_HEIGHT = FOOTER_PADDING * 2 + 48.dp * 2 + 8.dp

// region Preview

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewV2() {
    TangemThemePreviewRedesign {
        EarnFilterByNetworkBottomSheet(config = previewConfig(withFooter = false))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWithFooterV2() {
    TangemThemePreviewRedesign {
        EarnFilterByNetworkBottomSheet(config = previewConfig(withFooter = true))
    }
}

private fun previewConfig(withFooter: Boolean) = TangemBottomSheetConfig(
    isShown = true,
    onDismissRequest = {},
    content = EarnFilterByNetworkBottomSheetContentUM(
        scopeTabs = persistentListOf(
            TangemTabItemUM.Content(
                id = "AllNetworks",
                label = resourceReference(R.string.earn_filter_all_networks),
                isSelected = true,
                onClick = {},
            ),
            TangemTabItemUM.Content(
                id = "MyNetworks",
                label = resourceReference(R.string.earn_filter_my_networks),
                onClick = {},
            ),
        ),
        networks = persistentListOf(
            EarnFilterNetworkUM.All(isSelected = !withFooter, onClick = {}),
            EarnFilterNetworkUM.Network(
                id = "ethereum",
                name = stringReference("Ethereum"),
                symbol = "ETH",
                iconRes = R.drawable.img_btc_22,
                isSelected = withFooter,
                onClick = {},
            ),
            EarnFilterNetworkUM.Network(
                id = "polygon",
                name = stringReference("Polygon"),
                symbol = "MATIC",
                iconRes = R.drawable.img_btc_22,
                isSelected = false,
                onClick = {},
            ),
        ),
        footer = EarnFilterFooterUM(onReset = {}, onApply = {}).takeIf { withFooter },
    ),
)

// endregion
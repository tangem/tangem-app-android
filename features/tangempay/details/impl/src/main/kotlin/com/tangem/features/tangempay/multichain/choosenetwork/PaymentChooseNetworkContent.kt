package com.tangem.features.tangempay.multichain.choosenetwork

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.test.ChooseNetworkBottomSheetTestTags
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun PaymentChooseNetworkContent(state: PaymentChooseNetworkUM) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.dismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.dismiss,
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopBar(
                title = resourceReference(R.string.common_choose_network),
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                        onClick = state.dismiss,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                PaymentNetworkSection(
                    title = resourceReference(R.string.tangempay_choose_network_fast_way),
                    items = state.fastWay,
                )
                PaymentNetworkSection(
                    title = resourceReference(R.string.tangempay_choose_network_other_ways),
                    items = state.otherWays,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
        },
    )
}

@Composable
private fun PaymentNetworkSection(
    title: TextReference,
    items: ImmutableList<PaymentNetworkItemUM>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(modifier = modifier) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        items.fastForEachIndexed { index, item ->
            PaymentNetworkRow(
                item = item,
                modifier = Modifier.roundedShapeItemDecoration(
                    currentIndex = index,
                    lastIndex = items.lastIndex,
                    addDefaultPadding = false,
                ),
            )
        }
    }
}

@Composable
private fun PaymentNetworkRow(item: PaymentNetworkItemUM, modifier: Modifier = Modifier) {
    val isLoading = item.state == PaymentNetworkItemUM.State.Loading
    val isError = item.state == PaymentNetworkItemUM.State.Error
    val subtitle = when (item.state) {
        PaymentNetworkItemUM.State.Idle -> item.tokensLabel
        PaymentNetworkItemUM.State.Loading -> resourceReference(
            R.string.tangempay_choose_network_row_loading,
        ).resolveReference()
        PaymentNetworkItemUM.State.Error -> resourceReference(
            R.string.tangempay_choose_network_row_error,
        ).resolveReference()
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            // A row mid-flight must ignore taps; onRetry (wired below) is the only way out of Error.
            .clickable(enabled = !isLoading && !isError, onClick = item.onClick)
            .testTag(ChooseNetworkBottomSheetTestTags.NETWORK_ITEM)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(id = item.iconResId),
            contentDescription = null,
            modifier = Modifier
                .testTag(ChooseNetworkBottomSheetTestTags.NETWORK_ICON)
                .size(36.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.name,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
                modifier = Modifier.testTag(ChooseNetworkBottomSheetTestTags.NETWORK_TITLE),
            )
            Text(
                text = subtitle,
                style = TangemTheme.typography3.caption.medium,
                color = if (isError) TangemTheme.colors3.text.status.error else TangemTheme.colors3.text.secondary,
                modifier = Modifier.testTag(ChooseNetworkBottomSheetTestTags.NETWORK_SUBTITLE),
            )
        }
        when (item.state) {
            PaymentNetworkItemUM.State.Idle -> Unit
            PaymentNetworkItemUM.State.Loading -> TangemLoader(size = TangemLoaderSize.X20)
            PaymentNetworkItemUM.State.Error -> {
                val onRetry = item.onRetry
                Icon(
                    imageVector = Icons.ic_arrow_refresh_20,
                    contentDescription = resourceReference(R.string.common_retry).resolveReference(),
                    tint = TangemTheme.colors3.icon.status.error,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable(enabled = onRetry != null) { onRetry?.invoke() },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PaymentChooseNetworkContentPreview() {
    TangemThemePreviewRedesign {
        PaymentChooseNetworkContent(
            state = PaymentChooseNetworkUM(
                fastWay = persistentListOf(
                    previewItem(id = "polygon", name = "Polygon", iconResId = R.drawable.img_polygon_22),
                    previewItem(
                        id = "bsc-loading",
                        name = "BNB Smart Chain",
                        iconResId = R.drawable.img_bsc_22,
                        state = PaymentNetworkItemUM.State.Loading,
                    ),
                    previewItem(id = "base", name = "Base", iconResId = R.drawable.img_base_22),
                    previewItem(
                        id = "arbitrum-error",
                        name = "Arbitrum",
                        iconResId = R.drawable.img_arbitrum_22,
                        state = PaymentNetworkItemUM.State.Error,
                        onRetry = {},
                    ),
                ),
                otherWays = persistentListOf(
                    previewItem(
                        id = "ethereum",
                        name = "Ethereum",
                        tokensLabel = "USDC, USDT",
                        iconResId = R.drawable.img_eth_22,
                    ),
                    previewItem(id = "tron", name = "TRC-20", tokensLabel = "USDT", iconResId = R.drawable.img_tron_22),
                ),
                dismiss = {},
            ),
        )
    }
}

private fun previewItem(
    id: String,
    name: String,
    iconResId: Int,
    tokensLabel: String = "USDC, USDT",
    state: PaymentNetworkItemUM.State = PaymentNetworkItemUM.State.Idle,
    onRetry: (() -> Unit)? = null,
): PaymentNetworkItemUM = PaymentNetworkItemUM(
    id = id,
    name = name,
    tokensLabel = tokensLabel,
    iconResId = iconResId,
    state = state,
    onClick = {},
    onRetry = onRetry,
)
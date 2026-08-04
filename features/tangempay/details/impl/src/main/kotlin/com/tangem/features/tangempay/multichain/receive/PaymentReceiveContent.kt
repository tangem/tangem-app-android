package com.tangem.features.tangempay.multichain.receive

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.rememberQrPainter
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Pay-specific multi-token "Receive assets" bottom sheet (Figma node 5115:60858). Unlike the shared,
 * single-currency token-receive sheet, this one shows every token that shares the network's deposit
 * address (e.g. USDC + USDT on the same network) side by side.
 */
@Composable
internal fun PaymentReceiveContent(state: PaymentReceiveUM) {
    var isQrShown by rememberSaveable(state.address) { mutableStateOf(false) }

    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        containerColor = TangemTheme.colors3.bg.secondary,
        onBack = if (isQrShown) {
            { isQrShown = false }
        } else {
            state.onDismiss
        },
        title = {
            TangemTopBar(
                title = resourceReference(R.string.domain_receive_assets_navigation_title),
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                        onClick = state.onDismiss,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        },
        content = {
            if (isQrShown) {
                PaymentReceiveQrContent(state = state)
            } else {
                PaymentReceiveMainContent(
                    state = state,
                    onShowQr = {
                        isQrShown = true
                        state.onShowQr()
                    },
                )
            }
        },
    )
}

@Composable
private fun PaymentReceiveMainContent(state: PaymentReceiveUM, onShowQr: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WarningBanner(warning = state.warning.resolveReference(), modifier = Modifier.fillMaxWidth())
        SpacerH(28.dp)
        TokenIconsRow(tokens = state.tokens)
        SpacerH(16.dp)
        Text(
            text = state.tokensOnNetworkLabel.resolveReference(),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        SpacerH(8.dp)
        Text(
            text = state.address,
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
        SpacerH(24.dp)
        // The address may be briefly empty (before the first status emission, or while the just-created
        // contract is not yet visible in the status). Acting on an empty address is meaningless, and
        // rendering it as a QR crashes ZXing — keep the actions disabled until it arrives.
        val hasAddress = state.address.isNotEmpty()
        TangemButton(
            text = resourceReference(R.string.common_copy),
            iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_copy_24),
            onClick = state.onCopy,
            variant = TangemButton.Variant.Outline,
            size = TangemButton.Size.X8,
            isEnabled = hasAddress,
        )
        SpacerH(12.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemButton(
                modifier = Modifier.weight(1f),
                text = resourceReference(R.string.token_receive_show_qr_code_title),
                iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_qr_24),
                onClick = onShowQr,
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
                isEnabled = hasAddress,
            )
            TangemButton(
                modifier = Modifier.weight(1f),
                text = resourceReference(R.string.common_share),
                iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_share_android_24),
                onClick = state.onShare,
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                isEnabled = hasAddress,
            )
        }
    }
}

@Composable
private fun PaymentReceiveQrContent(state: PaymentReceiveUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = rememberQrPainter(content = state.address),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .border(
                    width = 16.dp,
                    color = TangemTheme.colors3.bg.secondary,
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(16.dp)
                .size(200.dp),
        )
        SpacerH(24.dp)
        Text(
            text = state.address,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        SpacerH(24.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemButton(
                modifier = Modifier.weight(1f),
                text = resourceReference(R.string.common_copy),
                iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_copy_24),
                onClick = state.onCopy,
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
            )
            TangemButton(
                modifier = Modifier.weight(1f),
                text = resourceReference(R.string.common_share),
                iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_share_android_24),
                onClick = state.onShare,
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
            )
        }
    }
}

@Composable
private fun WarningBanner(warning: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.status.infoSubtle)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.ic_info_24,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.status.info,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = warning,
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = stringResourceSafe(R.string.receive_bottom_sheet_warning_message_description),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun TokenIconsRow(tokens: ImmutableList<TokenIconUM>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tokens.fastForEach { token ->
            CurrencyIcon(
                state = token.iconState,
                shouldDisplayNetwork = true,
                networkBadgeSize = 24.dp,
                iconSize = 80.dp,
            )
        }
    }
}

@Preview(device = Devices.PIXEL_7_PRO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL_7_PRO)
@Composable
private fun PaymentReceiveContentPreview() {
    TangemThemePreviewRedesign {
        PaymentReceiveContent(
            state = PaymentReceiveUM(
                warning = resourceReference(
                    id = R.string.receive_bottom_sheet_warning_title,
                    formatArgs = wrappedList("USDC, USDT", "Solana"),
                ),
                tokensOnNetworkLabel = resourceReference(
                    id = R.string.receive_bottom_sheet_warning_message_compact,
                    formatArgs = wrappedList("USDC, USDT", "Solana"),
                ),
                tokens = persistentListOf(
                    TokenIconUM(symbol = "USDC", iconState = CurrencyIconState.Empty()),
                    TokenIconUM(symbol = "USDT", iconState = CurrencyIconState.Empty()),
                ),
                address = "0xe5178c7d4d0e861ed2e9414e045b501226b0de8d",
                onCopy = {},
                onShowQr = {},
                onShare = {},
                onDismiss = {},
            ),
        )
    }
}
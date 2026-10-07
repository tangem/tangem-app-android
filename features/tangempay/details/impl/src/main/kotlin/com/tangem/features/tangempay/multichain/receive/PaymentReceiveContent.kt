package com.tangem.features.tangempay.multichain.receive

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.rememberQrPainter
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Back
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.topnavigation.TangemNavigationText
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.copy
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val CurrencyIconInset = 2.dp

/**
 * Pay-specific multi-token "Receive assets" bottom sheet (Figma node 16762:12306). Unlike the shared,
 * single-currency token-receive sheet, this one shows every token that shares the network's deposit
 * address (e.g. USDC + USDT on the same network) side by side.
 */
@Composable
internal fun PaymentReceiveContent(state: PaymentReceiveUM) {
    var isQrShown by rememberSaveable(state.address) { mutableStateOf(false) }
    val hideQr = { isQrShown = false }

    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        onBack = if (isQrShown) hideQr else state.onDismiss,
        title = {
            PaymentReceiveTopNavigation(isQrShown = isQrShown, onBack = hideQr, onClose = state.onDismiss)
        },
        content = {
            Crossfade(
                targetState = isQrShown,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                animationSpec = tween(durationMillis = 100),
                label = "PaymentReceiveContent",
            ) { isQrState ->
                if (isQrState) {
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
            }
        },
    )
}

@Composable
private fun PaymentReceiveTopNavigation(isQrShown: Boolean, onBack: () -> Unit, onClose: () -> Unit) {
    TangemTopNavigation(
        contentAlign = TangemTopNavigation.ContentAlign.Center,
        windowInsets = WindowInsets(0),
        contentPadding = TangemTopNavigation.DefaultContentPadding.copy(top = 16.dp),
        fadeEnabled = false,
        startButton = if (isQrShown) {
            { TangemButton.Back(onClick = onBack) }
        } else {
            null
        },
        endButton = { TangemButton.Close(onClick = onClose) },
        contentColumn = {
            AnimatedVisibility(visible = !isQrShown, enter = fadeIn(), exit = fadeOut()) {
                TangemNavigationText(
                    text = resourceReference(R.string.domain_receive_assets_navigation_title),
                    role = TangemNavigationText.Role.Title,
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
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AddressBlock(state = state, onShowQr = onShowQr, modifier = Modifier.fillMaxWidth())
        WarningBanner(warning = state.warning, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun AddressBlock(state: PaymentReceiveUM, onShowQr: () -> Unit, modifier: Modifier = Modifier) {
    val hasAddress = state.address.isNotEmpty()
    Column(
        modifier = modifier.padding(start = 16.dp, top = 16.dp - CurrencyIconInset, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TokenIconsRow(tokens = state.tokens)
        SpacerH(12.dp - CurrencyIconInset)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = state.tokensOnNetworkLabel.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                modifier = Modifier.testTag(TangemPayTestTags.RECEIVE_ADDRESS),
                text = state.address,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
        SpacerH(8.dp)
        TangemButton(
            modifier = Modifier.testTag(TangemPayTestTags.RECEIVE_SHOW_QR_BUTTON),
            text = resourceReference(R.string.token_receive_show_qr_code_title),
            iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_qrcode_new_24),
            onClick = onShowQr,
            variant = TangemButton.Variant.Ghost,
            size = TangemButton.Size.X10,
            isEnabled = hasAddress,
        )
        SpacerH(20.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TangemButton(
                modifier = Modifier.testTag(TangemPayTestTags.RECEIVE_COPY_BUTTON),
                text = resourceReference(R.string.common_copy),
                iconStart = TangemIconUM.Icon(imageVector = Icons.ic_copy_24),
                onClick = state.onCopy,
                variant = TangemButton.Variant.Secondary,
                size = TangemButton.Size.X12,
                isEnabled = hasAddress,
            )
            TangemButton(
                modifier = Modifier.testTag(TangemPayTestTags.RECEIVE_SHARE_BUTTON),
                text = resourceReference(R.string.common_share),
                iconStart = TangemIconUM.Icon(imageVector = Icons.ic_share_android_24),
                onClick = state.onShare,
                variant = TangemButton.Variant.Secondary,
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
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.tokensOnNetworkLabel.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
            )
            SpacerH(32.dp)
            Image(
                painter = rememberQrPainter(content = state.address),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .testTag(TangemPayTestTags.RECEIVE_QR_CODE)
                    .border(
                        width = 16.dp,
                        color = TangemTheme.colors3.bg.secondary,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(16.dp)
                    .sizeIn(minWidth = 148.dp),
            )
        }
        Text(
            text = stringResourceSafe(R.string.wc_common_address),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
        SpacerH(4.dp)
        Text(
            modifier = Modifier.testTag(TangemPayTestTags.RECEIVE_ADDRESS),
            text = state.address,
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        SpacerH(32.dp)
        QrButtons(state = state, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun QrButtons(state: PaymentReceiveUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TangemButton(
            modifier = Modifier
                .weight(1f)
                .testTag(TangemPayTestTags.RECEIVE_COPY_BUTTON),
            text = resourceReference(R.string.common_copy),
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_copy_24),
            onClick = state.onCopy,
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
        )
        TangemButton(
            modifier = Modifier
                .weight(1f)
                .testTag(TangemPayTestTags.RECEIVE_SHARE_BUTTON),
            text = resourceReference(R.string.common_share),
            iconStart = TangemIconUM.Icon(imageVector = Icons.ic_share_android_24),
            onClick = state.onShare,
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
        )
    }
}

@Composable
private fun WarningBanner(warning: TextReference, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        TangemMessageBanner(
            modifier = Modifier.fillMaxWidth(),
            title = warning,
            description = resourceReference(R.string.receive_bottom_sheet_warning_message_description),
            showGlowRing = false,
            slotEnd = {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.ic_info_20,
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.primary,
                )
            },
        )
        TangemGlowRing(
            modifier = Modifier.matchParentSize(),
            variant = TangemGlowRing.Variant.Info,
            cornerRadius = 28.dp,
        )
    }
}

@Composable
private fun TokenIconsRow(tokens: ImmutableList<TokenIconUM>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp - CurrencyIconInset * 2),
    ) {
        tokens.fastForEach { token ->
            CurrencyIcon(
                state = token.iconState,
                shouldDisplayNetwork = true,
                networkBadgeSize = 20.dp,
                iconSize = 56.dp,
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
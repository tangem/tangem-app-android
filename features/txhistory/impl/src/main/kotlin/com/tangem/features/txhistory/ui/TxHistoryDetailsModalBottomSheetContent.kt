package com.tangem.features.txhistory.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import kotlinx.collections.immutable.persistentListOf

/**
 * The transaction details bottom sheet ("Operation"): the [TangemModalBottomSheet] shell shared by all transaction
 * types, with the [TxHistoryDetailsTopNavigation] header in the title slot and [TxHistoryDetailsContent] (single-asset
 * or two-asset body) as the content.
 *
 * Extracted from `DefaultTxHistoryDetailsComponent` so the whole sheet — header + body — is previewable in isolation.
 *
 * @param state Sheet state. `null` keeps the sheet hidden (the modal renders its empty placeholder).
 * @param onDismiss Invoked on close / dismiss request.
 */
@Composable
internal fun TxHistoryDetailsModalBottomSheetContent(state: TxHistoryDetailsUM?, onDismiss: () -> Unit) {
    TangemModalBottomSheet<TxHistoryDetailsUM>(
        containerColor = TangemTheme.colors3.bg.secondary,
        config = TangemBottomSheetConfig(
            isShown = state != null,
            onDismissRequest = onDismiss,
            content = state ?: TangemBottomSheetConfigContent.Empty,
        ),
        title = {
            state?.let { um -> TxHistoryDetailsTopNavigation(header = um.header, onCloseClick = onDismiss) }
        },
        content = { um -> TxHistoryDetailsContent(state = um) },
    )
}

// region Preview

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TxHistoryDetailsModalBottomSheetContentPreview() {
    TangemThemePreviewRedesign {
        TxHistoryDetailsModalBottomSheetContent(state = previewSingleAsset(), onDismiss = {})
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TxHistoryDetailsModalBottomSheetContentTwoAssetsPreview() {
    TangemThemePreviewRedesign {
        TxHistoryDetailsModalBottomSheetContent(state = previewTwoAssets(), onDismiss = {})
    }
}

/** Fully-populated single-asset state exercising every sub-view: header, amount block, counterparty and info rows. */
private fun previewSingleAsset() = TxHistoryDetailsUM.SingleAsset(
    header = TxHistoryDetailsUM.HeaderUM(
        iconRes = R.drawable.ic_arrow_up_24,
        status = Status.Confirmed,
        title = stringReference("Sent"),
        subtitle = stringReference("Jan 20 2026, 9:24 PM"),
    ),
    amountBlock = TxHistoryDetailsUM.AmountBlockUM(
        currencyIcon = CurrencyIconState.CoinIcon(
            url = null,
            fallbackResId = R.drawable.img_eth_22,
            isGrayscale = false,
            shouldShowCustomBadge = false,
        ),
        amount = stringReference("- 350.31 USDT"),
        fiatAmount = stringReference("$350.31"),
        isFailed = false,
    ),
    counterparty = TxHistoryDetailsUM.CounterpartyUM(
        label = stringReference("Recipient"),
        title = stringReference("33Bd321fS...ga21412B"),
        avatar = TxHistoryDetailsUM.CounterpartyAvatar.Address(
            rawAddress = "0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359",
        ),
        onCopyClick = {},
    ),
    rows = persistentListOf(
        TxHistoryDetailsUM.InfoRowUM(label = stringReference("Network fee"), value = stringReference("0.00056 ETH")),
    ),
)

/** Failed swap exercising the two-asset body: both legs, the error status banner, provider link row and the CTA. */
private fun previewTwoAssets() = TxHistoryDetailsUM.TwoAssets(
    header = TxHistoryDetailsUM.HeaderUM(
        iconRes = R.drawable.ic_exchange_vertical_24,
        status = Status.Failed,
        title = stringReference("Swap"),
        subtitle = stringReference("Jan 20 2026, 9:24 PM"),
    ),
    from = TxHistoryDetailsUM.AssetUM(
        label = stringReference("You send"),
        owner = null,
        amount = stringReference("- 1.5 ETH"),
        currencyIcon = CurrencyIconState.CoinIcon(
            url = null,
            fallbackResId = R.drawable.img_eth_22,
            isGrayscale = false,
            shouldShowCustomBadge = false,
        ),
        isFaded = true,
    ),
    to = TxHistoryDetailsUM.AssetUM(
        label = stringReference("You receive"),
        owner = null,
        amount = stringReference("+ 0.001 BTC"),
        currencyIcon = CurrencyIconState.CoinIcon(
            url = null,
            fallbackResId = R.drawable.img_btc_22,
            isGrayscale = false,
            shouldShowCustomBadge = false,
        ),
        isFaded = true,
    ),
    statusBanner = TxHistoryDetailsUM.StatusBannerUM(
        severity = TxHistoryDetailsUM.StatusBannerUM.Severity.Error,
        title = stringReference("Failed"),
        subtitle = stringReference("Funds will be refunded by the provider"),
        isLoading = false,
    ),
    rows = persistentListOf(
        TxHistoryDetailsUM.InfoRowUM(
            label = stringReference("Provider"),
            value = stringReference("Changelly"),
            trailingIconRes = R.drawable.ic_arrow_top_right_24,
            onClick = {},
        ),
        TxHistoryDetailsUM.InfoRowUM(label = stringReference("Network fee"), value = stringReference("0.00056 ETH")),
    ),
    providerButton = TxHistoryDetailsUM.ProviderButtonUM(
        text = stringReference("Go to provider"),
        onClick = {},
    ),
)

// endregion
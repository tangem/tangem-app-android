package com.tangem.features.txhistory.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.extensions.plus
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledStringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.core.ui.res.generated.icons.ic_globe_24
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
 * @param ratingContent Optional provider-rating (CSAT) card, rendered between the exchange block and the info rows
 *  of the two-asset body. `null` hides the card (non-swap txs).
 */
@Composable
internal fun TxHistoryDetailsModalBottomSheetContent(
    state: TxHistoryDetailsUM?,
    onDismiss: () -> Unit,
    ratingContent: (@Composable () -> Unit)? = null,
) {
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
        content = { um -> TxHistoryDetailsContent(state = um, ratingContent = ratingContent) },
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

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TxHistoryDetailsModalBottomSheetContentRefundedPreview() {
    TangemThemePreviewRedesign {
        TxHistoryDetailsModalBottomSheetContent(state = previewRefunded(), onDismiss = {})
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TxHistoryDetailsModalBottomSheetContentUnratedPreview() {
    TangemThemePreviewRedesign {
        TxHistoryDetailsModalBottomSheetContent(
            state = previewTwoAssets(),
            onDismiss = {},
            ratingContent = { PreviewRatingCard(selectedRating = null) },
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_7_PRO)
@Preview(showBackground = true, device = Devices.PIXEL_7_PRO, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun TxHistoryDetailsModalBottomSheetContentAlreadyRatedPreview() {
    TangemThemePreviewRedesign {
        TxHistoryDetailsModalBottomSheetContent(
            state = previewTwoAssets(),
            onDismiss = {},
            ratingContent = { PreviewRatingCard(selectedRating = 4) },
        )
    }
}

/**
 * Preview-only stand-in for the provider-rating card: the real one is internal to the rating feature and arrives
 * here as an opaque composable slot.
 */
@Composable
@Suppress("MagicNumber")
private fun PreviewRatingCard(selectedRating: Int?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.tertiary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Rate your experience with provider",
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(5) { index ->
                val isFilled = selectedRating != null && index < selectedRating
                Icon(
                    painter = painterResource(R.drawable.ic_rating_star_24),
                    contentDescription = null,
                    tint = if (isFilled) {
                        TangemTheme.colors3.icon.accent.orange
                    } else {
                        TangemTheme.colors3.icon.tertiary
                    },
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

/** Fully-populated single-asset state exercising every sub-view: header, amount block, counterparty and info rows. */
private fun previewSingleAsset() = TxHistoryDetailsUM.SingleAsset(
    header = TxHistoryDetailsUM.HeaderUM(
        icon = TxIcon.Vector(Icons.ic_arrow_up_20),
        status = Status.Confirmed,
        title = stringReference("Sent"),
        subtitle = stringReference("Jan 20 2026, 9:24 PM"),
        menu = previewMenu(),
    ),
    amountBlock = TxHistoryDetailsUM.AmountBlockUM(
        icon = TxHistoryDetailsUM.AmountIconUM.Single(
            CurrencyIconState.CoinIcon(
                url = null,
                fallbackResId = R.drawable.img_eth_22,
                isGrayscale = false,
                shouldShowCustomBadge = false,
            ),
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
        icon = TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20),
        status = Status.Failed,
        title = stringReference("Swap"),
        subtitle = stringReference("Jan 20 2026, 9:24 PM"),
        menu = previewMenu(),
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
        isFaded = false,
    ),
    to = TxHistoryDetailsUM.AssetUM(
        label = stringReference("You receive"),
        owner = null,
        amount = stringReference("0.001 BTC"),
        currencyIcon = CurrencyIconState.CoinIcon(
            url = null,
            fallbackResId = R.drawable.img_btc_22,
            isGrayscale = false,
            shouldShowCustomBadge = false,
        ),
        isFaded = true,
    ),
    statusBanner = TxHistoryDetailsUM.StatusBannerUM(
        style = TxHistoryDetailsUM.StatusBannerUM.Style.Error,
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

/** Refunded swap exercising the "Refunded in {symbol}" banner with the "Learn more" link and the "Go to token" CTA. */
private fun previewRefunded() = previewTwoAssets().copy(
    header = TxHistoryDetailsUM.HeaderUM(
        icon = TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20),
        status = Status.Failed,
        title = stringReference("Swapping failed"),
        subtitle = stringReference("Jan 20 2026, 9:24 PM"),
        menu = previewMenu(),
    ),
    statusBanner = TxHistoryDetailsUM.StatusBannerUM(
        style = TxHistoryDetailsUM.StatusBannerUM.Style.Refunded,
        title = stringReference("Refunded in WBTC"),
        subtitle = stringReference(
            "Your funds have been refunded in WBTC to your wallet on the Polygon network, " +
                "in accordance with OKX exchange rules. ",
        ) + styledStringReference(
            value = "Learn more",
            spanStyleReference = { SpanStyle(textDecoration = TextDecoration.Underline) },
            onClick = {},
        ),
        isLoading = false,
    ),
    providerButton = TxHistoryDetailsUM.ProviderButtonUM(
        text = stringReference("Go to token"),
        onClick = {},
    ),
)

/** Sample header `•••` menu used by the previews. */
private fun previewMenu() = persistentListOf(
    TxHistoryDetailsUM.MenuItemUM(
        icon = Icons.ic_copy_24,
        title = stringReference("Transaction ID"),
        onClick = {},
    ),
    TxHistoryDetailsUM.MenuItemUM(
        icon = Icons.ic_globe_24,
        title = stringReference("Explore"),
        onClick = {},
    ),
)

// endregion
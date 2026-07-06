package com.tangem.features.txhistory.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.currency.icon.TangemCurrencyIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM

/**
 * Centered amount block of the single-asset card: token avatar (with network badge) over the big signed amount and the
 * secondary fiat line.
 *
 * The failed state ([TxHistoryDetailsUM.AmountBlockUM.isFailed]) strikes the amount through and dims it (primary ->
 * secondary) — matching the status-driven recolor of the shared header. The `+`/`−` sign is already dropped upstream
 * by the converter for failed transactions (a failed tx moved nothing), so the [amount] text arrives unsigned here.
 */
@Composable
internal fun TxHistoryDetailsAmountBlock(amountBlock: TxHistoryDetailsUM.AmountBlockUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemCurrencyIcon(
            state = amountBlock.currencyIcon,
            modifier = Modifier.size(72.dp),
        )
        SpacerH(24.dp)
        Text(
            text = amountBlock.amount.resolveReference(),
            color = if (amountBlock.isFailed) {
                TangemTheme.colors3.text.secondary
            } else {
                TangemTheme.colors3.text.primary
            },
            style = TangemTheme.typography3.heading.medium,
            textAlign = TextAlign.Center,
            textDecoration = if (amountBlock.isFailed) TextDecoration.LineThrough else null,
        )
        amountBlock.fiatAmount?.let { fiatAmount ->
            SpacerH(4.dp)
            Text(
                text = fiatAmount.resolveReference(),
                color = if (amountBlock.isFailed) {
                    TangemTheme.colors3.text.tertiary
                } else {
                    TangemTheme.colors3.text.secondary
                },
                style = TangemTheme.typography3.body.medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// region Preview

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(name = "Dark", uiMode = UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
private fun TxHistoryDetailsAmountBlockPreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        ) {
            TxHistoryDetailsAmountBlock(amountBlock = previewAmountBlock(isFailed = false))
            TxHistoryDetailsAmountBlock(amountBlock = previewAmountBlock(isFailed = true))
            // No fiat — the fiat line is omitted entirely.
            TxHistoryDetailsAmountBlock(amountBlock = previewAmountBlock(isFailed = false, fiatAmount = null))
        }
    }
}

private fun previewAmountBlock(isFailed: Boolean, fiatAmount: TextReference? = stringReference("$350.31")) =
    TxHistoryDetailsUM.AmountBlockUM(
        currencyIcon = CurrencyIconState.CoinIcon(
            url = null,
            fallbackResId = R.drawable.img_eth_22,
            isGrayscale = false,
            shouldShowCustomBadge = false,
        ),
        amount = stringReference("+ 350.31 USDT"),
        fiatAmount = fiatAmount,
        isFailed = isFailed,
    )

// endregion
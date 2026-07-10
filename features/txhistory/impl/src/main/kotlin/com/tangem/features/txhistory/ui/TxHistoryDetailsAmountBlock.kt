package com.tangem.features.txhistory.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
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

private val IconSize = 72.dp
private val PairOverlap = 20.dp
private val PairRing = 4.dp
private val LeadingSize = IconSize + PairRing * 2

/**
 * Centered amount block of the single-asset card: the token avatar (single, or a yield-supply asset+Aave pair), an
 * optional label ("Supplied"/"Returned"), the big amount and the secondary fiat line.
 *
 * The failed state ([TxHistoryDetailsUM.AmountBlockUM.isFailed]) strikes the amount through and dims it (primary ->
 * secondary) — matching the status-driven recolor of the shared header. The `+`/`−` sign is resolved upstream by the
 * converter (dropped for failed and yield-supply transactions), so the [amount] text arrives ready to render here.
 */
@Composable
internal fun TxHistoryDetailsAmountBlock(amountBlock: TxHistoryDetailsUM.AmountBlockUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AmountIcon(amountBlock.icon)
        SpacerH(24.dp)
        amountBlock.label?.let { label ->
            Text(
                text = label.resolveReference(),
                color = TangemTheme.colors3.text.secondary,
                style = TangemTheme.typography3.subheading.medium,
                textAlign = TextAlign.Center,
            )
            SpacerH(4.dp)
        }
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

@Composable
private fun AmountIcon(icon: TxHistoryDetailsUM.AmountIconUM, modifier: Modifier = Modifier) {
    when (icon) {
        is TxHistoryDetailsUM.AmountIconUM.Single -> AmountIconItem(
            item = TxHistoryDetailsUM.AmountIconUM.Item.Currency(icon.currencyIcon),
            modifier = modifier,
        )
        is TxHistoryDetailsUM.AmountIconUM.OverlappingPair -> Box(
            modifier = modifier
                .height(LeadingSize)
                .width(PairRing + IconSize * 2 - PairOverlap),
        ) {
            // Trailing behind, right; the pair drops the network badge so the two token arts read as equal-size icons.
            AmountIconItem(
                item = icon.trailing,
                modifier = Modifier.align(Alignment.CenterEnd),
                shouldDisplayNetwork = false,
            )
            // Leading on top, left, wrapped in a background-colored ring that separates it from the trailing icon.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(LeadingSize)
                    .background(TangemTheme.colors3.bg.secondary, CircleShape)
                    .padding(PairRing),
            ) {
                AmountIconItem(item = icon.leading, shouldDisplayNetwork = false)
            }
        }
    }
}

@Composable
private fun AmountIconItem(
    item: TxHistoryDetailsUM.AmountIconUM.Item,
    modifier: Modifier = Modifier,
    shouldDisplayNetwork: Boolean = true,
) {
    when (item) {
        is TxHistoryDetailsUM.AmountIconUM.Item.Currency -> TangemCurrencyIcon(
            state = item.state,
            modifier = modifier.size(IconSize),
            shouldDisplayNetwork = shouldDisplayNetwork,
        )
        is TxHistoryDetailsUM.AmountIconUM.Item.Resource -> Image(
            painter = painterResource(item.resId),
            contentDescription = null,
            modifier = modifier
                .size(IconSize)
                .clip(CircleShape),
        )
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
            // Yield supply — "Supplied" (Aave leads) and "Returned" (asset leads), unsigned amount.
            TxHistoryDetailsAmountBlock(
                amountBlock = previewAmountBlock(
                    isFailed = false,
                    fiatAmount = null,
                    icon = TxHistoryDetailsUM.AmountIconUM.OverlappingPair(
                        leading = TxHistoryDetailsUM.AmountIconUM.Item.Resource(R.drawable.img_aave_22),
                        trailing = TxHistoryDetailsUM.AmountIconUM.Item.Currency(previewCurrencyIcon()),
                    ),
                    label = stringReference("Supplied"),
                    amount = stringReference("1,294.23 USDT"),
                ),
            )
        }
    }
}

private fun previewCurrencyIcon() = CurrencyIconState.CoinIcon(
    url = null,
    fallbackResId = R.drawable.img_eth_22,
    isGrayscale = false,
    shouldShowCustomBadge = false,
)

private fun previewAmountBlock(
    isFailed: Boolean,
    fiatAmount: TextReference? = stringReference("$350.31"),
    icon: TxHistoryDetailsUM.AmountIconUM = TxHistoryDetailsUM.AmountIconUM.Single(previewCurrencyIcon()),
    label: TextReference? = null,
    amount: TextReference = stringReference("+ 350.31 USDT"),
) = TxHistoryDetailsUM.AmountBlockUM(
    icon = icon,
    amount = amount,
    label = label,
    fiatAmount = fiatAmount,
    isFailed = isFailed,
)

// endregion
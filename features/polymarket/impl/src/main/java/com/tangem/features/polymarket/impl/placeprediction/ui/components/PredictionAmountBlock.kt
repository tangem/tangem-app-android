package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.AmountTextFieldColors
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.common.USD_CODE
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PaymentSourceUM
import com.tangem.utils.StringsSigns.DASH_SIGN
import java.math.BigDecimal

@Suppress("LongParameterList")
@Composable
internal fun PredictionAmountBlock(
    amountValue: String,
    toWin: BigDecimal?,
    amountError: AmountErrorUM?,
    payment: PaymentSourceUM,
    onAmountChange: (String) -> Unit,
    onAddFundsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemSurface(modifier = modifier, color = TangemTheme.colors3.bg.secondary) {
        Column {
            AmountSection(
                amountValue = amountValue,
                toWin = toWin,
                amountError = amountError,
                onAmountChange = onAmountChange,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = TangemTheme.colors3.border.secondary,
            )
            TangemRow(
                verticalAlignment = TangemRowVerticalAlignment.Center,
                startSlot = payment.iconState?.let { iconState ->
                    { CurrencyIcon(state = iconState, shouldDisplayNetwork = true) }
                },
                titleSlot = {
                    Text(
                        text = payment.tokenSymbol,
                        style = TangemTheme.typography3.body.medium,
                        color = TangemTheme.colors3.text.primary,
                    )
                },
                subtitleSlot = {
                    Text(
                        text = stringResourceSafe(
                            R.string.common_balance,
                            payment.balance?.formatPolymarketMoney() ?: DASH_SIGN,
                        ),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.secondary,
                    )
                },
                endSlot = {
                    TangemButton(
                        variant = TangemButton.Variant.Secondary,
                        size = TangemButton.Size.X7,
                        text = resourceReference(R.string.common_add_funds),
                        onClick = onAddFundsClick,
                    )
                },
            )
        }
    }
}

@Composable
private fun AmountSection(
    amountValue: String,
    toWin: BigDecimal?,
    amountError: AmountErrorUM?,
    onAmountChange: (String) -> Unit,
) {
    val amountColor by animateColorAsState(
        targetValue = if (amountValue.isBlank()) {
            TangemTheme.colors3.text.tertiary
        } else {
            TangemTheme.colors3.text.primary
        },
        label = "amountColor",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.prediction_place_you_will_pay),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        AmountTextField(
            modifier = Modifier.padding(top = CAPTION_GAP),
            value = amountValue,
            decimals = COLLATERAL_DECIMALS,
            onValueChange = onAmountChange,
            textStyle = TangemTheme.typography3.display.medium.copy(textAlign = TextAlign.Center),
            colors = AmountTextFieldColors(
                textColor = amountColor,
                disabledTextColor = amountColor,
                backgroundColor = Color.Transparent,
            ),
            isAutoResize = true,
            visualTransformation = AmountVisualTransformation(
                decimals = COLLATERAL_DECIMALS,
                symbol = AMOUNT_SYMBOL,
                currencyCode = USD_CODE,
                symbolColor = TangemTheme.colors3.text.primary,
                emptyValueText = EMPTY_AMOUNT_HINT,
            ),
        )
        AmountCaptionSlot(amountError = amountError, toWin = toWin)
    }
}

@Composable
private fun AmountCaptionSlot(amountError: AmountErrorUM?, toWin: BigDecimal?) {
    AnimatedContent(
        targetState = amountCaption(amountError = amountError, toWin = toWin),
        // The column animates its own height, so the swap only crossfades — otherwise both would resize at once.
        transitionSpec = { fadeIn() togetherWith fadeOut() using null },
        // Left-aligned, the width difference between the two captions would read as a slide.
        contentAlignment = Alignment.Center,
        label = "amountCaption",
    ) { caption ->
        when (caption) {
            is AmountCaption.Shortfall -> Text(
                modifier = Modifier.padding(top = CAPTION_GAP),
                text = stringResourceSafe(caption.error.textRes()),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.status.error,
                textAlign = TextAlign.Center,
            )
            is AmountCaption.ToWin -> TangemBadge(
                modifier = Modifier.padding(top = CAPTION_GAP),
                text = stringReference(
                    stringResourceSafe(R.string.prediction_place_to_win, caption.amount.formatPolymarketMoney()),
                ),
                status = TangemBadge.Status.Info,
                size = TangemBadge.Size.X6,
            )
            null -> Unit
        }
    }
}

/** What sits under the amount: at most one of them, so the swap is a single animated slot. */
private sealed interface AmountCaption {

    data class Shortfall(val error: AmountErrorUM) : AmountCaption

    data class ToWin(val amount: BigDecimal) : AmountCaption
}

private fun amountCaption(amountError: AmountErrorUM?, toWin: BigDecimal?): AmountCaption? = when {
    amountError != null -> AmountCaption.Shortfall(error = amountError)
    toWin != null -> AmountCaption.ToWin(amount = toWin)
    else -> null
}

private const val COLLATERAL_DECIMALS = 2

private const val EMPTY_AMOUNT_HINT = "0"

private val CAPTION_GAP = 12.dp

// The transformation glues the prefix onto the amount, so the gap lives in the symbol — non-breaking, to
// survive the field resizing.
private const val AMOUNT_SYMBOL = "$\u00A0"

private fun AmountErrorUM.textRes(): Int = when (this) {
    AmountErrorUM.NotEnoughForBet -> R.string.prediction_place_insufficient_balance_bet
    AmountErrorUM.NotEnoughForFees -> R.string.prediction_place_insufficient_balance_fees
}
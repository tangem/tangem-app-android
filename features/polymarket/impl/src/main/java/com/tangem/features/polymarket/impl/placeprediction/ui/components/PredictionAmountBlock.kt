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
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountCaptionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.CollateralUM
import java.math.BigDecimal

@Suppress("LongParameterList")
@Composable
internal fun PredictionAmountBlock(
    amountValue: String,
    amountCaption: AmountCaptionUM?,
    collateral: CollateralUM,
    balance: BigDecimal?,
    onAmountChange: (String) -> Unit,
    onAddFundsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TangemSurface(modifier = modifier, color = TangemTheme.colors3.bg.secondary, shape = PolymarketCardShape) {
        Column {
            AmountSection(
                amountValue = amountValue,
                amountCaption = amountCaption,
                onAmountChange = onAmountChange,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = TangemTheme.colors3.border.secondary,
            )
            TangemRow(
                verticalAlignment = TangemRowVerticalAlignment.Center,
                startSlot = collateral.iconState?.let { iconState ->
                    { CurrencyIcon(state = iconState, shouldDisplayNetwork = true) }
                },
                titleSlot = {
                    Text(
                        text = collateral.tokenSymbol,
                        style = TangemTheme.typography3.body.medium,
                        color = TangemTheme.colors3.text.primary,
                    )
                },
                subtitleSlot = {
                    Text(
                        text = stringResourceSafe(
                            R.string.common_balance,
                            balance.formatPolymarketMoney(),
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
private fun AmountSection(amountValue: String, amountCaption: AmountCaptionUM?, onAmountChange: (String) -> Unit) {
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
            decimals = PolymarketConst.COLLATERAL_DECIMALS,
            onValueChange = onAmountChange,
            textStyle = TangemTheme.typography3.display.medium.copy(textAlign = TextAlign.Center),
            colors = AmountTextFieldColors(
                textColor = amountColor,
                disabledTextColor = amountColor,
                backgroundColor = Color.Transparent,
            ),
            isAutoResize = true,
            visualTransformation = AmountVisualTransformation(
                decimals = PolymarketConst.COLLATERAL_DECIMALS,
                symbol = PolymarketConst.USD_SYMBOL,
                currencyCode = PolymarketConst.USD_CODE,
                symbolColor = amountColor,
            ),
        )
        AmountCaptionSlot(amountCaption = amountCaption)
    }
}

@Composable
private fun AmountCaptionSlot(amountCaption: AmountCaptionUM?) {
    AnimatedContent(
        targetState = amountCaption,
        transitionSpec = { fadeIn() togetherWith fadeOut() using null },
        contentAlignment = Alignment.Center,
        label = "amountCaption",
    ) { caption ->
        when (caption) {
            is AmountCaptionUM.Shortfall -> Text(
                modifier = Modifier.padding(top = CAPTION_GAP),
                text = caption.error.text.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.status.error,
                textAlign = TextAlign.Center,
            )
            is AmountCaptionUM.BelowMinimum -> Text(
                modifier = Modifier.padding(top = CAPTION_GAP),
                text = stringResourceSafe(
                    R.string.prediction_place_min_amount_error,
                    caption.minAmount.formatPolymarketMoney(),
                ),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.status.error,
                textAlign = TextAlign.Center,
            )
            is AmountCaptionUM.ToWin -> TangemBadge(
                modifier = Modifier.padding(top = CAPTION_GAP),
                text = stringReference(
                    stringResourceSafe(
                        R.string.prediction_place_to_win,
                        caption.payout.formatPolymarketMoney(),
                    ),
                ),
                status = TangemBadge.Status.Success,
                size = TangemBadge.Size.X6,
            )
            null -> Unit
        }
    }
}

private val CAPTION_GAP = 12.dp
package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.fields.AmountTextField
import com.tangem.core.ui.components.fields.AmountTextFieldColors
import com.tangem.core.ui.components.fields.visualtransformations.AmountVisualTransformation
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.common.USD_CODE
import com.tangem.features.polymarket.impl.common.USD_SYMBOL
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.features.polymarket.impl.placeprediction.entity.PaymentSourceUM
import java.math.BigDecimal

@Composable
internal fun PredictionAmountBlock(
    amountValue: String,
    toWin: BigDecimal?,
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
                onAmountChange = onAmountChange,
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = TangemTheme.colors3.border.secondary,
            )
            TangemRow(
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
private fun AmountSection(amountValue: String, toWin: BigDecimal?, onAmountChange: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 4.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.prediction_place_you_will_pay),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        AmountTextField(
            value = amountValue,
            decimals = COLLATERAL_DECIMALS,
            onValueChange = onAmountChange,
            textStyle = TangemTheme.typography3.display.medium,
            colors = AmountTextFieldColors(
                textColor = TangemTheme.colors3.text.primary,
                disabledTextColor = TangemTheme.colors3.text.tertiary,
                backgroundColor = Color.Transparent,
            ),
            visualTransformation = AmountVisualTransformation(
                decimals = COLLATERAL_DECIMALS,
                symbol = USD_SYMBOL,
                currencyCode = USD_CODE,
                symbolColor = if (amountValue.isBlank()) {
                    TangemTheme.colors3.text.tertiary
                } else {
                    TangemTheme.colors3.text.primary
                },
            ),
        )
        if (toWin != null) {
            TangemBadge(
                text = stringReference(
                    stringResourceSafe(R.string.prediction_place_to_win, toWin.formatPolymarketMoney()),
                ),
                status = TangemBadge.Status.Info,
                size = TangemBadge.Size.X6,
            )
        }
    }
}

private const val COLLATERAL_DECIMALS = 2
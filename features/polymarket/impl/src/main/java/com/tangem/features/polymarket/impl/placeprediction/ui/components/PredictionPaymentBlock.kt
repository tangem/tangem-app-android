package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.account.AccountResIcon
import com.tangem.core.ui.components.account.PredictionAccountIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.common.formatPolymarketBalance
import com.tangem.features.polymarket.impl.common.formatPolymarketEnteredAmount
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape
import com.tangem.features.polymarket.impl.placeprediction.entity.CollateralUM
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.balance
import com.tangem.features.polymarket.impl.common.formatPolymarketShares
import com.tangem.utils.StringsSigns.DASH_SIGN
import java.math.BigDecimal

/** What the order is paid from and what it pays out, as read on the summary screen. */
@Composable
internal fun PredictionPaymentBlock(
    amountValue: String,
    payout: BigDecimal?,
    collateral: CollateralUM,
    order: OrderSideUM,
    modifier: Modifier = Modifier,
) {
    TangemSurface(modifier = modifier, color = TangemTheme.colors3.bg.secondary, shape = PolymarketCardShape) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 24.dp),
        ) {
            SourceHeader(collateral = collateral, order = order)
            when (order) {
                is OrderSideUM.Buy -> BuyAmountRow(amountValue = amountValue, payout = payout, collateral = collateral)
                is OrderSideUM.Sell -> SellAmountRow(
                    amountValue = amountValue,
                    payout = payout,
                    collateral = collateral,
                )
            }
        }
    }
}

@Composable
private fun SourceHeader(collateral: CollateralUM, order: OrderSideUM) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 2.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderText(
                text = stringResourceSafe(
                    when (order) {
                        is OrderSideUM.Buy -> R.string.prediction_place_summary_from
                        is OrderSideUM.Sell -> R.string.prediction_close_summary_to
                    },
                ),
            )
            AccountResIcon(
                imageVector = PredictionAccountIcon.imageVector,
                color = PredictionAccountIcon.color,
                size = AccountIconSize.ExtraSmall,
            )
            HeaderText(text = stringResourceSafe(R.string.prediction_place_summary_account))
        }
        HeaderText(
            text = stringResourceSafe(
                R.string.common_balance,
                order.balance.formatPolymarketBalance(tokenSymbol = collateral.tokenSymbol),
            ),
        )
    }
}

@Composable
private fun BuyAmountRow(amountValue: String, payout: BigDecimal?, collateral: CollateralUM) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            Text(
                text = amountValue.parseToBigDecimal(PolymarketConst.COLLATERAL_DECIMALS)
                    .formatPolymarketEnteredAmount(),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            if (payout != null) {
                TangemBadge(
                    text = stringReference(
                        value = stringResourceSafe(
                            R.string.prediction_place_to_win,
                            payout.formatPolymarketMoney(),
                        ),
                    ),
                    status = TangemBadge.Status.Success,
                    size = TangemBadge.Size.X4,
                )
            }
        }
        collateral.iconState?.let { CurrencyIcon(state = it, shouldDisplayNetwork = false) }
    }
}

@Composable
private fun SellAmountRow(amountValue: String, payout: BigDecimal?, collateral: CollateralUM) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(weight = 1f),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            Text(
                text = payout?.formatPolymarketMoney() ?: DASH_SIGN,
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.status.success,
            )
            Text(
                text = stringResourceSafe(
                    R.string.prediction_close_selling_tokens,
                    amountValue.parseToBigDecimal(PolymarketConst.SHARES_DECIMALS).formatPolymarketShares(),
                ),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
        collateral.iconState?.let { CurrencyIcon(state = it, shouldDisplayNetwork = false) }
    }
}

@Composable
private fun HeaderText(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography3.caption.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}
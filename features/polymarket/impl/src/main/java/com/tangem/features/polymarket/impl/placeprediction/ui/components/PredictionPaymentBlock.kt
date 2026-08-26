package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.features.polymarket.impl.placeprediction.entity.PaymentSourceUM
import java.math.BigDecimal

/** What the order is paid from and what it pays out, as read on the summary screen. */
@Composable
internal fun PredictionPaymentBlock(
    amountValue: String,
    toWin: BigDecimal?,
    payment: PaymentSourceUM,
    modifier: Modifier = Modifier,
) {
    TangemSurface(modifier = modifier, color = TangemTheme.colors3.bg.secondary) {
        Column(modifier = Modifier.padding(all = 16.dp), verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            TangemRow(
                includeInnerPaddings = false,
                titleSlot = {
                    Text(
                        text = stringResourceSafe(R.string.prediction_place_summary_from),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.secondary,
                    )
                },
                valueSlot = {
                    Text(
                        text = stringResourceSafe(
                            R.string.common_balance,
                            payment.balance?.formatPolymarketMoney() ?: DASH_SIGN,
                        ),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.secondary,
                    )
                },
            )
            TangemBadge(
                text = stringReference(stringResourceSafe(R.string.prediction_place_summary_account)),
                status = TangemBadge.Status.Info,
                size = TangemBadge.Size.X6,
            )
            TangemRow(
                includeInnerPaddings = false,
                titleSlot = {
                    Text(
                        text = amountValue.toBigDecimalOrNull()?.formatPolymarketMoney().orEmpty(),
                        style = TangemTheme.typography3.heading.medium,
                        color = TangemTheme.colors3.text.primary,
                    )
                },
                valueSlot = {
                    Text(
                        text = payment.tokenSymbol,
                        style = TangemTheme.typography3.body.medium,
                        color = TangemTheme.colors3.text.secondary,
                    )
                },
                extraBottomSlot = toWin?.let {
                    {
                        TangemBadge(
                            text = stringReference(
                                value = stringResourceSafe(
                                    R.string.prediction_place_to_win,
                                    it.formatPolymarketMoney(),
                                ),
                            ),
                            status = TangemBadge.Status.Info,
                            size = TangemBadge.Size.X6,
                        )
                    }
                },
            )
        }
    }
}
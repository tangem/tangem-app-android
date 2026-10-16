package com.tangem.features.polymarket.impl.history.model

import com.tangem.core.res.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.anyDecimals
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivity.Type
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.formatPolymarketPrice
import com.tangem.features.polymarket.impl.common.formatPolymarketShares
import com.tangem.features.polymarket.impl.common.polymarketOutcomeAccent
import com.tangem.features.polymarket.impl.common.toBadgeStatus
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityDetailsUM
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityDetailsUM.Row
import com.tangem.utils.StringsSigns.MINUS
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle

internal class PolymarketActivityDetailsConverter(
    private val dateFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .appendLocalized(FormatStyle.MEDIUM, null)
        .appendLiteral(" · ")
        .appendLocalized(null, FormatStyle.SHORT)
        .toFormatter().withZone(ZoneId.systemDefault()),
) {

    fun convert(activity: PolymarketActivity, onGoToPrediction: (() -> Unit)?): PolymarketActivityDetailsUM {
        val outcome = activity.market?.outcome?.takeIf { it.isNotBlank() }
        return PolymarketActivityDetailsUM(
            title = activity.rowType().title(),
            date = stringReference(dateFormatter.format(activity.occurredAt)),
            amount = stringReference(activity.detailsAmount()),
            marketTitle = activity.marketTitle.takeIf { it.isNotBlank() }?.let(
                ::stringReference,
            ),
            iconUrl = activity.historyIconUrl,
            outcomePrice = activity.order?.averagePrice?.takeIf { outcome != null }
                ?.formatPolymarketPrice(isResolved = true)?.let(::stringReference),
            outcome = outcome?.let(::stringReference),
            outcomeStatus = when {
                outcome.equals("Yes", ignoreCase = true) -> TangemBadge.Status.Success
                else -> polymarketOutcomeAccent(activity.market?.outcomeIndex, outcome.orEmpty()).toBadgeStatus()
            },
            rows = tradeRows(activity),
            onGoToPrediction = onGoToPrediction,
        )
    }

    private fun PolymarketActivity.detailsAmount(): String {
        val money = amount.abs().formatPolymarketMoney()
        return when (type) {
            Type.BUY, Type.WITHDRAW -> "$MINUS$money"
            else -> money
        }
    }

    private fun tradeRows(activity: PolymarketActivity) = buildList {
        activity.market?.let { market ->
            add(row(resourceReference(R.string.prediction_positions_quantity), market.shares.formatQuantity()))
        }
        activity.order?.let { order ->
            add(row(resourceReference(R.string.prediction_history_price), order.averagePrice.formatPrice()))
            add(
                row(
                    resourceReference(R.string.prediction_history_fees),
                    (order.platformFee + order.builderFee).formatPolymarketMoney(),
                ),
            )
            add(
                row(
                    resourceReference(R.string.prediction_history_notional_gross),
                    order.notional.formatPolymarketMoney(),
                ),
            )
        }
    }.toImmutableList()

    private fun row(title: TextReference, value: String) = Row(title, stringReference(value))

    private fun BigDecimal.formatQuantity(): String {
        if (signum() > 0 && this < PolymarketConst.SHARES_DISPLAY_STEP) return formatPolymarketShares()
        return NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = PolymarketConst.SHARES_DISPLAY_DECIMALS
            maximumFractionDigits = PolymarketConst.SHARES_DISPLAY_DECIMALS
            roundingMode = RoundingMode.FLOOR
        }.format(this)
    }

    // Historical average execution prices can have sub-cent precision; never clamp them as a live quote.
    private fun BigDecimal.formatPrice(): String = format {
        fiat(PolymarketConst.USD_CODE, PolymarketConst.USD_SYMBOL)
            .anyDecimals(stripTrailingZeros().scale().coerceAtLeast(2))
    }
}
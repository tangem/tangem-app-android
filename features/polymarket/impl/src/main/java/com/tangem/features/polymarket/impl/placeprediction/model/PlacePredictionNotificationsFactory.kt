package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.enteredAmount
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal object PlacePredictionNotificationsFactory {

    fun build(state: PlacePredictionUM): ImmutableList<PredictionNotificationUM> = buildList {
        when (val quote = state.quote) {
            is QuoteUM.Content -> when (quote.status) {
                PredictionQuoteStatus.FULL -> Unit
                PredictionQuoteStatus.PARTIAL -> add(PredictionNotificationUM.PartialFill)
                PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY -> add(PredictionNotificationUM.NoLiquidity)
                PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE ->
                    add(PredictionNotificationUM.BelowMinOrderSize(minOrderSize = quote.minOrderSize))
                PredictionQuoteStatus.MARKET_CLOSED -> add(PredictionNotificationUM.MarketClosed)
            }
            is QuoteUM.Error -> add(PredictionNotificationUM.QuoteFailed)
            QuoteUM.Empty, QuoteUM.Loading -> Unit
        }

        if (state.tradingPermission == TradingPermissionUM.Restricted) add(PredictionNotificationUM.RegionRestricted)
    }.toImmutableList()
}

/**
 * The single place the primary button is gated.
 *
 * Every transformer ends with this, so the notifications and the button can never disagree with the state
 * that produced them. Nothing else — no model, no composable — may compute `isPrimaryButtonEnabled`: the
 * flow this one is modelled on gates its button from three places, one of them outside the notifications.
 */
internal fun PlacePredictionUM.recomputeGate(): PlacePredictionUM {
    val entered = enteredAmount()
    val required = (quote as? QuoteUM.Content)?.total ?: entered
    val balance = payment.balance
    val hasSufficientBalance = required != null && balance != null && balance >= required

    val priced = copy(
        payment = payment.copy(hasSufficientBalance = hasSufficientBalance),
        amountError = amountError(entered = entered, required = required, balance = balance),
    )
    val notifications = PlacePredictionNotificationsFactory.build(state = priced)

    return priced.copy(
        notifications = notifications,
        isPrimaryButtonEnabled = notifications.none { it.isBlocking } &&
            quote is QuoteUM.Content &&
            submit is SubmitUM.Idle &&
            hasSufficientBalance &&
            tradingPermission == TradingPermissionUM.Allowed,
    )
}

/** A balance nobody has read yet accuses the user of nothing, so an unknown one names no shortfall. */
private fun amountError(entered: BigDecimal?, required: BigDecimal?, balance: BigDecimal?): AmountErrorUM? = when {
    balance == null || entered == null -> null
    entered > balance -> AmountErrorUM.NotEnoughForBet
    required != null && required > balance -> AmountErrorUM.NotEnoughForFees
    else -> null
}
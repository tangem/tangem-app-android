package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.enteredAmount
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal object PlacePredictionNotificationsFactory {

    fun build(state: PlacePredictionUM): ImmutableList<PredictionNotificationUM> = buildList {
        when (val quote = state.quote) {
            is QuoteUM.Content -> when (quote.status) {
                PredictionQuoteStatus.FULL -> Unit
                PredictionQuoteStatus.PARTIAL -> add(PredictionNotificationUM.PartialFill)
                PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE ->
                    add(PredictionNotificationUM.BelowMinOrderSize(minOrderSize = quote.minOrderSize))
                // A placeable status is the only one Content is built for
                PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY, PredictionQuoteStatus.MARKET_CLOSED -> Unit
            }
            is QuoteUM.Unavailable -> when (quote.status) {
                PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY -> add(PredictionNotificationUM.NoLiquidity)
                PredictionQuoteStatus.MARKET_CLOSED -> add(PredictionNotificationUM.MarketClosed)
                PredictionQuoteStatus.FULL,
                PredictionQuoteStatus.PARTIAL,
                PredictionQuoteStatus.BELOW_MIN_ORDER_SIZE,
                -> Unit
            }
            is QuoteUM.Error -> add(PredictionNotificationUM.QuoteFailed)
            QuoteUM.Empty, QuoteUM.Loading -> Unit
        }

        val isBalanceKnown = state.payment.balance != null
        if (isBalanceKnown && state.quote is QuoteUM.Content && !state.payment.hasSufficientBalance) {
            add(PredictionNotificationUM.InsufficientBalance)
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
    val required = (quote as? QuoteUM.Content)?.total ?: enteredAmount()
    val balance = payment.balance
    val hasSufficientBalance = required != null && balance != null && balance >= required

    val priced = copy(payment = payment.copy(hasSufficientBalance = hasSufficientBalance))
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
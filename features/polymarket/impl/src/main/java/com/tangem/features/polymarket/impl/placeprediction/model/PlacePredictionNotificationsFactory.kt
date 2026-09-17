package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountCaptionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PaymentAmountErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PriceErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.enteredAmount
import com.tangem.features.polymarket.impl.placeprediction.entity.isBelowMinAmount
import com.tangem.features.polymarket.impl.placeprediction.entity.payout
import com.tangem.features.polymarket.impl.placeprediction.entity.SubmitUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal object PlacePredictionNotificationsFactory {

    fun build(state: PlacePredictionUM): ImmutableList<PredictionNotificationUM> = buildList {
        if (!state.isMarketAcceptingOrders) add(PredictionNotificationUM.MarketClosed)

        addAll(state.quote.notifications())

        when (state.priceError) {
            PriceErrorUM.MarketClosed -> add(PredictionNotificationUM.MarketClosed)
            PriceErrorUM.Unknown -> add(PredictionNotificationUM.PriceFailed)
            PriceErrorUM.EmptyBook,
            null,
            -> Unit
        }

        if (state.tradingPermission == TradingPermissionUM.Restricted) add(PredictionNotificationUM.RegionRestricted)
    }.distinct().toImmutableList()

    private fun QuoteUM.notifications(): List<PredictionNotificationUM> = when (this) {
        is QuoteUM.Content -> when (status) {
            PredictionQuoteStatus.PARTIAL -> listOf(PredictionNotificationUM.PartialFill)
            else -> emptyList()
        }
        is QuoteUM.Unavailable -> when (status) {
            PredictionQuoteStatus.INSUFFICIENT_LIQUIDITY -> listOf(PredictionNotificationUM.NoLiquidity)
            PredictionQuoteStatus.MARKET_CLOSED -> listOf(PredictionNotificationUM.MarketClosed)
            else -> emptyList()
        }
        is QuoteUM.Error -> listOf(PredictionNotificationUM.QuoteFailed)
        QuoteUM.Empty, QuoteUM.Loading -> emptyList()
    }
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
    val isBelowMinAmount = isBelowMinAmount()

    val priced = copy(
        payment = payment.copy(hasSufficientBalance = hasSufficientBalance),
        amountCaption = amountCaption(
            entered = entered,
            required = required,
            balance = balance,
            isBelowMinAmount = isBelowMinAmount,
        ),
    )
    val notifications = PlacePredictionNotificationsFactory.build(state = priced)

    return priced.copy(
        notifications = notifications,
        isPrimaryButtonEnabled = notifications.none { it.isBlocking } &&
            quote is QuoteUM.Content &&
            quote.status.isPlaceable &&
            !isBelowMinAmount &&
            submit is SubmitUM.Idle &&
            hasSufficientBalance &&
            tradingPermission == TradingPermissionUM.Allowed,
    )
}

private fun PlacePredictionUM.amountCaption(
    entered: BigDecimal?,
    required: BigDecimal?,
    balance: BigDecimal?,
    isBelowMinAmount: Boolean,
): AmountCaptionUM? {
    val error = paymentAmountError(entered = entered, required = required, balance = balance)
    val minAmount = minAmount

    return when {
        error != null -> AmountCaptionUM.Shortfall(error = error)
        isBelowMinAmount && minAmount != null -> AmountCaptionUM.BelowMinimum(minAmount = minAmount)
        else -> quote.payout()?.let(AmountCaptionUM::ToWin)
    }
}

private fun paymentAmountError(
    entered: BigDecimal?,
    required: BigDecimal?,
    balance: BigDecimal?,
): PaymentAmountErrorUM? = when {
    balance == null || entered == null -> null
    entered > balance -> PaymentAmountErrorUM.NotEnoughForBet
    required != null && required > balance -> PaymentAmountErrorUM.NotEnoughForFees
    else -> null
}
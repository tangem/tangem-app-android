package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.placeprediction.entity.AmountCaptionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PaymentAmountErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PriceErrorUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SharesProceedsUM
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

        val order = state.order
        if (order is OrderSideUM.Sell) {
            when (order.position) {
                SellPositionUM.NotHeld,
                SellPositionUM.Unreadable,
                -> add(PredictionNotificationUM.PositionUnavailable)
                SellPositionUM.Loading,
                is SellPositionUM.Held,
                -> Unit
            }
        }
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
    val isBelowMinAmount = isBelowMinAmount()

    val priced = when (val order = order) {
        is OrderSideUM.Buy -> pricedBuy(order = order, entered = entered, isBelowMinAmount = isBelowMinAmount)
        is OrderSideUM.Sell -> pricedSell(order = order)
    }
    val notifications = PlacePredictionNotificationsFactory.build(state = priced)

    return priced.copy(
        notifications = notifications,
        isPrimaryButtonEnabled = notifications.none { it.isBlocking } &&
            quote is QuoteUM.Content &&
            priced.order.isPlaceable(status = quote.status) &&
            !isBelowMinAmount &&
            submit is SubmitUM.Idle &&
            tradingPermission == TradingPermissionUM.Allowed &&
            priced.order.allowsPlacing(entered = entered),
    )
}

private fun OrderSideUM.isPlaceable(status: PredictionQuoteStatus): Boolean = when (this) {
    is OrderSideUM.Buy -> status.isPlaceable
    is OrderSideUM.Sell -> status.isPlaceableForSell
}

private fun OrderSideUM.allowsPlacing(entered: BigDecimal?): Boolean = when (this) {
    is OrderSideUM.Buy -> hasSufficientBalance
    is OrderSideUM.Sell -> position is SellPositionUM.Held &&
        entered != null &&
        entered > BigDecimal.ZERO &&
        entered <= position.available
}

private fun PlacePredictionUM.pricedBuy(
    order: OrderSideUM.Buy,
    entered: BigDecimal?,
    isBelowMinAmount: Boolean,
): PlacePredictionUM {
    val required = (quote as? QuoteUM.Content)?.total ?: entered
    val balance = order.balance
    val hasSufficientBalance = required != null && balance != null && balance >= required

    return copy(
        order = order.copy(hasSufficientBalance = hasSufficientBalance),
        amountCaption = buyCaption(
            entered = entered,
            required = required,
            balance = balance,
            isBelowMinAmount = isBelowMinAmount,
        ),
    )
}

private fun PlacePredictionUM.pricedSell(order: OrderSideUM.Sell): PlacePredictionUM = copy(
    order = order.copy(proceeds = sharesProceeds()),
    amountCaption = null,
)

private fun PlacePredictionUM.sharesProceeds(): SharesProceedsUM = when (quote) {
    is QuoteUM.Loading -> SharesProceedsUM.Loading
    is QuoteUM.Content -> SharesProceedsUM.Content(expected = quote.expected)
    else -> SharesProceedsUM.Unavailable
}

private fun PlacePredictionUM.buyCaption(
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
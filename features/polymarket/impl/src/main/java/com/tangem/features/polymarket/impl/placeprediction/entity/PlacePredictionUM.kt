package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.common.formatSlippagePercent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Immutable
internal data class PlacePredictionUM(
    val market: MarketHeaderUM,
    val collateral: CollateralUM,
    val order: OrderSideUM,
    val amountValue: String,
    val slippage: SlippageUM,
    val quote: QuoteUM,
    val isMarketAcceptingOrders: Boolean,
    val tradingPermission: TradingPermissionUM,
    val notifications: ImmutableList<PredictionNotificationUM>,
    val amountCaption: AmountCaptionUM? = null,
    val minAmount: BigDecimal? = null,
    val priceError: PriceErrorUM? = null,
    val slippagePicker: SlippagePickerUM,
    val submit: SubmitUM,
    val isPrimaryButtonEnabled: Boolean,
) {

    companion object {

        fun initial(side: PredictionOrderSide): PlacePredictionUM = PlacePredictionUM(
            market = MarketHeaderUM(
                title = "",
                imageUrl = null,
                outcomeTitle = "",
                outcomePriceCents = null,
                resolutionDateMillis = null,
            ),
            collateral = CollateralUM(tokenSymbol = "", iconState = null),
            order = when (side) {
                PredictionOrderSide.BUY -> OrderSideUM.Buy(balance = null, hasSufficientBalance = false)
                PredictionOrderSide.SELL -> OrderSideUM.Sell(
                    available = BigDecimal.ZERO,
                    proceeds = SharesProceedsUM.Unavailable,
                    balance = null,
                )
            },
            amountValue = "",
            slippage = SlippageUM.of(percent = DEFAULT_SLIPPAGE_PERCENT),
            quote = QuoteUM.Empty,
            isMarketAcceptingOrders = true,
            tradingPermission = TradingPermissionUM.Unknown,
            notifications = persistentListOf(),
            slippagePicker = SlippagePickerUM.empty(),
            submit = SubmitUM.Idle,
            isPrimaryButtonEnabled = false,
        )
    }
}

/**
 * The sum to quote, or `null` while what the user typed is not one yet.
 *
 * Anything below a cent is not one: the BFF normalises amounts onto a 2-decimal grid, so a finer figure
 * becomes zero there and comes back as a `400` rather than a quote.
 */
internal fun PlacePredictionUM.enteredAmount(): BigDecimal? =
    amountValue.toBigDecimalOrNull()?.takeIf { it >= MIN_QUOTABLE_AMOUNT }

private val MIN_QUOTABLE_AMOUNT: BigDecimal = BigDecimal("0.01")

internal fun PlacePredictionUM.isPositionReadable(): Boolean = (order as? OrderSideUM.Sell)?.isPositionReadable != false

internal fun PlacePredictionUM.isBelowMinAmount(): Boolean {
    val entered = enteredAmount() ?: return false
    val minAmount = minAmount ?: return false
    return entered < minAmount
}

@Immutable
internal data class MarketHeaderUM(
    val title: String,
    val imageUrl: String?,
    val outcomeTitle: String,
    val outcomePriceCents: Int?,
    val resolutionDateMillis: Long?,
    val isPriceUnavailable: Boolean = false,
    val entryPriceCents: Int? = null,
)

@Immutable
internal sealed interface AmountCaptionUM {

    data class ToWin(val payout: PayoutUM) : AmountCaptionUM

    data class Shortfall(val error: PaymentAmountErrorUM) : AmountCaptionUM

    data class BelowMinimum(val minAmount: BigDecimal) : AmountCaptionUM
}

internal enum class PaymentAmountErrorUM(val text: TextReference) {
    NotEnoughForBet(text = resourceReference(R.string.prediction_place_insufficient_balance_bet)),
    NotEnoughForFees(text = resourceReference(R.string.prediction_place_insufficient_balance_fees)),
}

@Immutable
internal data class CollateralUM(val tokenSymbol: String, val iconState: CurrencyIconState?)

@Immutable
internal sealed interface OrderSideUM {

    data class Buy(val balance: BigDecimal?, val hasSufficientBalance: Boolean) : OrderSideUM

    /**
     * @property available the held shares the market can accept, truncated onto its 2-decimal grid
     * @property isPositionReadable false once the position behind the sell could not be read at all, which
     *  leaves nothing to sell and nothing worth quoting
     */
    data class Sell(
        val available: BigDecimal,
        val proceeds: SharesProceedsUM,
        val balance: BigDecimal?,
        val isPositionReadable: Boolean = true,
    ) : OrderSideUM
}

internal val OrderSideUM.side: PredictionOrderSide
    get() = when (this) {
        is OrderSideUM.Buy -> PredictionOrderSide.BUY
        is OrderSideUM.Sell -> PredictionOrderSide.SELL
    }

internal val OrderSideUM.balance: BigDecimal?
    get() = when (this) {
        is OrderSideUM.Buy -> balance
        is OrderSideUM.Sell -> balance
    }

internal enum class TradingPermissionUM { Unknown, Allowed, Restricted }

internal enum class PriceErrorUM { MarketClosed, EmptyBook, Unknown }

@Immutable
internal data class SlippageUM(val percent: BigDecimal, val isDefault: Boolean) {

    companion object {

        fun of(percent: BigDecimal): SlippageUM = SlippageUM(
            percent = percent,
            isDefault = percent.compareTo(DEFAULT_SLIPPAGE_PERCENT) == 0,
        )
    }
}

internal fun SlippageUM.label(): TextReference {
    val formatted = percent.formatSlippagePercent()

    return if (isDefault) {
        resourceReference(R.string.prediction_place_slippage_default, wrappedList(formatted))
    } else {
        stringReference(formatted)
    }
}

@Immutable
internal sealed interface QuoteUM {

    data object Empty : QuoteUM

    data object Loading : QuoteUM

    /**
     * @property expected the leg the user receives at the average price — shares on a buy, collateral on a sell
     * @property guaranteed the same leg at the worst-case price
     */
    data class Content(
        val status: PredictionQuoteStatus,
        val expected: BigDecimal,
        val guaranteed: BigDecimal,
        val feeTotal: BigDecimal,
        val total: BigDecimal,
        val signable: SignableQuoteUM,
    ) : QuoteUM

    data class Unavailable(val status: PredictionQuoteStatus) : QuoteUM

    data class Error(val reason: QuoteErrorUM) : QuoteUM
}

internal enum class QuoteErrorUM { Network, Unknown }

@Immutable
internal data class SignableQuoteUM(val quote: PredictionOrderQuote, val receivedAtMillis: Long)

@Immutable
internal data class PayoutUM(val expected: BigDecimal, val guaranteed: BigDecimal)

internal fun QuoteUM.payout(): PayoutUM? = (this as? QuoteUM.Content)
    ?.let { PayoutUM(expected = it.expected, guaranteed = it.guaranteed) }

internal fun QuoteUM.isMarketClosed(): Boolean =
    (this as? QuoteUM.Unavailable)?.status == PredictionQuoteStatus.MARKET_CLOSED

@Immutable
internal sealed interface SubmitUM {

    data object Idle : SubmitUM

    data object Signing : SubmitUM

    data object Posting : SubmitUM

    data object AwaitingFill : SubmitUM

    data class Result(val result: PlaceResultUM) : SubmitUM
}
package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import com.tangem.features.polymarket.impl.common.formatSlippagePercent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Immutable
internal data class PlacePredictionUM(
    val market: MarketHeaderUM,
    val payment: PaymentSourceUM,
    val amountValue: String,
    val slippage: SlippageUM,
    val quote: QuoteUM,
    val isMarketAcceptingOrders: Boolean,
    val tradingPermission: TradingPermissionUM,
    val notifications: ImmutableList<PredictionNotificationUM>,
    val amountCaption: AmountCaptionUM? = null,
    val priceError: PriceErrorUM? = null,
    val slippagePicker: SlippagePickerUM,
    val submit: SubmitUM,
    val isPrimaryButtonEnabled: Boolean,
) {

    companion object {

        fun initial(): PlacePredictionUM = PlacePredictionUM(
            market = MarketHeaderUM(
                title = "",
                imageUrl = null,
                outcomeTitle = "",
                outcomePriceCents = null,
                resolutionDateMillis = null,
            ),
            payment = PaymentSourceUM(
                tokenSymbol = "",
                iconState = null,
                balance = null,
                hasSufficientBalance = false,
            ),
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

@Immutable
internal data class MarketHeaderUM(
    val title: String,
    val imageUrl: String?,
    val outcomeTitle: String,
    val outcomePriceCents: Int?,
    val resolutionDateMillis: Long?,
    val isPriceUnavailable: Boolean = false,
)

@Immutable
internal sealed interface AmountCaptionUM {

    data class ToWin(val payout: PayoutUM) : AmountCaptionUM

    data class Shortfall(val error: PaymentAmountErrorUM) : AmountCaptionUM
}

internal enum class PaymentAmountErrorUM(val text: TextReference) {
    NotEnoughForBet(text = resourceReference(R.string.prediction_place_insufficient_balance_bet)),
    NotEnoughForFees(text = resourceReference(R.string.prediction_place_insufficient_balance_fees)),
}

@Immutable
internal data class PaymentSourceUM(
    val tokenSymbol: String,
    val iconState: CurrencyIconState?,
    val balance: BigDecimal?,
    val hasSufficientBalance: Boolean,
)

internal enum class TradingPermissionUM { Unknown, Allowed, Restricted }

internal enum class PriceErrorUM { MarketClosed, Unknown }

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

    data class Content(
        val status: PredictionQuoteStatus,
        val expectedShares: BigDecimal,
        val guaranteedShares: BigDecimal,
        val feeTotal: BigDecimal,
        val total: BigDecimal,
        val minOrderSize: BigDecimal,
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
    ?.let { PayoutUM(expected = it.expectedShares, guaranteed = it.guaranteedShares) }

internal fun QuoteUM.isMarketClosed(): Boolean =
    (this as? QuoteUM.Unavailable)?.status == PredictionQuoteStatus.MARKET_CLOSED

@Immutable
internal sealed interface SubmitUM {

    data object Idle : SubmitUM

    data object Signing : SubmitUM

    data object Posting : SubmitUM

    data class Result(val result: PlaceResultUM) : SubmitUM
}
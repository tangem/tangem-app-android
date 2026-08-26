package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
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
    val tradingPermission: TradingPermissionUM,
    val notifications: ImmutableList<PredictionNotificationUM>,
    val submit: SubmitUM,
    val isPrimaryButtonEnabled: Boolean,
) {

    companion object {

        fun initial(): PlacePredictionUM = PlacePredictionUM(
            market = MarketHeaderUM(title = "", imageUrl = null, outcomeTitle = "", outcomePriceCents = null),
            payment = PaymentSourceUM(tokenSymbol = "", balance = null, hasSufficientBalance = false),
            amountValue = "",
            slippage = SlippageUM(percent = DEFAULT_SLIPPAGE_PERCENT, isDefault = true),
            quote = QuoteUM.Empty,
            tradingPermission = TradingPermissionUM.Unknown,
            notifications = persistentListOf(),
            submit = SubmitUM.Idle,
            isPrimaryButtonEnabled = false,
        )
    }
}

internal val DEFAULT_SLIPPAGE_PERCENT: BigDecimal = BigDecimal("3")

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
)

@Immutable
internal data class PaymentSourceUM(
    val tokenSymbol: String,
    val balance: BigDecimal?,
    val hasSufficientBalance: Boolean,
)

internal enum class TradingPermissionUM { Unknown, Allowed, Restricted }

@Immutable
internal data class SlippageUM(val percent: BigDecimal, val isDefault: Boolean)

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
    ) : QuoteUM

    data class Unavailable(val status: PredictionQuoteStatus) : QuoteUM

    data class Error(val reason: QuoteErrorUM) : QuoteUM
}

internal enum class QuoteErrorUM { Network, Unknown }

@Immutable
internal data class PayoutUM(val expected: BigDecimal, val guaranteed: BigDecimal)

internal fun QuoteUM.payout(): PayoutUM? = (this as? QuoteUM.Content)
    ?.let { PayoutUM(expected = it.expectedShares, guaranteed = it.guaranteedShares) }

@Immutable
internal sealed interface SubmitUM {

    data object Idle : SubmitUM

    data object Signing : SubmitUM

    data object Posting : SubmitUM

    data class Result(val result: PlaceResultUM) : SubmitUM
}
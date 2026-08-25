package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable
import com.tangem.domain.polymarket.model.PredictionQuoteStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

/**
 * State of the whole place-prediction flow. Every step renders a slice of it; nothing else holds state.
 *
 * [notifications], [PaymentSourceUM.hasSufficientBalance] and [isPrimaryButtonEnabled] are derived — they are
 * never assigned directly, only produced by
 * [com.tangem.features.polymarket.impl.placeprediction.model.recomputeGate].
 */
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

internal val DEFAULT_SLIPPAGE_PERCENT: BigDecimal = BigDecimal("0.25")

/**
 * @property outcomePriceCents the outcome's price as a caption, `null` when the backend states none. A price
 *  nobody reported is not a price of zero, and the screen has nothing to show in its place.
 */
@Immutable
internal data class MarketHeaderUM(
    val title: String,
    val imageUrl: String?,
    val outcomeTitle: String,
    val outcomePriceCents: Int?,
)

/**
 * @property balance the spending power the order is paid from, `null` while it has not been read. A balance
 *  the exchange did not answer for is not a balance of zero: reading it as one would tell the user their
 *  money is gone and refuse the order in the same breath.
 */
@Immutable
internal data class PaymentSourceUM(
    val tokenSymbol: String,
    val balance: BigDecimal?,
    val hasSufficientBalance: Boolean,
)

/**
 * Whether the region allows placing an order. The flow reads this itself, so until the answer arrives it is
 * neither of the two: [Unknown] keeps the button shut without claiming the user is restricted, which a
 * plain `false` could not express and which the screen would otherwise announce as a restriction.
 */
internal enum class TradingPermissionUM { Unknown, Allowed, Restricted }

@Immutable
internal data class SlippageUM(val percent: BigDecimal, val isDefault: Boolean)

@Immutable
internal sealed interface QuoteUM {

    data object Empty : QuoteUM

    data object Loading : QuoteUM

    data class Content(
        val status: PredictionQuoteStatus,
        val shares: BigDecimal,
        val toWin: BigDecimal,
        val feeTotal: BigDecimal,
        val total: BigDecimal,
        val minOrderSize: BigDecimal,
    ) : QuoteUM

    data class Error(val reason: QuoteErrorUM) : QuoteUM
}

internal enum class QuoteErrorUM { Network, Unknown }

@Immutable
internal sealed interface SubmitUM {

    data object Idle : SubmitUM

    data object Signing : SubmitUM

    data object Posting : SubmitUM

    data class Result(val result: PlaceResultUM) : SubmitUM
}
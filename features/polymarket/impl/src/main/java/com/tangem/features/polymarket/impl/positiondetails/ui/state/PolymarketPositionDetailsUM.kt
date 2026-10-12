package com.tangem.features.polymarket.impl.positiondetails.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class PolymarketPositionDetailsUM(
    val content: Content,
    val onCloseClick: () -> Unit,
) {
    @Immutable
    sealed interface Content {
        data object Loading : Content
        data object Unavailable : Content
        data class Error(val onRetry: () -> Unit) : Content

        data class Data(
            val summary: Summary,
            val event: Event?,
            val metrics: Metrics,
            val information: Information,
            val notice: Notice? = null,
            val onClosePositionClick: (() -> Unit)? = null,
            val onBuyMoreClick: (() -> Unit)? = null,
        ) : Content
    }

    enum class Notice { Pending, Disputed }

    enum class Pnl { Unrealized, Realized }

    @Immutable
    data class Summary(
        val title: TextReference,
        val iconUrl: String?,
        val pnl: TangemPriceChange.State,
        val currentValue: TextReference?,
        val outcomeLabel: TextReference,
        val outcomeVariant: TangemMessageBubble.Variant,
        val payout: TextReference?,
        val result: TangemBadgeUM? = null,
    )

    @Immutable
    data class Event(
        val title: TextReference,
        val iconUrl: String?,
        val volume: TextReference?,
        val onClick: (() -> Unit)? = null,
    )

    @Immutable
    data class Metrics(
        val quantity: TextReference,
        val priceChanges: TextReference,
        val unrealizedPnl: TextReference,
        val realizedPnl: TextReference,
    )

    @Immutable
    data class Information(
        val invested: TextReference,
        val status: TextReference,
        val endDate: TextReference?,
    )
}
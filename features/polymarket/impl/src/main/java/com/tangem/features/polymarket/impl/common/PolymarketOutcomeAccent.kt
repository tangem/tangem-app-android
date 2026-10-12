package com.tangem.features.polymarket.impl.common

import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.domain.polymarket.model.PolymarketPosition

/**
 * Accent of an outcome, driven by its position in the market the way the feed paints its buttons: the first
 * outcome is [Info], the second is [Error]. Without an index the "Yes" / "No" labels keep their known accents.
 */
internal enum class PolymarketOutcomeAccent {
    Info,
    Error,
    Neutral,
}

internal fun polymarketOutcomeAccent(outcomeIndex: Int?, outcome: String): PolymarketOutcomeAccent = when {
    outcomeIndex == 0 -> PolymarketOutcomeAccent.Info
    outcomeIndex == 1 -> PolymarketOutcomeAccent.Error
    outcome.equals("Yes", ignoreCase = true) -> PolymarketOutcomeAccent.Info
    outcome.equals("No", ignoreCase = true) -> PolymarketOutcomeAccent.Error
    else -> PolymarketOutcomeAccent.Neutral
}

internal val PolymarketPosition.outcomeAccent: PolymarketOutcomeAccent
    get() = polymarketOutcomeAccent(outcomeIndex = outcomeIndex, outcome = outcome)

internal fun PolymarketOutcomeAccent.toBubbleVariant(): TangemMessageBubble.Variant = when (this) {
    PolymarketOutcomeAccent.Info -> TangemMessageBubble.Variant.Info
    PolymarketOutcomeAccent.Error -> TangemMessageBubble.Variant.Error
    PolymarketOutcomeAccent.Neutral -> TangemMessageBubble.Variant.Neutral
}

internal fun PolymarketOutcomeAccent.toBadgeStatus(): TangemBadge.Status = when (this) {
    PolymarketOutcomeAccent.Info -> TangemBadge.Status.Info
    PolymarketOutcomeAccent.Error -> TangemBadge.Status.Error
    PolymarketOutcomeAccent.Neutral -> TangemBadge.Status.Neutral
}
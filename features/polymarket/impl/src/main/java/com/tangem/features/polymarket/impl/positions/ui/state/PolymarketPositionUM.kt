package com.tangem.features.polymarket.impl.positions.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference

/**
 * Settlement styling is independent of the P&L direction. [result] replaces [currentValue] when present.
 * A null [onClick] leaves positions without a supported details or claim flow non-interactive.
 */
@Immutable
internal data class PolymarketPositionUM(
    val id: String,
    val title: TextReference,
    val iconUrl: String?,
    val outcomeLabel: TextReference,
    val outcomeVariant: TangemMessageBubble.Variant,
    val priceChange: TangemPriceChange.State?,
    val currentValue: TextReference?,
    val result: TangemBadgeUM?,
    val glowVariant: TangemGlowRing.Variant? = null,
    val onClick: (() -> Unit)? = null,
)
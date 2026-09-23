package com.tangem.features.polymarket.impl.history.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class PolymarketActivityRowUM(
    val id: String,
    val type: Type,
    val title: TextReference,
    val subtitle: PolymarketActivitySubtitleUM,
    val amount: TextReference,
    val iconUrl: String? = null,
    val onClick: (() -> Unit)? = null,
) {

    enum class Type {
        BOUGHT,
        SOLD,
        FUNDS_ADDED,
        WITHDRAWN,
        REDEEMED,
        LOST,
    }
}
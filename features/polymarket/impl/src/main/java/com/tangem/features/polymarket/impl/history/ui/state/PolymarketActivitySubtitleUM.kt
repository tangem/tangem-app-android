package com.tangem.features.polymarket.impl.history.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed interface PolymarketActivitySubtitleUM {

    data class Plain(val text: TextReference) : PolymarketActivitySubtitleUM

    data class Funding(
        val direction: Direction,
        val symbol: String,
        val icon: CurrencyIconState?,
        val account: Account? = null,
    ) : PolymarketActivitySubtitleUM

    data class Account(
        val name: TextReference,
        @DrawableRes val iconResId: Int,
        val iconBackgroundColor: Color,
    )

    enum class Direction { FROM, TO }
}
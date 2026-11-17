package com.tangem.features.polymarket.impl.fasttrading.banner.ui

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface PolymarketFastTradingBannerUM {

    val onActionClick: () -> Unit

    data class UnknownSigners(override val onActionClick: () -> Unit) : PolymarketFastTradingBannerUM

    data class Activate(
        override val onActionClick: () -> Unit,
        val onCloseClick: () -> Unit,
    ) : PolymarketFastTradingBannerUM

    data class Reactivate(
        override val onActionClick: () -> Unit,
        val onCloseClick: () -> Unit,
    ) : PolymarketFastTradingBannerUM

    data class Extend(
        val daysLeft: Int,
        override val onActionClick: () -> Unit,
        val onCloseClick: () -> Unit,
    ) : PolymarketFastTradingBannerUM
}
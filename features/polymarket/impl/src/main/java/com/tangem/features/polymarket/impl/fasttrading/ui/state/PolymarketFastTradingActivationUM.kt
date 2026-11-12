package com.tangem.features.polymarket.impl.fasttrading.ui.state

internal data class PolymarketFastTradingActivationUM(
    val isActivating: Boolean,
    val isFaqShown: Boolean,
    val onBackClick: () -> Unit,
    val onActivateClick: () -> Unit,
    val onFaqClick: () -> Unit,
    val onFaqDismiss: () -> Unit,
)
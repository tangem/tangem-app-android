package com.tangem.features.polymarket.impl.fasttrading.ui.state

import com.tangem.core.ui.extensions.TextReference

internal data class PolymarketFastTradingActivationUM(
    val buttonText: TextReference,
    val isHoldToConfirm: Boolean,
    val isActivating: Boolean,
    val isFaqShown: Boolean,
    val onBackClick: () -> Unit,
    val onActivateClick: () -> Unit,
    val onFaqClick: () -> Unit,
    val onFaqDismiss: () -> Unit,
)
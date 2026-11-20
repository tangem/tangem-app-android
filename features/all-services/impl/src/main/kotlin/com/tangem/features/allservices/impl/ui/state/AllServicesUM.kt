package com.tangem.features.allservices.impl.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal data class AllServicesUM(
    val onReferralClick: () -> Unit,
    val onBuyTangemClick: () -> Unit,
    val onCloseClick: () -> Unit,
)
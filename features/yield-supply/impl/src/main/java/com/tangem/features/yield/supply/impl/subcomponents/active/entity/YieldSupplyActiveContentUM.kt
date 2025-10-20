package com.tangem.features.yield.supply.impl.subcomponents.active.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference

internal data class YieldSupplyActiveContentUM(
    val totalEarnings: TextReference,
    val availableBalance: TextReference?,
    val providerTitle: TextReference,
    val subtitle: TextReference,
    val subtitleLink: TextReference,
    val notificationUM: NotificationUM?,
    val minAmount: TextReference?,
    val currentFee: TextReference?,
    val feeDescription: TextReference?,
    val apy: TextReference? = null,
    val isHighFee: Boolean = false,
)
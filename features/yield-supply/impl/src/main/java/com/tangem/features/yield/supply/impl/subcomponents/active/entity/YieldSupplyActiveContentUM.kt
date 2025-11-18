package com.tangem.features.yield.supply.impl.subcomponents.active.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class YieldSupplyActiveContentUM(
    val totalEarnings: TextReference,
    val availableBalance: TextReference?,
    val providerTitle: TextReference,
    val subtitle: TextReference,
    val subtitleLink: TextReference,
    val notifications: ImmutableList<NotificationUM>,
    val minAmount: TextReference?,
    val currentFee: TextReference?,
    val feeDescription: TextReference?,
    val minFeeDescription: TextReference?,
    val apy: TextReference? = null,
    val isHighFee: Boolean = false,
)
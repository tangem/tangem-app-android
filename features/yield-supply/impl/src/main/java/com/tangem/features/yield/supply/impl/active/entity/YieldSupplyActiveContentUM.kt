package com.tangem.features.yield.supply.impl.active.entity

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.yield.supply.impl.active.model.ApyBreakdown
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
    /** Null hides the APY info entry — without a campaign the rate has nothing to break down. */
    val apyBreakdown: ApyBreakdown? = null,
    val onApyInfoClick: () -> Unit = {},
    val isHighFee: Boolean = false,
    val boostText: TextReference? = null,
    val onBoostClick: () -> Unit = {},
)
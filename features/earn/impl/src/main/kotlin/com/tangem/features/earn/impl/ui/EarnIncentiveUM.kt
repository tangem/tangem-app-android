package com.tangem.features.earn.impl.ui

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.badge.TangemBadgeUM

@Immutable
internal data class EarnIncentiveUM(
    val badge: TangemBadgeUM,
    val onClick: () -> Unit,
)
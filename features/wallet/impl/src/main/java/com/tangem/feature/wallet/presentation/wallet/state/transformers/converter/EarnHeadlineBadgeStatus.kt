package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.api.overview.EarnHeadline

internal fun EarnHeadline.toBadgeStatus(): TangemBadge.Status = when {
    !isActive -> TangemBadge.Status.Neutral
    badgeAccent == EarnBadgeAccent.Info -> TangemBadge.Status.Info
    else -> TangemBadge.Status.Success
}
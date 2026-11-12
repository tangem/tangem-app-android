package com.tangem.domain.wallets.analytics

import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.wallet.UserWallet

fun List<UserWallet>?.toAnalyticsWalletsType(): AnalyticsParam.WalletsType? {
    if (this == null) return null
    val hasMobile = any { it is UserWallet.Hot }
    val hasCold = any { it is UserWallet.Cold }

    return when {
        hasMobile && hasCold -> AnalyticsParam.WalletsType.Multiple
        hasMobile -> AnalyticsParam.WalletsType.Mobile
        hasCold -> AnalyticsParam.WalletsType.Cold
        else -> null
    }
}
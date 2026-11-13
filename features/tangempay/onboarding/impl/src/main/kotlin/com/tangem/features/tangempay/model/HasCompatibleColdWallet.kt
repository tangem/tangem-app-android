package com.tangem.features.tangempay.model

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.isColdWallet
import com.tangem.domain.tangempay.TangemPayEligibilityManager

internal suspend fun hasCompatibleColdWallet(
    eligibilityManager: TangemPayEligibilityManager,
    userWalletsListRepository: UserWalletsListRepository,
    shouldExcludePaeraCustomers: Boolean,
): Boolean {
    val compatibleWalletsIds = eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers).toSet()
    return userWalletsListRepository.userWalletsSync().any { wallet ->
        wallet.isColdWallet && wallet.walletId in compatibleWalletsIds
    }
}
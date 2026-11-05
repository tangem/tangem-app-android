package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.isHotWalletsLimitReached
import com.tangem.domain.wallets.models.HotWalletAdditionRestriction

class GetHotWalletAdditionRestrictionUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val isMultipleHotWalletsEnabled: () -> Boolean,
) {

    suspend operator fun invoke(): HotWalletAdditionRestriction? {
        val userWallets = userWalletsListRepository.userWalletsSync()

        return when {
            isMultipleHotWalletsEnabled() && userWallets.isHotWalletsLimitReached() -> {
                HotWalletAdditionRestriction.LimitReached
            }
            else -> null
        }
    }
}
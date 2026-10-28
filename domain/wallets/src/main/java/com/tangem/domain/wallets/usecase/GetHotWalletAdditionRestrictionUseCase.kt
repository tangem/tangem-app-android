package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.isHotWalletsLimitReached
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.models.HotWalletAdditionRestriction

class GetHotWalletAdditionRestrictionUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val isMultipleHotWalletsEnabled: () -> Boolean,
) {

    suspend operator fun invoke(isSocialLogin: Boolean): HotWalletAdditionRestriction? {
        val userWallets = userWalletsListRepository.userWalletsSync()

        return when {
            isMultipleHotWalletsEnabled() && userWallets.isHotWalletsLimitReached() -> {
                HotWalletAdditionRestriction.LimitReached
            }
            isSocialLogin && userWallets.any { it is UserWallet.Hot && it.isSocialBackupCompleted } -> {
                HotWalletAdditionRestriction.SocialWalletAlreadyAdded
            }
            else -> null
        }
    }
}
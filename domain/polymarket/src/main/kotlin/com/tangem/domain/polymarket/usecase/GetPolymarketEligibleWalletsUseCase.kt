package com.tangem.domain.polymarket.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked

/**
 * Wallets a user may onboard to Predictions.
 *
 * A locked wallet is excluded because onboarding derives its owner address, which a locked wallet cannot
 * provide without an unlock the chooser has no way to ask for.
 */
class GetPolymarketEligibleWalletsUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    suspend operator fun invoke(): List<UserWallet> {
        userWalletsListRepository.load()

        return userWalletsListRepository.userWallets.value
            ?.filter { !it.isLocked }
            .orEmpty()
    }
}
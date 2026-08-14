package com.tangem.domain.polymarket.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency

/**
 * Wallets a user may onboard to Predictions.
 *
 * A locked wallet is excluded because onboarding derives its owner address, which a locked wallet cannot
 * provide without an unlock the chooser has no way to ask for.
 *
 * A single-currency wallet is excluded because onboarding requires the deposit chain in the portfolio and
 * such a wallet can never add it. Settling on one strands the user: the add-network sheet opens bound to
 * that wallet alone and every row in it is disabled.
 */
class GetPolymarketEligibleWalletsUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    suspend operator fun invoke(): List<UserWallet> {
        userWalletsListRepository.load()

        return userWalletsListRepository.userWallets.value
            ?.filter(::isEligible)
            .orEmpty()
    }

    /** Whether [userWallet] can be onboarded — the single rule both the auto-pick and the chooser obey. */
    fun isEligible(userWallet: UserWallet): Boolean = !userWallet.isLocked && userWallet.isMultiCurrency
}
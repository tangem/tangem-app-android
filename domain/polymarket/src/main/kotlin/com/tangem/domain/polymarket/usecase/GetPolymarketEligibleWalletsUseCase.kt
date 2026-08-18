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
 * A single-currency wallet is excluded because Note, Twins, Start2Coin and Visa cards have no business in
 * Predictions — each for reasons of its own, none of them about the deposit chain, which the hardened owner
 * path does not need in the portfolio.
 *
 * The rule is not a test of whether the wallet can derive that path: `isMultiCurrency` admits firmware below
 * [com.tangem.common.card.FirmwareVersion.HDWalletAvailable], which the derivation requires, so such a card
 * is offered here and fails after the tap. Closing that would mean filtering on
 * [com.tangem.domain.models.wallet.isTangemPayCompatible]; it is knowingly not done, because it would hide
 * the wallet instead of telling its owner why it cannot be used.
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
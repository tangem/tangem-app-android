package com.tangem.domain.wallets.usecase

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository

/**
 *  Helps to determine whether the tangem icon should be displayed on the buttons for interacting with the wallet.
 */
class ColdWalletAndHasMissedDerivationsUseCase(
    private val derivationsRepository: DerivationsRepository,
    private val userWalletUseCase: GetUserWalletUseCase,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        networksWithDerivationPath: Map<BackendId, String?>,
    ): Boolean {
        val userWallet = userWalletUseCase.invoke(userWalletId).getOrNull() ?: return false
        return userWallet is UserWallet.Cold &&
            derivationsRepository.hasMissedDerivations(userWalletId, networksWithDerivationPath)
    }
}
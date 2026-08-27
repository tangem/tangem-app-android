package com.tangem.domain.wallets.usecase

import arrow.core.getOrElse
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.utils.logging.TangemLogger

/**
 * Use case for resolving the target [UserWallet] for a deeplink: falls back to the currently
 * selected wallet when no [userWalletId] is given, and selects [userWalletId] as the active
 * wallet when it differs from the current selection.
 *
 * @property getUserWalletUseCase use case for getting a wallet by id
 * @property userWalletsListRepository repository for getting the currently selected wallet and selecting a wallet
 */
class ResolveAndSelectUserWalletUseCase(
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId?): UserWallet? {
        val selectedUserWallet = userWalletsListRepository.selectedUserWalletSync()
        val userWallet = when {
            userWalletId == null -> selectedUserWallet ?: run {
                TangemLogger.e("Error on getting user wallet: no selected wallet")
                return null
            }
            selectedUserWallet?.walletId == userWalletId -> selectedUserWallet
            else -> getUserWalletUseCase(userWalletId).getOrElse { error ->
                TangemLogger.e("Error on getting user wallet $userWalletId: $error")
                return null
            }
        }
        if (userWallet.isLocked) {
            TangemLogger.e("Error on getting user wallet ${userWallet.walletId}: wallet is locked")
            return null
        }

        if (userWalletId != null && selectedUserWallet?.walletId != userWalletId) {
            userWalletsListRepository.select(userWalletId).onLeft { error ->
                TangemLogger.e("Error on selecting user wallet $userWalletId: $error")
                return null
            }
        }

        return userWallet
    }
}
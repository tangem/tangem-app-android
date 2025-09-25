package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.core.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.legacy.UserWalletsListManager

class IsWalletAlreadySavedUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        canOverride: Boolean = false,
    ): Either<SaveWalletError, Boolean> {
        return if (useNewRepository) {
            either {
                userWalletsListRepository.userWalletsSync()
                    .any { it.walletId == userWallet.walletId }
            }
        } else {
            either {
                userWalletsListManager.userWalletsSync
                    .any { it.walletId == userWallet.walletId }
            }
        }
    }
}
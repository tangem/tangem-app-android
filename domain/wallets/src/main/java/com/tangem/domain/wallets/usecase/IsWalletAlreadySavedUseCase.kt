package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet

class IsWalletAlreadySavedUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        canOverride: Boolean = false,
    ): Either<SaveWalletError, Boolean> = either {
        userWalletsListRepository.userWalletsSync()
            .any { it.walletId == userWallet.walletId }
    }
}
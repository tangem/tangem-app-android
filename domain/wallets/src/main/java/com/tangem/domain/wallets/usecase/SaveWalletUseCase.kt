package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.repository.WalletsRepository

/**
 * Use case for saving user wallet
 *
[REDACTED_AUTHOR]
 */
class SaveWalletUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletsRepository: WalletsRepository,
) {

    suspend operator fun invoke(userWallet: UserWallet, canOverride: Boolean = false): Either<SaveWalletError, Unit> {
        return either {
            val newUserWallet =
                userWalletsListRepository.userWalletsSync().none { it.walletId == userWallet.walletId }
            val userWallet = userWalletsListRepository.saveWithoutLock(userWallet, canOverride).bind()

            if (newUserWallet) {
                when (userWallet) {
                    is UserWallet.Cold -> {
                        if (walletsRepository.useBiometricAuthentication()) {
                            userWalletsListRepository.setLock(
                                userWallet.walletId,
                                UserWalletsListRepository.LockMethod.Biometric,
                            )
                        } else {
                            Unit.right()
                        }
                    }
                    is UserWallet.Hot -> {
                        userWalletsListRepository.setLock(
                            userWallet.walletId,
                            UserWalletsListRepository.LockMethod.NoLock,
                        )
                    }
                }.mapLeft {
                    SaveWalletError.DataError(null)
                }.map {
                    userWalletsListRepository.select(userWallet.walletId)
                }.bind()
            }
        }
    }
}
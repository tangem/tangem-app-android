package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.core.wallets.error.SaveWalletError
import com.tangem.domain.models.wallet.UserWallet

/**
 * Use case for saving user wallet
 *
 * @property userWalletsListManager user wallets list manager
 *
[REDACTED_AUTHOR]
 */
class SaveWalletUseCase(private val userWalletsListManager: UserWalletsListManager) {

    suspend operator fun invoke(userWallet: UserWallet, canOverride: Boolean = false): Either<SaveWalletError, Unit> {
        return either {
            userWalletsListManager.save(userWallet, canOverride)
                .doOnSuccess { return Unit.right() }
                .doOnFailure {
                    return when (it) {
                        is UserWalletsListError.WalletAlreadySaved -> SaveWalletError.WalletAlreadySaved(
                            it.messageResId,
                        )
                        else -> SaveWalletError.DataError(it.messageResId)
                    }.left()
                }

            return Unit.right()
        }
    }
}
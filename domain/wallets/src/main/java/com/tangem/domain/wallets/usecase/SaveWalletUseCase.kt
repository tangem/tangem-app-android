package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.legacy.ensureUserWalletListManagerNotNull
import com.tangem.domain.wallets.models.SaveWalletError
import com.tangem.domain.wallets.models.UserWallet

/**
 * Use case for saving user wallet
 *
 * @property walletsStateHolder state holder for getting static initialized 'userWalletsListManager'
 *
[REDACTED_AUTHOR]
 */
class SaveWalletUseCase(private val walletsStateHolder: WalletsStateHolder) {

    suspend operator fun invoke(userWallet: UserWallet, canOverride: Boolean = false): Either<SaveWalletError, Unit> {
        return either {
            val userWalletsListManager = ensureUserWalletListManagerNotNull(
                walletsStateHolder = walletsStateHolder,
                raise = { SaveWalletError.DataError },
            )

            userWalletsListManager.save(userWallet, canOverride)
                .doOnSuccess { return Unit.right() }
                .doOnFailure {
                    return when (it) {
                        is UserWalletsListError.WalletAlreadySaved -> SaveWalletError.WalletAlreadySaved(
                            it.messageResId,
                        )
                        else -> SaveWalletError.DataError
                    }.left()
                }

            return Unit.right()
        }
    }
}
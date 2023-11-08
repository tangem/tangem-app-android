package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.common.doOnFailure
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.legacy.asLockable
import com.tangem.domain.wallets.models.UnlockWalletsError

/**
 * Unlock wallets use case
 *
 * @property walletsStateHolder wallets state holder
 *
[REDACTED_AUTHOR]
 */
class UnlockWalletsUseCase(private val walletsStateHolder: WalletsStateHolder) {

    suspend operator fun invoke(throwIfNotAllWalletsUnlocked: Boolean = false): Either<UnlockWalletsError, Unit> =
        either {
            val userWalletsListManager = ensureNotNull(
                value = walletsStateHolder.userWalletsListManager?.asLockable(),
                raise = {
                    UnlockWalletsError.DataError(
                        cause = IllegalStateException("The lockable user wallets list manager could not be found"),
                    )
                },
            )

            userWalletsListManager.unlock(throwIfNotAllWalletsUnlocked)
                .doOnFailure { error ->
                    val e = when (error) {
                        is UserWalletsListError.NoUserWalletSelected -> UnlockWalletsError.NoUserWalletSelected
                        is UserWalletsListError.NotAllUserWalletsUnlocked ->
                            UnlockWalletsError.NotAllUserWalletsUnlocked
                        else -> UnlockWalletsError.UnableToUnlockWallets
                    }

                    raise(e)
                }
        }
}
package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.common.doOnFailure
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.DeleteWalletError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.legacy.UserWalletsListManager

/**
 * Use case for deleting user wallet
 *
 * @property userWalletsListManager user wallets list manager
 *
[REDACTED_AUTHOR]
 */
class DeleteWalletUseCase(
    private val userWalletsListManager: UserWalletsListManager,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val useNewRepository: Boolean,
) {

    /**
     * Deletes user wallet with provided ID.
     *
     * @param userWalletId ID of user wallet to be deleted.
     *
     * @return [Either] with [com.tangem.domain.common.wallets.error.DeleteWalletError] or [Boolean] which indicates that there are still saved wallets.
     * */
    suspend operator fun invoke(userWalletId: UserWalletId): Either<DeleteWalletError, Boolean> {
        if (useNewRepository) {
            return userWalletsListRepository.delete(userWalletIds = listOf(userWalletId)).map {
                userWalletsListRepository.selectedUserWallet.value != null
            }
        }

        return either {
            userWalletsListManager.delete(userWalletIds = listOf(userWalletId))
                .doOnFailure {
                    raise(DeleteWalletError.UnableToDelete)
                }

            userWalletsListManager.hasUserWallets()
        }
    }
}
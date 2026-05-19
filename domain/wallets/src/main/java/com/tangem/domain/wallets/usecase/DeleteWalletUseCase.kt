package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.DeleteWalletError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.logging.TangemLogger

/**
 * Use case for deleting user wallet
 *
 * @property userWalletsListRepository repository for getting list of user wallets
 *
[REDACTED_AUTHOR]
 */
class DeleteWalletUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    /**
     * Deletes user wallet with provided ID.
     *
     * @param userWalletId ID of user wallet to be deleted.
     *
     * @return [Either] with [com.tangem.domain.common.wallets.error.DeleteWalletError] or [Boolean] which indicates that there are still saved wallets.
     * */
    suspend operator fun invoke(userWalletId: UserWalletId): Either<DeleteWalletError, Boolean> {
        return userWalletsListRepository.delete(userWalletIds = listOf(userWalletId))
            .map { userWalletsListRepository.selectedUserWallet.value != null }
            .onLeft {
                TangemLogger.e("Failed to delete wallet with id ${userWalletId.value}: $it")
            }
    }
}
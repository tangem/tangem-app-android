package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.DeleteWalletError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch

/**
 * Use case for deleting user wallet
 *
 * @property userWalletsListRepository repository for getting list of user wallets
 *
[REDACTED_AUTHOR]
 */
class DeleteWalletUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val userWalletDataCleaners: Set<UserWalletDataCleaner>,
    private val appCoroutineScope: AppCoroutineScope,
) {

    /**
     * Deletes user wallet with provided ID.
     *
     * @param userWalletId ID of user wallet to be deleted.
     *
     * @return [Either] with [com.tangem.domain.common.wallets.error.DeleteWalletError] or [Boolean] which indicates that there are still saved wallets.
     * */
    suspend operator fun invoke(userWalletId: UserWalletId): Either<DeleteWalletError, Boolean> {
        val userWalletIds = listOf(userWalletId)
        return userWalletsListRepository.delete(userWalletIds = userWalletIds)
            .onRight { clearWalletDataInBackground(userWalletIds) }
            .map { userWalletsListRepository.selectedUserWallet.value != null }
            .onLeft {
                TangemLogger.e("Failed to delete wallet with id ${userWalletId.stringValue}: $it")
            }
    }

    private fun clearWalletDataInBackground(userWalletIds: List<UserWalletId>) {
        if (userWalletDataCleaners.isEmpty()) return
        appCoroutineScope.launch {
            userWalletDataCleaners.forEach { cleaner ->
                runSuspendCatching { cleaner.clear(userWalletIds) }
                    .onFailure { TangemLogger.e("Failed to clear data for wallets $userWalletIds", it) }
            }
        }
    }
}
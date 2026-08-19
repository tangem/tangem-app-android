package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.card.DeleteSavedAccessCodesUseCase
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.DeleteWalletError
import com.tangem.domain.models.wallet.UserWallet
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
    private val deleteSavedAccessCodesUseCase: DeleteSavedAccessCodesUseCase,
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
        // The wallet is gone from the repository right after delete, so capture its cards up front
        val cardsIds = getWalletCardsIds(userWalletId)
        return userWalletsListRepository.delete(userWalletIds = userWalletIds)
            .onRight { clearWalletDataInBackground(userWalletIds, cardsIds) }
            .map { userWalletsListRepository.selectedUserWallet.value != null }
            .onLeft {
                TangemLogger.e("Failed to delete wallet with id ${userWalletId.stringValue}: $it")
            }
    }

    private suspend fun getWalletCardsIds(userWalletId: UserWalletId): Set<String> {
        val userWallet = runSuspendCatching { userWalletsListRepository.userWalletsSync() }
            .getOrNull()
            ?.find { it.walletId == userWalletId }

        return when (userWallet) {
            is UserWallet.Cold -> userWallet.cardsInWallet + userWallet.cardId
            is UserWallet.Hot, null -> emptySet()
        }
    }

    private fun clearWalletDataInBackground(userWalletIds: List<UserWalletId>, cardsIds: Set<String>) {
        if (userWalletDataCleaners.isEmpty() && cardsIds.isEmpty()) return
        appCoroutineScope.launch {
            if (cardsIds.isNotEmpty()) {
                // Stale saved codes make every next scan request biometrics even for cards without them
                runSuspendCatching { deleteSavedAccessCodesUseCase(cardsIds) }
                    .onFailure { TangemLogger.e("Failed to delete saved access codes for cards $cardsIds", it) }
            }

            userWalletDataCleaners.forEach { cleaner ->
                runSuspendCatching { cleaner.clear(userWalletIds) }
                    .onFailure { TangemLogger.e("Failed to clear data for wallets $userWalletIds", it) }
            }
        }
    }
}
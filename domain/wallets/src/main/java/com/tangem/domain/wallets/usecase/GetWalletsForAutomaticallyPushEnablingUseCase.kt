package com.tangem.domain.wallets.usecase

import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for retrieving wallets where automatically enabling push notifications was not applied.
 * * This use case filters out wallets that have already had push notifications automatically enabled
 * from the complete list of user wallets, returning only those wallets that still need to have
 * push notifications automatically enabled.
 * * @property userWalletsListManager Manager for user wallets list operations
 * @property dispatchers Coroutine dispatcher provider for background operations
 */
class GetWalletsForAutomaticallyPushEnablingUseCase @Inject constructor(
    private val userWalletsListManager: UserWalletsListManager,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(walletsListWherePushWasEnabled: List<UserWalletId>): List<UserWalletId> =
        withContext(dispatchers.default) {
            val allLocalWallets = userWalletsListManager.userWalletsSync.map { it.walletId }
            allLocalWallets - walletsListWherePushWasEnabled.toSet()
        }
}
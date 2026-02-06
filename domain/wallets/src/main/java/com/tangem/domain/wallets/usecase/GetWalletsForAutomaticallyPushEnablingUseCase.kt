package com.tangem.domain.wallets.usecase

import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

/**
 * Use case for retrieving wallets where automatically enabling push notifications was not applied.
 * * This use case filters out wallets that have already had push notifications automatically enabled
 * from the complete list of user wallets, returning only those wallets that still need to have
 * push notifications automatically enabled.
 * @property userWalletsListRepository Repository for user wallets list operations
 * @property dispatchers Coroutine dispatcher provider for background operations
 */
class GetWalletsForAutomaticallyPushEnablingUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(walletsListWherePushWasEnabled: List<UserWalletId>): List<UserWalletId> =
        withContext(dispatchers.default) {
            val allLocalWallets = userWalletsListRepository.userWalletsSync().map { it.walletId }
            allLocalWallets - walletsListWherePushWasEnabled.toSet()
        }
}
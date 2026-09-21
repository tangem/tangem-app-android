package com.tangem.data.collectibles.account

import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.isAccountCreated
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Total by design: every operation degrades to "nothing recorded" instead of throwing. The record only ever saves
 * a chain read, so losing it costs one — while a throw here would surface as a crash on opening the feature, or as
 * a failed report for an onboarding that actually succeeded.
 */
internal class DefaultCollectiblesAccountRepository @Inject constructor(
    private val collectiblesAccountStore: CollectiblesAccountStore,
) : CollectiblesAccountRepository {

    override fun getStatus(userWalletId: UserWalletId): Flow<CollectiblesAccountStatusValue?> =
        collectiblesAccountStore.get(userWalletId)

    override suspend fun getStatusSync(userWalletId: UserWalletId): CollectiblesAccountStatusValue? =
        runSuspendCatching { collectiblesAccountStore.getSyncOrNull(userWalletId) }
            .onFailure { TangemLogger.e("Failed to read the collectibles account of $userWalletId", it) }
            .getOrNull()

    override suspend fun saveAccount(userWalletId: UserWalletId, status: CollectiblesAccountStatusValue) {
        // TODO([REDACTED_TASK_KEY]): a wallet with nothing recorded is onboarding for the first time, so this is where the
        //  account is registered with the backend — `PUT /accounts` with a `collectibles` record. Until the
        //  backend knows the type, the account lives on this device only.
        if (!getStatusSync(userWalletId).isAccountCreated) {
            TangemLogger.d(messageString = "Collectibles account of $userWalletId created")
        }

        runSuspendCatching { collectiblesAccountStore.store(userWalletId = userWalletId, value = status) }
            .onFailure { TangemLogger.e("Failed to record the collectibles account of $userWalletId", it) }
    }

    override suspend fun clearAccount(userWalletId: UserWalletId) {
        runSuspendCatching { collectiblesAccountStore.clear(userWalletId) }
            .onFailure { TangemLogger.e("Failed to clear the collectibles account of $userWalletId", it) }
    }
}
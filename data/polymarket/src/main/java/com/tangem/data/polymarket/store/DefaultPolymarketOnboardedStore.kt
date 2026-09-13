package com.tangem.data.polymarket.store

import androidx.datastore.core.DataStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first

/**
 * Total by design: every operation degrades to "not recorded" instead of throwing. The record is only ever an
 * optimisation over asking the backend, so losing it costs a network call, while a throw here would surface as
 * a crash on opening the feature or as a failed report for an onboarding run that actually succeeded.
 */
internal class DefaultPolymarketOnboardedStore(
    private val dataStore: DataStore<Set<String>>,
) : PolymarketOnboardedStore {

    override suspend fun isOnboarded(userWalletId: UserWalletId): Boolean =
        runSuspendCatching { userWalletId.stringValue in dataStore.data.first() }
            .onFailure { TangemLogger.e("Failed to read the onboarded record of $userWalletId", it) }
            .getOrDefault(false)

    override suspend fun markOnboarded(userWalletId: UserWalletId) {
        edit(userWalletId) { stored -> stored + userWalletId.stringValue }
    }

    override suspend fun clear(userWalletId: UserWalletId) {
        edit(userWalletId) { stored -> stored - userWalletId.stringValue }
    }

    private suspend fun edit(userWalletId: UserWalletId, update: (Set<String>) -> Set<String>) {
        runSuspendCatching { dataStore.updateData(update) }
            .onFailure { TangemLogger.e("Failed to update the onboarded record of $userWalletId", it) }
    }
}
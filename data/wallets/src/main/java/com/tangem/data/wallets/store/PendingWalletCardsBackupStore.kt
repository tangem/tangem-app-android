package com.tangem.data.wallets.store

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first

internal typealias PendingWalletCardsBackups = List<PendingWalletCardsBackup>

/**
 * Durable queue of cards-backup reports awaiting acceptance by the backend.
 *
 * A report is enqueued before it is sent, so it survives the request failing, the process being killed
 * mid-flight, or the device being offline for the whole of onboarding. Entries are kept in the order they
 * were made — the backend builds its change history out of them, so replaying them out of order would
 * misrepresent how the backup actually progressed.
 */
internal class PendingWalletCardsBackupStore(
    private val dataStore: DataStore<PendingWalletCardsBackups>,
) {

    suspend fun enqueue(entry: PendingWalletCardsBackup) {
        dataStore.updateData { pending -> (pending + entry).takeLast(MAX_PENDING) }
    }

    suspend fun getAll(): PendingWalletCardsBackups = dataStore.data.first()

    suspend fun remove(id: String) {
        dataStore.updateData { pending -> pending.filterNot { it.id == id } }
    }

    private companion object {

        /**
         * Onboarding makes at most a handful of reports per wallet, so the queue only grows this far if the
         * device stays offline across many activations. Past that the oldest entries are dropped.
         */
        const val MAX_PENDING = 50
    }
}
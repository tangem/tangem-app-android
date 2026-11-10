package com.tangem.data.collectibles.store

import androidx.datastore.core.DataStore
import com.tangem.domain.collectibles.repository.CollectiblesDismissedTxRepository
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val MAX_STORED_TX_IDS = 100

internal class DefaultCollectiblesDismissedTxRepository(
    private val store: DataStore<DismissedTxsDTO>,
) : CollectiblesDismissedTxRepository {

    override fun getDismissedTxIds(): Flow<Set<String>> = store.data
        .map { dto -> dto.txIds.toSet() }
        .catch { error ->
            TangemLogger.e("Unable to read the dismissed collectible packs", error)
            emit(emptySet())
        }

    override suspend fun saveAsDismissed(txId: String) {
        runSuspendCatching {
            store.updateData { dto -> dto.copy(txIds = (dto.txIds - txId + txId).takeLast(MAX_STORED_TX_IDS)) }
        }.onFailure { error ->
            TangemLogger.e("Unable to store a dismissed collectible pack", error)
        }
    }
}
package com.tangem.data.polymarket.store

import androidx.datastore.core.DataStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.polymarket.entity.PredictionWithdrawalRecordDTO
import com.tangem.data.polymarket.entity.toDTO
import com.tangem.data.polymarket.entity.toDomain
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PredictionWithdrawalStore
import com.tangem.domain.polymarket.model.PredictionWithdrawalRecord
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal typealias WalletIdWithWithdrawalRecord = Map<String, PredictionWithdrawalRecord>
internal typealias WalletIdWithWithdrawalRecordDTO = Map<String, PredictionWithdrawalRecordDTO>

/**
 * @property runtimeStore         runtime store for fast in-memory access
 * @property persistenceDataStore persistence store, the copy that outlives the process
 */
internal class DefaultPredictionWithdrawalStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithWithdrawalRecord>,
    private val persistenceDataStore: DataStore<WalletIdWithWithdrawalRecordDTO>,
    scope: AppCoroutineScope,
) : PredictionWithdrawalStore {

    private val logger = TangemLogger.withTag(TAG)

    private val preloaded = CompletableDeferred<Unit>()

    private var preloadFailure: Throwable? = null

    init {
        scope.launch {
            try {
                runSuspendCatching { persistenceDataStore.data.first() }
                    .onSuccess { cached -> runtimeStore.store(cached.mapValues { (_, dto) -> dto.toDomain() }) }
                    .onFailure { cause ->
                        logger.e("Failed to read the stored prediction withdrawals", cause)
                        preloadFailure = cause
                    }
            } finally {
                preloaded.complete(Unit)
            }
        }
    }

    override suspend fun save(userWalletId: UserWalletId, record: PredictionWithdrawalRecord) {
        preloaded.await()

        persistenceDataStore.updateData { stored -> stored + (userWalletId.stringValue to record.toDTO()) }
        runtimeStore.update(default = emptyMap()) { stored -> stored + (userWalletId.stringValue to record) }
    }

    /** @throws PredictionWithdrawalStoreUnreadableException when the persisted records could not be read */
    override suspend fun get(userWalletId: UserWalletId): PredictionWithdrawalRecord? {
        preloaded.await()
        preloadFailure?.let { throw PredictionWithdrawalStoreUnreadableException(it) }

        return runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)
    }

    override suspend fun clear(userWalletId: UserWalletId) {
        preloaded.await()

        coroutineScope {
            launch { runtimeStore.update(default = emptyMap()) { it - userWalletId.stringValue } }
            launch { persistenceDataStore.updateData { it - userWalletId.stringValue } }
        }
    }

    private companion object {
        const val TAG = "PredictionWithdrawalStore"
    }
}

internal class PredictionWithdrawalStoreUnreadableException(
    cause: Throwable,
) : IllegalStateException("The stored prediction withdrawals could not be read", cause)
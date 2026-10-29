package com.tangem.data.polymarket.store

import androidx.datastore.core.DataStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.polymarket.entity.PredictionPositionsValueDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PredictionPositionsValue
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal typealias WalletIdWithPositionsValue = Map<String, PredictionPositionsValue>
internal typealias WalletIdWithPositionsValueDTO = Map<String, PredictionPositionsValueDTO>

internal class PredictionPositionsValueStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithPositionsValue>,
    private val persistenceDataStore: DataStore<WalletIdWithPositionsValueDTO>,
    scope: AppCoroutineScope,
) {

    private val preloaded = CompletableDeferred<Unit>()

    private val mutex = Mutex()

    init {
        scope.launch {
            try {
                val cached = runSuspendCatching { persistenceDataStore.data.first() }
                    .onFailure { logger.e("Failed to read the cached prediction positions values", it) }
                    .getOrDefault(emptyMap())

                runtimeStore.store(
                    cached.mapValues { (_, dto) ->
                        PredictionPositionsValue.Known(amount = dto.amount, source = StatusSource.CACHE)
                    },
                )
            } finally {
                preloaded.complete(Unit)
            }
        }
    }

    fun get(userWalletId: UserWalletId): Flow<PredictionPositionsValue> {
        return runtimeStore.get()
            .onStart { emit(runtimeStore.getSyncOrDefault(emptyMap())) }
            .map { values -> values[userWalletId.stringValue] ?: PredictionPositionsValue.Unknown }
            .distinctUntilChanged()
    }

    suspend fun getSyncOrNull(userWalletId: UserWalletId): PredictionPositionsValue? {
        return runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)
    }

    suspend fun store(userWalletId: UserWalletId, value: PredictionPositionsValue.Known) {
        preloaded.await()

        mutex.withLock {
            runtimeStore.update(default = emptyMap()) { it + (userWalletId.stringValue to value) }
            persistenceDataStore.updateData { stored ->
                stored + (userWalletId.stringValue to PredictionPositionsValueDTO(amount = value.amount))
            }
        }
    }

    suspend fun markUnrefreshed(userWalletId: UserWalletId) {
        preloaded.await()

        runtimeStore.update(default = emptyMap()) { stored ->
            val value = when (val current = stored[userWalletId.stringValue]) {
                is PredictionPositionsValue.Known -> current.copy(source = StatusSource.ONLY_CACHE)
                PredictionPositionsValue.Unknown,
                PredictionPositionsValue.Unavailable,
                null,
                -> PredictionPositionsValue.Unavailable
            }

            stored + (userWalletId.stringValue to value)
        }
    }

    suspend fun clear(userWalletId: UserWalletId) {
        preloaded.await()

        mutex.withLock {
            runtimeStore.update(default = emptyMap()) { it - userWalletId.stringValue }
            persistenceDataStore.updateData { it - userWalletId.stringValue }
        }
    }

    private companion object {
        val logger = TangemLogger.withTag("PredictionPositionsValueStore")
    }
}
package com.tangem.data.polymarket.store

import com.tangem.common.services.secure.SecureStorage
import com.tangem.data.polymarket.converter.PolymarketApiCredentialsConverter
import com.tangem.data.polymarket.entity.PolymarketApiCredentialsDTO
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Stores the Polymarket L2 API credentials in [SecureStorage] as JSON, one entry per wallet.
 *
 * The underlying `AndroidSecureStorageV2` encrypts the payload with an AES-256-GCM key held in the
 * Android Keystore, so the credentials are unreadable outside this device.
 */
internal class DefaultPolymarketCredentialsStore(
    private val secureStorage: SecureStorage,
    private val json: Json,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketCredentialsStore {

    private val mutex = Mutex()

    private val loaded = MutableStateFlow<Map<UserWalletId, Loaded>>(value = emptyMap())

    override suspend fun store(userWalletId: UserWalletId, credentials: PolymarketApiCredentials) {
        mutex.withLock {
            withContext(dispatchers.io) {
                val dto = PolymarketApiCredentialsConverter.convert(credentials)
                val payload = json.encodeToString(PolymarketApiCredentialsDTO.serializer(), dto)

                secureStorage.store(createKey(userWalletId), payload)
            }
            loaded.update { it + (userWalletId to Loaded(credentials)) }
        }
    }

    override suspend fun get(userWalletId: UserWalletId): PolymarketApiCredentials? =
        mutex.withLock { loadLocked(userWalletId) }

    override suspend fun clear(userWalletId: UserWalletId) {
        mutex.withLock { deleteLocked(userWalletId) }
    }

    override suspend fun clearIfCurrent(userWalletId: UserWalletId, rejected: PolymarketApiCredentials) {
        mutex.withLock {
            if (loaded.value[userWalletId]?.credentials !== rejected) return

            deleteLocked(userWalletId)
        }
    }

    override fun hasCredentials(userWalletId: UserWalletId): Flow<Boolean> = loaded
        .onStart { get(userWalletId) }
        .filter { userWalletId in it }
        .map { it[userWalletId]?.credentials != null }
        .distinctUntilChanged()
        .catch { error -> TangemLogger.e("Failed to observe the API credentials of $userWalletId", error) }

    private suspend fun loadLocked(userWalletId: UserWalletId): PolymarketApiCredentials? {
        loaded.value[userWalletId]?.let { return it.credentials }

        val credentials = read(userWalletId)
        loaded.update { it + (userWalletId to Loaded(credentials)) }
        return credentials
    }

    private suspend fun read(userWalletId: UserWalletId): PolymarketApiCredentials? = withContext(dispatchers.io) {
        val key = createKey(userWalletId)
        val payload = secureStorage.getAsString(key) ?: return@withContext null

        try {
            val dto = json.decodeFromString(PolymarketApiCredentialsDTO.serializer(), payload)
            PolymarketApiCredentialsConverter.convertBack(dto)
        } catch (e: SerializationException) {
            TangemLogger.e("Failed to decode the stored API credentials; clearing them")
            secureStorage.delete(key)
            null
        }
    }

    private suspend fun deleteLocked(userWalletId: UserWalletId) {
        withContext(dispatchers.io) {
            secureStorage.delete(createKey(userWalletId))
        }
        loaded.update { it + (userWalletId to Loaded(credentials = null)) }
    }

    private fun createKey(userWalletId: UserWalletId): String = KEY_PREFIX + userWalletId.stringValue

    private class Loaded(val credentials: PolymarketApiCredentials?)

    private companion object {
        const val KEY_PREFIX = "polymarket_api_credentials_"
    }
}
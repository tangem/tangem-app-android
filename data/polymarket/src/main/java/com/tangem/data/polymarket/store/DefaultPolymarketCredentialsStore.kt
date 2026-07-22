package com.tangem.data.polymarket.store

import com.tangem.common.services.secure.SecureStorage
import com.tangem.data.polymarket.converter.toDomain
import com.tangem.data.polymarket.converter.toDto
import com.tangem.data.polymarket.entity.PolymarketApiCredentialsDTO
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Stores the Polymarket L2 API credentials in [SecureStorage] as JSON, one entry per owner EOA address.
 *
 * The underlying `AndroidSecureStorageV2` encrypts the payload with an AES-256-GCM key held in the
 * Android Keystore, so the credentials are unreadable outside this device.
 */
internal class DefaultPolymarketCredentialsStore(
    private val secureStorage: SecureStorage,
    private val json: Json,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketCredentialsStore {

    override suspend fun store(ownerAddress: String, credentials: PolymarketApiCredentials) {
        withContext(dispatchers.io) {
            val payload = json.encodeToString(PolymarketApiCredentialsDTO.serializer(), credentials.toDto())

            secureStorage.store(createKey(ownerAddress), payload)
        }
    }

    override suspend fun get(ownerAddress: String): PolymarketApiCredentials? = withContext(dispatchers.io) {
        val key = createKey(ownerAddress)
        val payload = secureStorage.getAsString(key) ?: return@withContext null

        try {
            json.decodeFromString(PolymarketApiCredentialsDTO.serializer(), payload).toDomain()
        } catch (e: SerializationException) {
            TangemLogger.e("Failed to decode Polymarket API credentials; clearing storage")
            secureStorage.delete(key)
            null
        }
    }

    override suspend fun clear(ownerAddress: String) {
        withContext(dispatchers.io) {
            secureStorage.delete(createKey(ownerAddress))
        }
    }

    private fun createKey(ownerAddress: String): String = KEY_PREFIX + ownerAddress.lowercase()

    private companion object {
        const val KEY_PREFIX = "polymarket_api_credentials_"
    }
}
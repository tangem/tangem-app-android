package com.tangem.data.polymarket.store

import com.tangem.common.services.secure.SecureStorage
import com.tangem.data.polymarket.converter.PolymarketApiCredentialsConverter
import com.tangem.data.polymarket.entity.PolymarketApiCredentialsDTO
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
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

    override suspend fun store(userWalletId: UserWalletId, credentials: PolymarketApiCredentials) {
        withContext(dispatchers.io) {
            val dto = PolymarketApiCredentialsConverter.convert(credentials)
            val payload = json.encodeToString(PolymarketApiCredentialsDTO.serializer(), dto)

            secureStorage.store(createKey(userWalletId), payload)
        }
    }

    override suspend fun get(userWalletId: UserWalletId): PolymarketApiCredentials? = withContext(dispatchers.io) {
        val key = createKey(userWalletId)
        val payload = secureStorage.getAsString(key) ?: return@withContext null

        try {
            val dto = json.decodeFromString(PolymarketApiCredentialsDTO.serializer(), payload)
            PolymarketApiCredentialsConverter.convertBack(dto)
        } catch (e: SerializationException) {
            TangemLogger.e("Failed to decode Polymarket API credentials; clearing storage")
            secureStorage.delete(key)
            null
        }
    }

    override suspend fun clear(userWalletId: UserWalletId) {
        withContext(dispatchers.io) {
            secureStorage.delete(createKey(userWalletId))
        }
    }

    private fun createKey(userWalletId: UserWalletId): String = KEY_PREFIX + userWalletId.stringValue

    private companion object {
        const val KEY_PREFIX = "polymarket_api_credentials_"
    }
}
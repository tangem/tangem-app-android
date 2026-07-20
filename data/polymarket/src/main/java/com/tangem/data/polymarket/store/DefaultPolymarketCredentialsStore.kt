package com.tangem.data.polymarket.store

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.tangem.common.services.secure.SecureStorage
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext

/**
 * Stores the Polymarket L2 API credentials in [SecureStorage] as JSON, one entry per owner EOA address.
 *
 * The underlying `AndroidSecureStorageV2` encrypts the payload with an AES-256-GCM key held in the
 * Android Keystore, so the credentials are unreadable outside this device.
 */
internal class DefaultPolymarketCredentialsStore(
    private val secureStorage: SecureStorage,
    moshi: Moshi,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketCredentialsStore {

    private val adapter: JsonAdapter<PolymarketApiCredentials> by lazy {
        moshi.adapter(PolymarketApiCredentials::class.java)
    }

    override suspend fun store(ownerAddress: String, credentials: PolymarketApiCredentials) {
        withContext(dispatchers.io) {
            secureStorage.store(createKey(ownerAddress), adapter.toJson(credentials))
        }
    }

    override suspend fun get(ownerAddress: String): PolymarketApiCredentials? = withContext(dispatchers.io) {
        val key = createKey(ownerAddress)
        val payload = secureStorage.getAsString(key) ?: return@withContext null

        try {
            adapter.fromJson(payload)
        } catch (e: Exception) {
            TangemLogger.e("Failed to decode Polymarket API credentials; clearing storage", e)
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
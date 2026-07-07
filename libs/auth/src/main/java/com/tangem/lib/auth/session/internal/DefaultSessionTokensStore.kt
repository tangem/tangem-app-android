package com.tangem.lib.auth.session.internal

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.tangem.common.services.secure.SecureStorage
import com.tangem.datasource.api.auth.models.response.TokenApiResponse
import com.tangem.lib.auth.session.SessionTokens
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext

/**
 * Stores tokens in [SecureStorage] using the wire-format [TokenApiResponse] as the on-disk
 * DTO — keeps the storage layout in lockstep with the Auth Service contract while
 * isolating the [SessionTokens] domain model from serialization concerns.
 *
 * The underlying `AndroidSecureStorageV2` wraps `SharedPreferences` with an AES-256-GCM
 * cipher whose key lives in AndroidKeystore, so token blobs are encrypted at rest and only
 * decryptable on this device.
 */
internal class DefaultSessionTokensStore(
    private val storage: SecureStorage,
    private val moshi: Moshi,
    private val dispatchers: CoroutineDispatcherProvider,
) : SessionTokensStore {

    private val adapter: JsonAdapter<TokenApiResponse> by lazy {
        moshi.adapter(TokenApiResponse::class.java)
    }

    override suspend fun get(): Option<SessionTokens> = withContext(dispatchers.io) {
        val payload = storage.getAsString(KEY) ?: return@withContext None
        try {
            val dto = adapter.fromJson(payload) ?: return@withContext None
            Some(SessionTokensConverter.convertBack(dto))
        } catch (e: Exception) {
            TangemLogger.e("Failed to decode session tokens; clearing storage", e)
            storage.delete(KEY)
            None
        }
    }

    override suspend fun save(tokens: SessionTokens) {
        withContext(dispatchers.io) {
            storage.store(KEY, adapter.toJson(SessionTokensConverter.convert(tokens)))
        }
    }

    override suspend fun clear() {
        withContext(dispatchers.io) {
            storage.delete(KEY)
        }
    }

    private companion object {
        const val KEY = "session_tokens"
    }
}
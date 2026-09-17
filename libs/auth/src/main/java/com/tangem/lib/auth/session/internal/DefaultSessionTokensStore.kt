package com.tangem.lib.auth.session.internal

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.tangem.common.services.secure.SecureStorage
import com.tangem.lib.auth.api.models.response.TokenApiResponse
import com.tangem.lib.auth.session.SessionTokens
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val issuedAt = AccessTokenIssuedAt(moshi)

    // Makes the compare-and-write in [save] atomic against a concurrent save/clear. Guards local
    // storage only — never held across a network call — so it cannot deadlock with the locks the
    // registration paths take.
    private val writeMutex = Mutex()

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
            writeMutex.withLock {
                if (isSuperseded(tokens)) {
                    // A response that lost the race — e.g. a wallet registration reissuing the session
                    // while a 401 elsewhere already refreshed it. Keeping it would leave the app
                    // authenticating with credentials the service has already replaced.
                    TangemLogger.i("Discarded a session token set issued before the stored one")
                    return@withLock
                }
                storage.store(KEY, adapter.toJson(SessionTokensConverter.convert(tokens)))
            }
        }
    }

    /**
     * Unknown issuance on either side means the two cannot be ordered — the incoming set is then kept,
     * so an unreadable claim can never strand the app on credentials it can no longer refresh.
     */
    private suspend fun isSuperseded(incoming: SessionTokens): Boolean {
        val stored = get().getOrNull() ?: return false
        val storedIssuedAt = issuedAt.of(stored.accessToken) ?: return false
        val incomingIssuedAt = issuedAt.of(incoming.accessToken) ?: return false

        return incomingIssuedAt < storedIssuedAt
    }

    override suspend fun clear() {
        withContext(dispatchers.io) {
            writeMutex.withLock {
                storage.delete(KEY)
            }
        }
    }

    private companion object {
        const val KEY = "session_tokens"
    }
}
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

    // Serialises every mutation — the compare-and-write in [save], the clearing of a corrupt entry
    // and [clear] — so none of them can act on a value another one has already replaced. Guards local
    // storage only, never held across a network call, so it cannot deadlock with the locks the
    // registration paths take.
    private val writeMutex = Mutex()

    override suspend fun get(): Option<SessionTokens> = withContext(dispatchers.io) {
        val payload = storage.getAsString(KEY) ?: return@withContext None
        val tokens = decode(payload)
        if (tokens == null) {
            dropIfStill(payload)
            return@withContext None
        }

        Some(tokens)
    }

    override suspend fun save(tokens: SessionTokens): SessionTokens = withContext(dispatchers.io) {
        writeMutex.withLock {
            // read straight from storage rather than through get(): that one takes the same
            // non-reentrant mutex to clear a corrupt entry, and a corrupt entry here simply means
            // there is nothing to compare against
            val stored = storage.getAsString(KEY)?.let(::decode)
            if (stored != null && stored.supersedes(tokens)) {
                // A response that lost the race — e.g. a wallet registration reissuing the session
                // while a 401 elsewhere already refreshed it. Keeping it would leave the app
                // authenticating with credentials the service has already replaced.
                TangemLogger.i("Discarded a superseded session token set")
                return@withLock stored
            }
            storage.store(KEY, adapter.toJson(SessionTokensConverter.convert(tokens)))
            tokens
        }
    }

    /**
     * Whether the stored set wins over [incoming], i.e. was issued later.
     *
     * Unknown issuance on either side, or the very same second on both, leaves the two
     * indistinguishable, and [incoming] — the answer that arrived last — is taken. See
     * [SessionTokensStore.save] for what that costs.
     */
    private fun SessionTokens.supersedes(incoming: SessionTokens): Boolean {
        val storedIssuedAt = issuedAt.of(accessToken) ?: return false
        val incomingIssuedAt = issuedAt.of(incoming.accessToken) ?: return false

        return incomingIssuedAt < storedIssuedAt
    }

    private fun decode(payload: String): SessionTokens? = try {
        adapter.fromJson(payload)?.let(SessionTokensConverter::convertBack)
    } catch (e: Exception) {
        TangemLogger.e("Failed to decode session tokens", e)
        null
    }

    /**
     * Drops the entry only while it still holds the exact [payload] that failed to decode: a save may
     * have replaced it with a valid set in the meantime, and deleting that would log the user out.
     */
    private suspend fun dropIfStill(payload: String) {
        writeMutex.withLock {
            if (storage.getAsString(KEY) == payload) {
                TangemLogger.e("Clearing the corrupted session tokens entry")
                storage.delete(KEY)
            }
        }
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
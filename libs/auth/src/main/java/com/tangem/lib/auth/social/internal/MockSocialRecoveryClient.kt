package com.tangem.lib.auth.social.internal

import android.util.Base64
import androidx.datastore.preferences.core.stringPreferencesKey
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.lib.auth.social.SignInNonce
import com.tangem.lib.auth.social.SocialRecoveryClient
import com.tangem.lib.auth.social.SocialRecoveryError
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.SecureRandom

/**
 * On-device stand-in for the social recovery backend, so the flow can be exercised end to end before the
 * service ships.
 *
 * The rules the flow depends on are real: the one-to-one binding, the idempotency of a repeated
 * registration, and the wallet signature taken from the caller's [WalletSigner].
 *
 * It deliberately does not verify the id token (no provider keys on device — the identity is read
 * straight out of the `iss`/`sub` claims) or the wallet signature, and it keeps the recovery key in
 * plain preferences: the transport encryption belongs to the real client, and a key sealed to a
 * per-process pair would stop opening after the first app restart.
 */
internal class MockSocialRecoveryClient(
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : SocialRecoveryClient {

    private val random = SecureRandom()
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun requestSignInNonce(): Either<SocialRecoveryError, SignInNonce> = randomBytes()
        .toBase64Url()
        .let { SignInNonce(forProvider = it.sha256Base64(), forRequest = it) }
        .right()

    override suspend fun recoverKey(nonce: String, providerIdToken: String): Either<SocialRecoveryError, ByteArray> =
        withContext(dispatchers.io) {
            val identity = providerIdToken.toProviderIdentity() ?: return@withContext malformedIdToken()

            mutex.withLock {
                val binding = readBindings()[identity] ?: return@withLock SocialRecoveryError.BindingNotFound.left()

                Either
                    .catch { Base64.decode(binding.recoveryKey, Base64.NO_WRAP) }
                    .mapLeft { error ->
                        TangemLogger.e("Mock social backend failed to read the stored recovery key", error)
                        SocialRecoveryError.DecryptionFailed(error)
                    }
            }
        }

    override suspend fun registerBinding(
        walletId: String,
        providerIdToken: String,
        recoveryKey: ByteArray,
        signer: WalletSigner,
    ): Either<SocialRecoveryError, Unit> = withContext(dispatchers.io) {
        val identity = providerIdToken.toProviderIdentity() ?: return@withContext malformedIdToken()

        try {
            signer.sign(nonceBytes = randomBytes())
        } catch (e: Exception) {
            TangemLogger.e("Failed to sign the mock social wallet registration payload", e)
            return@withContext SocialRecoveryError.SigningFailed(e).left()
        }

        mutex.withLock {
            val bindings = readBindings()
            val walletOfIdentity = bindings[identity]?.walletId
            val identityOfWallet = bindings.entries.firstOrNull { it.value.walletId == walletId }?.key

            val isConflict = walletOfIdentity != null && walletOfIdentity != walletId ||
                identityOfWallet != null && identityOfWallet != identity
            if (isConflict) return@withLock SocialRecoveryError.BindingConflict.left()

            writeBindings(bindings + (identity to StoredBinding.of(walletId, recoveryKey)))
            Unit.right()
        }
    }

    override suspend fun unbindWallet(walletId: String): Either<SocialRecoveryError, Unit> =
        withContext(dispatchers.io) {
            mutex.withLock {
                writeBindings(readBindings().filterValues { it.walletId != walletId })
                Unit.right()
            }
        }

    private suspend fun readBindings(): Map<String, StoredBinding> {
        val raw = appPreferencesStore.getSyncOrDefault(BINDINGS_KEY, default = "")
        if (raw.isEmpty()) return emptyMap()
        return runCatching { json.decodeFromString<Map<String, StoredBinding>>(raw) }.getOrDefault(emptyMap())
    }

    private suspend fun writeBindings(bindings: Map<String, StoredBinding>) {
        appPreferencesStore.store(BINDINGS_KEY, json.encodeToString(bindings))
    }

    /** `iss` + `sub` of the id token, never the email */
    private fun String.toProviderIdentity(): String? {
        val payload = split(".").getOrNull(index = 1) ?: return null
        val claims = runCatching {
            val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP).decodeToString()
            json.decodeFromString<IdTokenClaims>(decoded)
        }.getOrNull() ?: return null

        val issuer = claims.issuer ?: return null
        val subject = claims.subject ?: return null
        return "$issuer|$subject"
    }

    private fun <T> malformedIdToken(): Either<SocialRecoveryError, T> =
        SocialRecoveryError.Api(AuthError.BadRequest(problem = null)).left()

    private fun randomBytes(): ByteArray = ByteArray(NONCE_SIZE_BYTES).also(random::nextBytes)

    private fun ByteArray.toBase64Url(): String = Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_WRAP)

    @Serializable
    private data class IdTokenClaims(
        @SerialName("iss") val issuer: String? = null,
        @SerialName("sub") val subject: String? = null,
    )

    @Serializable
    private data class StoredBinding(
        val walletId: String,
        val recoveryKey: String,
    ) {

        companion object {

            fun of(walletId: String, recoveryKey: ByteArray): StoredBinding = StoredBinding(
                walletId = walletId,
                recoveryKey = Base64.encodeToString(recoveryKey, Base64.NO_WRAP),
            )
        }
    }

    private companion object {
        const val NONCE_SIZE_BYTES = 32
        val BINDINGS_KEY = stringPreferencesKey(name = "mockSocialRecoveryBindings")
    }
}
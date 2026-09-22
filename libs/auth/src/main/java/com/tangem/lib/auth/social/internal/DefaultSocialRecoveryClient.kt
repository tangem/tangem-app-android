package com.tangem.lib.auth.social.internal

import android.util.Base64
import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.core.remote.response.ApiResponse
import com.tangem.lib.auth.api.AuthApi
import com.tangem.lib.auth.api.models.request.DeviceMetadata
import com.tangem.lib.auth.api.models.request.NonceApiRequest
import com.tangem.lib.auth.attestation.AttestationProvider
import com.tangem.lib.auth.attestation.getAttestationTokenOrNull
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.WalletRegistrationError
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.lib.auth.session.internal.AuthErrorConverter
import com.tangem.lib.auth.session.internal.WalletRegistrationLocks
import com.tangem.lib.auth.session.internal.SessionTokensConverter
import com.tangem.lib.auth.session.internal.SignedRequestPayload
import com.tangem.lib.auth.session.internal.toBase64NoWrap
import com.tangem.lib.auth.social.HpkeEnvelope
import com.tangem.lib.auth.social.HpkeRecoveryCipher
import com.tangem.lib.auth.social.RecoveryKeyRsaCipher
import com.tangem.lib.auth.social.SignInNonce
import com.tangem.lib.auth.social.SocialRecoveryClient
import com.tangem.lib.auth.social.SocialRecoveryError
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.bouncycastle.crypto.AsymmetricCipherKeyPair

/**
 * [SocialRecoveryClient] over the Tangem Auth Service.
 *
 * Besides the DPoP-bound session, `recover` carries a device-key signature over the body: without it
 * HPKE would add nothing over TLS, since anything able to rewrite the body could swap in its own key.
 */
@Suppress("LongParameterList")
internal class DefaultSocialRecoveryClient(
    private val api: SocialRecoveryApi,
    private val authApi: AuthApi,
    private val deviceKeyManager: DeviceKeyManager,
    private val nonceDecryptor: AuthNonceDecryptor,
    private val signedRequestPayload: SignedRequestPayload,
    private val attestationProvider: AttestationProvider,
    private val errorConverter: AuthErrorConverter,
    private val sessionTokensStore: SessionTokensStore,
    private val walletRegistrar: WalletRegistrar,
    // shared with the plain wallet registrar, which posts to the same endpoint — see the class doc
    private val locks: WalletRegistrationLocks,
    private val dispatchers: CoroutineDispatcherProvider,
) : SocialRecoveryClient {

    override suspend fun requestSignInNonce(): Either<SocialRecoveryError, SignInNonce> = withContext(dispatchers.io) {
        either {
            val devicePublicKey = devicePublicKeyBase64()
            val cipheredNonce = when (
                val response = api.requestRecoverNonce(NonceApiRequest(devicePublicKey = devicePublicKey))
            ) {
                is ApiResponse.Success -> response.data.cipheredNonce
                is ApiResponse.Error -> raise(response.cause.toDomainError())
            }
            val nonce = decryptNonce(cipheredNonce)
            SignInNonce(forProvider = nonce.sha256Base64(), forRequest = nonce)
        }
    }

    override suspend fun recoverKey(nonce: String, providerIdToken: String): Either<SocialRecoveryError, ByteArray> =
        withContext(dispatchers.io) {
            either {
                ensureSession()

                val keyPair = HpkeRecoveryCipher.generateKeyPair()
                val encryptionPublicKey = HpkeRecoveryCipher.serializePublicKey(keyPair).toBase64NoWrap()
                // binds to the nonce, like every other attested call — the id token would both miss that
                // binding and push a multi-kilobyte JWT through Play Integrity as the request hash
                val attestationToken = attestationProvider.getAttestationTokenOrNull(nonce)
                // read once: the property re-reads the locale and the time zone on every call, and the
                // signature has to cover the very metadata the body carries
                val metadata = signedRequestPayload.deviceMetadata

                val request = WalletRecoverApiRequest(
                    nonce = nonce,
                    recoveryEncryptionPublicKey = encryptionPublicKey,
                    deviceSignature = signRecoverPayload(
                        nonce = nonce,
                        recoveryEncryptionPublicKey = encryptionPublicKey,
                        providerIdToken = providerIdToken,
                        attestationToken = attestationToken,
                        metadata = metadata,
                    ),
                    providerIdToken = providerIdToken,
                    attestationToken = attestationToken,
                    appAttestKeyId = null,
                    metadata = metadata,
                )

                val envelope = when (val response = api.recoverWallet(request)) {
                    is ApiResponse.Success -> HpkeEnvelope(
                        enc = decodeBase64(response.data.enc),
                        ct = decodeBase64(response.data.ct),
                    )
                    is ApiResponse.Error -> {
                        val error = response.cause.toDomainError()
                        // 404 is the documented "this identity has no wallet yet" answer, not a failure
                        raise(if (error.isNotFound()) SocialRecoveryError.BindingNotFound else error)
                    }
                }

                openRecoveryKey(keyPair, envelope)
            }
        }

    override suspend fun registerBinding(
        walletId: String,
        providerIdToken: String,
        recoveryKey: ByteArray,
        signer: WalletSigner,
    ): Either<SocialRecoveryError, Unit> = withContext(dispatchers.io) {
        // Held across the whole flow, like the plain wallet registration does: the call consumes a
        // one-time nonce and returns rotated session tokens, so two concurrent attempts for the same
        // wallet would burn two nonces and race to persist the tokens, the loser saving a set the
        // server has already superseded.
        locks.forWallet(walletId).withLock {
            either {
                ensureSession()

                val kmsPublicKey = when (val response = api.getRecoveryPublicKey()) {
                    is ApiResponse.Success -> response.data
                    is ApiResponse.Error -> raise(response.cause.toDomainError())
                }
                // sealing to an algorithm the service did not ask for stores a key it cannot unwrap, and the
                // binding looks healthy until someone tries to restore from it. A missing `alg` is refused
                // for the same reason: the one we would assume is exactly what is left unconfirmed.
                val algorithm = kmsPublicKey.algorithm
                if (algorithm != RecoveryKeyRsaCipher.SUPPORTED_ALGORITHM) {
                    TangemLogger.e("Unsupported recovery key algorithm: $algorithm")
                    raise(SocialRecoveryError.Unknown())
                }
                val sealed = try {
                    RecoveryKeyRsaCipher.seal(
                        modulusBase64Url = kmsPublicKey.modulus,
                        exponentBase64Url = kmsPublicKey.exponent,
                        plaintext = recoveryKey,
                    )
                } catch (e: Exception) {
                    TangemLogger.e("Failed to seal the recovery key for the KMS key", e)
                    raise(SocialRecoveryError.Unknown(e))
                }

                // fresh nonce per attempt: the call returns credentials, so the backend consumes the nonce
                // and a byte-identical retry can never be replayed
                val nonce = requestWalletNonce()
                val nonceBytes = try {
                    Base64.decode(nonce, Base64.URL_SAFE or Base64.NO_WRAP)
                } catch (e: Exception) {
                    TangemLogger.e("Failed to decode the wallet nonce", e)
                    raise(SocialRecoveryError.Unknown(e))
                }
                val bundle = try {
                    signer.sign(nonceBytes = nonceBytes)
                } catch (e: Exception) {
                    TangemLogger.e("Failed to sign the social wallet registration payload", e)
                    raise(SocialRecoveryError.SigningFailed(e))
                }

                val request = SocialWalletRegistrationApiRequest(
                    nonce = nonce,
                    walletId = walletId,
                    walletSignature = bundle.walletSignature.toBase64NoWrap(),
                    walletSignatureSalt = bundle.walletSignatureSalt.toBase64NoWrap(),
                    providerIdToken = providerIdToken,
                    encryptedRecoveryKey = sealed.toBase64NoWrap(),
                    attestationToken = attestationProvider.getAttestationTokenOrNull(nonce),
                    metadata = signedRequestPayload.deviceMetadata,
                )

                // the request and the token write stay under one lock: the reply reissues the session,
                // and a wallet registration running in parallel writes the same single token set
                locks.sessionRotation.withLock {
                    when (val response = api.registerWalletBinding(request)) {
                        is ApiResponse.Success -> try {
                            sessionTokensStore.save(SessionTokensConverter.convertBack(response.data))
                        } catch (e: Exception) {
                            // the server rotated the session; keeping the superseded tokens while reporting
                            // success would break every later authenticated call. Re-registering updates the
                            // binding rather than creating a second one, so the retry is safe.
                            TangemLogger.e("Failed to persist the tokens reissued by the social registration", e)
                            raise(SocialRecoveryError.Unknown(e))
                        }
                        is ApiResponse.Error -> raise(response.cause.toDomainError())
                    }
                }
            }
        }
    }

    /**
     * Delegated rather than calling the endpoint here: unregistering also clears the local
     * registered-wallet marker under the per-wallet lock, and a wallet the server has forgotten while
     * the app still believes it is registered would never be registered again.
     */
    override suspend fun unbindWallet(walletId: String): Either<SocialRecoveryError, Unit> =
        withContext(dispatchers.io) {
            walletRegistrar.unregister(walletId).fold(
                ifRight = { Either.Right(Unit) },
                ifLeft = { error ->
                    when (error) {
                        // a binding registered while auth was on outlives the toggle being turned off, and
                        // reporting success here would let the caller drop the flag and the cloud file
                        // while the identity stays bound to this wallet on the server
                        WalletRegistrationError.Disabled -> Either.Left(SocialRecoveryError.NoActiveSession)
                        is WalletRegistrationError.Api -> Either.Left(SocialRecoveryError.Api(error.cause))
                        WalletRegistrationError.DeviceKeyUnavailable ->
                            Either.Left(SocialRecoveryError.DeviceKeyUnavailable)
                        WalletRegistrationError.NoActiveSession -> Either.Left(SocialRecoveryError.NoActiveSession)
                        is WalletRegistrationError.SigningFailed ->
                            Either.Left(SocialRecoveryError.SigningFailed(error.cause))
                        is WalletRegistrationError.PersistenceFailed ->
                            Either.Left(SocialRecoveryError.Unknown(error.cause))
                        is WalletRegistrationError.NonceDecodingFailed ->
                            Either.Left(SocialRecoveryError.Unknown(error.cause))
                        is WalletRegistrationError.NonceDecryptionFailed ->
                            Either.Left(SocialRecoveryError.Unknown(error.cause))
                    }
                },
            )
        }

    private suspend fun Raise<SocialRecoveryError>.requestWalletNonce(): String {
        val response = authApi.requestWalletNonce(NonceApiRequest(devicePublicKey = devicePublicKeyBase64()))
        val cipheredNonce = when (response) {
            is ApiResponse.Success -> response.data.cipheredNonce
            is ApiResponse.Error -> raise(response.cause.toDomainError())
        }
        return decryptNonce(cipheredNonce)
    }

    private suspend fun Raise<SocialRecoveryError>.decryptNonce(cipheredNonce: String): String = try {
        nonceDecryptor.decryptNonce(cipheredNonce)
    } catch (e: Exception) {
        TangemLogger.e("Failed to decrypt the nonce", e)
        raise(SocialRecoveryError.Unknown(e))
    }

    /**
     * Colon-joined, a missing optional field collapsing to an empty segment and no trailing separator —
     * the same canonicalization the register/authenticate payloads use.
     */
    private suspend fun Raise<SocialRecoveryError>.signRecoverPayload(
        nonce: String,
        recoveryEncryptionPublicKey: String,
        providerIdToken: String,
        attestationToken: String?,
        metadata: DeviceMetadata,
    ): String {
        val payload = buildString {
            append(nonce).append(':')
            append(recoveryEncryptionPublicKey).append(':')
            append(providerIdToken).append(':')
            append(attestationToken.orEmpty()).append(':')
            append("").append(':') // appAttestKeyId — iOS only
            append(metadata.deviceModel).append(':')
            append(metadata.os).append(':')
            append(metadata.osVersion).append(':')
            append(metadata.appVersion).append(':')
            append(metadata.userAgent).append(':')
            append(metadata.locale).append(':')
            append(metadata.timezone)
        }.toByteArray(Charsets.UTF_8)

        return try {
            deviceKeyManager.signDer(payload).toBase64NoWrap()
        } catch (e: Exception) {
            TangemLogger.e("Failed to sign the recovery request payload", e)
            raise(SocialRecoveryError.SigningFailed(e))
        }
    }

    private fun Raise<SocialRecoveryError>.openRecoveryKey(
        keyPair: AsymmetricCipherKeyPair,
        envelope: HpkeEnvelope,
    ): ByteArray = try {
        HpkeRecoveryCipher.open(keyPair, envelope)
    } catch (e: Exception) {
        TangemLogger.e("Failed to open the recovery key envelope", e)
        raise(SocialRecoveryError.DecryptionFailed(e))
    }

    private suspend fun Raise<SocialRecoveryError>.devicePublicKeyBase64(): String =
        deviceKeyManager.getPublicKeyEncoded().getOrNull()?.toBase64NoWrap()
            ?: raise(SocialRecoveryError.DeviceKeyUnavailable)

    /** Without a session the request goes out unauthenticated and 401s, wasting its one-time nonce */
    private suspend fun Raise<SocialRecoveryError>.ensureSession() {
        val hasSession = try {
            sessionTokensStore.get().getOrNull() != null
        } catch (e: Exception) {
            TangemLogger.e("Failed to read the session tokens", e)
            raise(SocialRecoveryError.Unknown(e))
        }
        if (!hasSession) raise(SocialRecoveryError.NoActiveSession)
    }

    /** A 409 can only mean the one-to-one rule was broken — re-binding the same pair is allowed */
    private fun Throwable.toDomainError(): SocialRecoveryError = when (val authError = errorConverter.convert(this)) {
        is AuthError.Conflict -> SocialRecoveryError.BindingConflict
        else -> SocialRecoveryError.Api(authError)
    }

    private fun SocialRecoveryError.isNotFound(): Boolean =
        this is SocialRecoveryError.Api && error is AuthError.NotFound

    private fun Raise<SocialRecoveryError>.decodeBase64(value: String): ByteArray = try {
        Base64.decode(value, Base64.NO_WRAP)
    } catch (e: IllegalArgumentException) {
        raise(SocialRecoveryError.DecryptionFailed(e))
    }
}
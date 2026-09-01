package com.tangem.lib.auth.session.internal

import android.util.Base64
import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.lib.auth.api.AuthApi
import com.tangem.lib.auth.api.models.request.NonceApiRequest
import com.tangem.lib.auth.api.models.request.WalletRegistrationRequest
import com.tangem.lib.auth.api.models.request.WalletUnregisterRequest
import com.tangem.lib.auth.api.models.response.TokenApiResponse
import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.lib.auth.session.AuthPreferenceKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.lib.auth.attestation.AttestationProvider
import com.tangem.lib.auth.attestation.getAttestationTokenOrNull
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.PreparedWalletRegistration
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.WalletRegistrationError
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@Suppress("LongParameterList")
internal class DefaultWalletRegistrar(
    private val authApi: AuthApi,
    private val store: SessionTokensStore,
    private val deviceKeyManager: DeviceKeyManager,
    private val nonceDecryptor: AuthNonceDecryptor,
    private val signedRequestPayload: SignedRequestPayload,
    private val attestationProvider: AttestationProvider,
    private val errorConverter: AuthErrorConverter,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletRegistrar {

    // Per-wallet mutex: serialises concurrent attempts for the SAME wallet (so its nonce isn't
    // consumed twice) while letting different wallets register in parallel. The registered-wallet
    // set is mutated atomically inside a single DataStore transaction (see [markRegistered]), so it
    // needs no cross-wallet lock.
    private val mutexes = ConcurrentHashMap<String, Mutex>()

    override suspend fun register(walletId: String, signer: WalletSigner): Either<WalletRegistrationError, Unit> =
        withContext(dispatchers.io) {
            // Hold the per-wallet lock across the WHOLE one-shot flow (prepare + submit), so two
            // concurrent register() calls for the same wallet can't both pass the idempotency check
            // and consume/sign separate nonces. Calls the internal steps directly instead of
            // re-entering the public prepare()/submit() (which would take the same lock again).
            getMutex(walletId).withLock {
                either {
                    val prepared = runPrepare(walletId, signer).bind() ?: return@either
                    handleRegisterResponse(prepared.walletId, authApi.registerWallet(prepared.request))
                }
            }
        }

    override suspend fun prepare(
        walletId: String,
        signer: WalletSigner,
    ): Either<WalletRegistrationError, PreparedWalletRegistration?> = withContext(dispatchers.io) {
        getMutex(walletId).withLock { runPrepare(walletId, signer) }
    }

    override suspend fun submit(prepared: PreparedWalletRegistration): Either<WalletRegistrationError, Unit> =
        withContext(dispatchers.io) {
            getMutex(prepared.walletId).withLock {
                either { handleRegisterResponse(prepared.walletId, authApi.registerWallet(prepared.request)) }
            }
        }

    override suspend fun unregister(walletId: String): Either<WalletRegistrationError, Unit> =
        withContext(dispatchers.io) {
            getMutex(walletId).withLock {
                either {
                    val request = WalletUnregisterRequest(walletId = walletId)
                    when (val response = authApi.unregisterWallet(request)) {
                        is ApiResponse.Success -> {
                            try {
                                // Server rotated the session tokens to reflect the removed wallet;
                                // convert, persist and drop the local marker in one catch so this
                                // method stays total — every failure becomes a typed Left, never a throw.
                                val tokens = SessionTokensConverter.convertBack(response.data)
                                store.save(tokens)
                                markUnregistered(walletId)
                            } catch (e: Exception) {
                                TangemLogger.e("Failed to persist wallet-unregister tokens / marker", e)
                                raise(WalletRegistrationError.PersistenceFailed(e))
                            }
                            TangemLogger.i("Wallet unregistered successfully")
                        }
                        is ApiResponse.Error -> {
                            val authError = errorConverter.convert(response.cause)
                            if (authError is AuthError.NotFound) {
                                // Wallet is already not registered server-side (e.g. unregistered
                                // elsewhere). The desired end state is reached, so clear the local
                                // marker and treat it as success — idempotent, mirroring how register
                                // treats a 409 Conflict.
                                TangemLogger.i("Wallet already not registered server-side (404) — clearing marker")
                                try {
                                    markUnregistered(walletId)
                                } catch (e: Exception) {
                                    TangemLogger.e("Failed to clear marker after unregister 404", e)
                                    raise(WalletRegistrationError.PersistenceFailed(e))
                                }
                            } else {
                                TangemLogger.e("/wallet/unregister request failed: $authError")
                                raise(WalletRegistrationError.Api(authError))
                            }
                        }
                    }
                }
            }
        }

    private fun getMutex(walletId: String): Mutex = mutexes.computeIfAbsent(walletId) { Mutex() }

    private suspend fun runPrepare(
        walletId: String,
        signer: WalletSigner,
    ): Either<WalletRegistrationError, PreparedWalletRegistration?> = either {
        val isAlreadyRegistered = try {
            walletId in registeredWalletIds()
        } catch (e: Exception) {
            TangemLogger.e("Failed to read registered wallet ids", e)
            raise(WalletRegistrationError.PersistenceFailed(e))
        }
        if (isAlreadyRegistered) {
            TangemLogger.i("Wallet already registered — skipping /wallet")
            return@either null
        }

        TangemLogger.i("Preparing wallet registration")

        val devicePublicKey = deviceKeyManager.getPublicKeyEncoded().getOrNull()
            ?: raise(WalletRegistrationError.DeviceKeyUnavailable)
        val devicePublicKeyBase64 = devicePublicKey.toBase64NoWrap()

        val nonceResponse = authApi.requestWalletNonce(NonceApiRequest(devicePublicKey = devicePublicKeyBase64))
        val cipheredNonce = when (nonceResponse) {
            is ApiResponse.Success -> nonceResponse.data.cipheredNonce
            is ApiResponse.Error -> {
                val authError = errorConverter.convert(nonceResponse.cause)
                TangemLogger.e("/nonce/wallet request failed: $authError")
                raise(WalletRegistrationError.Api(authError))
            }
        }

        val nonce = try {
            nonceDecryptor.decryptNonce(cipheredNonce)
        } catch (e: Exception) {
            TangemLogger.e("Failed to decrypt wallet nonce", e)
            raise(WalletRegistrationError.NonceDecryptionFailed(e))
        }

        // The server issues the nonce as a base64url string but signs/verifies over its raw bytes.
        // Decode once here so every signer (MOBILE + COLD) operates on the exact bytes the backend
        // recovers the wallet public key against; the string form is still sent in the request
        // (`nonce` below) for the server to re-derive.
        val nonceBytes = try {
            Base64.decode(nonce, Base64.URL_SAFE or Base64.NO_WRAP)
        } catch (e: Exception) {
            TangemLogger.e("Failed to decode wallet nonce", e)
            raise(WalletRegistrationError.NonceDecodingFailed(e))
        }

        val bundle = try {
            signer.sign(nonceBytes = nonceBytes)
        } catch (e: Exception) {
            TangemLogger.e("Failed to sign wallet-registration payload", e)
            raise(WalletRegistrationError.SigningFailed(e))
        }

        PreparedWalletRegistration(
            walletId = walletId,
            request = WalletRegistrationRequest(
                nonce = nonce,
                walletId = walletId,
                walletSignature = bundle.walletSignature.toBase64NoWrap(),
                walletSignatureSalt = bundle.walletSignatureSalt.toBase64NoWrap(),
                cardSignature = bundle.cardSignature?.toBase64NoWrap(),
                cardSignatureSalt = bundle.cardSignatureSalt?.toBase64NoWrap(),
                walletStatus = bundle.walletStatusByte?.let { byteArrayOf(it).toBase64NoWrap() },
                attestationToken = attestationProvider.getAttestationTokenOrNull(nonce),
                metadata = signedRequestPayload.deviceMetadata,
            ),
        )
    }

    private suspend fun Raise<WalletRegistrationError>.handleRegisterResponse(
        walletId: String,
        response: ApiResponse<TokenApiResponse>,
    ) {
        when (response) {
            is ApiResponse.Success -> {
                val tokens = SessionTokensConverter.convertBack(response.data)
                try {
                    // Keep both writes inside one catch — if the marker write fails, the wallet
                    // stays unregistered locally and the next attempt retries cleanly.
                    store.save(tokens)
                    markRegistered(walletId)
                } catch (e: Exception) {
                    TangemLogger.e("Failed to persist wallet-registration tokens / marker", e)
                    raise(WalletRegistrationError.PersistenceFailed(e))
                }
                TangemLogger.i("Wallet registered successfully")
            }
            is ApiResponse.Error -> {
                val authError = errorConverter.convert(response.cause)
                if (authError is AuthError.Conflict) {
                    // Wallet is already registered server-side (e.g. local marker was lost on
                    // reinstall). Persist the marker to stop retrying.
                    TangemLogger.i("Wallet already registered server-side (409) — marking as registered")
                    try {
                        markRegistered(walletId)
                    } catch (e: Exception) {
                        TangemLogger.e("Failed to persist wallet-registration marker after 409", e)
                        raise(WalletRegistrationError.PersistenceFailed(e))
                    }
                    return
                }
                TangemLogger.e("/wallet request failed: $authError")
                raise(WalletRegistrationError.Api(authError))
            }
        }
    }

    private suspend fun registeredWalletIds(): Set<String> = appPreferencesStore.getSyncOrDefault(
        key = AuthPreferenceKeys.REGISTERED_WALLET_IDS_KEY,
        default = emptySet(),
    )

    private suspend fun markRegistered(walletId: String) {
        // Atomic read-modify-write inside a single DataStore transaction — DataStore serialises
        // these, so concurrent registrations of different wallets can't lose set entries.
        appPreferencesStore.editData { preferences ->
            val current = preferences.getOrDefault(AuthPreferenceKeys.REGISTERED_WALLET_IDS_KEY, emptySet())
            preferences[AuthPreferenceKeys.REGISTERED_WALLET_IDS_KEY] = current + walletId
        }
    }

    private suspend fun markUnregistered(walletId: String) {
        appPreferencesStore.editData { preferences ->
            val current = preferences.getOrDefault(AuthPreferenceKeys.REGISTERED_WALLET_IDS_KEY, emptySet())
            preferences[AuthPreferenceKeys.REGISTERED_WALLET_IDS_KEY] = current - walletId
        }
    }
}
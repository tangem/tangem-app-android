package com.tangem.lib.auth.session.internal

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.datasource.api.auth.AuthApi
import com.tangem.datasource.api.auth.models.request.NonceApiRequest
import com.tangem.datasource.api.auth.models.request.WalletRegistrationRequest
import com.tangem.datasource.api.auth.models.response.TokenApiResponse
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.WalletRegistrationError
import com.tangem.lib.auth.session.WalletSignatureBundle
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
    private val errorConverter: AuthErrorConverter,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : WalletRegistrar {

    // Per-wallet mutex: serialises concurrent attempts for the SAME wallet (so its nonce isn't
    // consumed twice) while letting different wallets register in parallel. The registered-wallet
    // set is mutated atomically inside a single DataStore transaction (see [markRegistered]), so it
    // needs no cross-wallet lock.
    private val mutexes = ConcurrentHashMap<String, Mutex>()

    override suspend fun register(
        walletId: String,
        signer: WalletSigner,
    ): Either<WalletRegistrationError, Unit> = withContext(dispatchers.io) {
        getMutex(walletId).withLock { runRegister(walletId, signer) }
    }

    private fun getMutex(walletId: String): Mutex = mutexes.computeIfAbsent(walletId) { Mutex() }

    private suspend fun runRegister(walletId: String, signer: WalletSigner): Either<WalletRegistrationError, Unit> =
        either {
            if (walletId in registeredWalletIds()) {
                TangemLogger.i("Wallet already registered — skipping /wallet")
                return@either
            }

            TangemLogger.i("Starting wallet registration")

            val devicePublicKey = deviceKeyManager.getPublicKey().getOrNull()
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

            val bundle = try {
                signer.sign(nonceBytes = nonce.toByteArray(Charsets.UTF_8))
            } catch (e: Exception) {
                TangemLogger.e("Failed to sign wallet-registration payload", e)
                raise(WalletRegistrationError.SigningFailed(e))
            }

            val request = WalletRegistrationRequest(
                nonce = nonce,
                walletId = walletId,
                walletSignature = bundle.walletSignature.toBase64NoWrap(),
                walletSignatureSalt = bundle.walletSignatureSalt.toBase64NoWrap(),
                cardSignature = bundle.cardSignature?.toBase64NoWrap(),
                cardSignatureSalt = bundle.cardSignatureSalt?.toBase64NoWrap(),
                walletStatus = bundle.walletStatusByte?.let { byteArrayOf(it).toBase64NoWrap() },
                attestationToken = null,
                metadata = signedRequestPayload.deviceMetadata,
            )

            handleRegisterResponse(walletId = walletId, response = authApi.registerWallet(request))
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
        key = PreferencesKeys.REGISTERED_WALLET_IDS_KEY,
        default = emptySet(),
    )

    private suspend fun markRegistered(walletId: String) {
        // Atomic read-modify-write inside a single DataStore transaction — DataStore serialises
        // these, so concurrent registrations of different wallets can't lose set entries.
        appPreferencesStore.editData { preferences ->
            val current = preferences.getOrDefault(PreferencesKeys.REGISTERED_WALLET_IDS_KEY, emptySet())
            preferences[PreferencesKeys.REGISTERED_WALLET_IDS_KEY] = current + walletId
        }
    }
}
package com.tangem.lib.auth.session.internal

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.lib.auth.api.AuthApi
import com.tangem.lib.auth.api.models.request.NonceApiRequest
import com.tangem.lib.auth.api.models.request.RegisterApiRequest
import com.tangem.lib.auth.api.models.request.RegisterPayload
import com.tangem.lib.auth.api.models.response.TokenApiResponse
import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.lib.auth.session.AuthPreferenceKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.lib.auth.attestation.AttestationProvider
import com.tangem.lib.auth.attestation.getAttestationTokenOrNull
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.DeviceRegistrar
import com.tangem.lib.auth.session.DeviceRegistrationError
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class DefaultDeviceRegistrar(
    private val authApi: AuthApi,
    private val store: SessionTokensStore,
    private val deviceKeyManager: DeviceKeyManager,
    private val nonceDecryptor: AuthNonceDecryptor,
    private val signedRequestPayload: SignedRequestPayload,
    private val attestationProvider: AttestationProvider,
    private val errorConverter: AuthErrorConverter,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : DeviceRegistrar {

    private val mutex = Mutex()

    override suspend fun register(): Either<DeviceRegistrationError, Unit> = withContext(dispatchers.io) {
        // `Mutex` guards against the unlikely case of two concurrent callers passing the
        // already-registered check together and consuming the same device nonce twice.
        mutex.withLock { runRegister() }
    }

    override suspend fun reregister(): Either<DeviceRegistrationError, Unit> = withContext(dispatchers.io) {
        mutex.withLock {
            either {
                // The local flag is stale (backend lost the device record). Clear it up front so
                // runRegister doesn't short-circuit, and so a failed attempt self-heals on the next
                // launch's register() call.
                runSuspendCatching {
                    appPreferencesStore.store(key = AuthPreferenceKeys.IS_DEVICE_REGISTERED_KEY, value = false)
                }.onFailure { e ->
                    TangemLogger.e("Failed to reset device-registration flag before re-register", e)
                    raise(DeviceRegistrationError.PersistenceFailed(e))
                }
                runRegister().bind()
            }
        }
    }

    private suspend fun runRegister(): Either<DeviceRegistrationError, Unit> = either {
        val isAlreadyRegistered = appPreferencesStore.getSyncOrDefault(
            key = AuthPreferenceKeys.IS_DEVICE_REGISTERED_KEY,
            default = false,
        )
        if (isAlreadyRegistered) {
            TangemLogger.i("Device already registered — skipping /register")
            return@either
        }

        TangemLogger.i("Starting device registration")

        val devicePublicKey = deviceKeyManager.getPublicKeyEncoded().getOrNull()
            ?: raise(DeviceRegistrationError.DeviceKeyUnavailable)

        val devicePublicKeyBase64 = devicePublicKey.toBase64NoWrap()

        val nonceResponse = authApi.requestDeviceNonce(NonceApiRequest(devicePublicKey = devicePublicKeyBase64))
        val cipheredNonce = when (nonceResponse) {
            is ApiResponse.Success -> nonceResponse.data.cipheredNonce
            is ApiResponse.Error -> {
                val authError = errorConverter.convert(nonceResponse.cause)
                TangemLogger.e("/nonce/device request failed: $authError")
                raise(DeviceRegistrationError.Api(authError))
            }
        }

        val nonce = try {
            nonceDecryptor.decryptNonce(cipheredNonce)
        } catch (e: Exception) {
            TangemLogger.e("Failed to decrypt device-registration nonce", e)
            raise(DeviceRegistrationError.NonceDecryptionFailed(e))
        }

        val payload = RegisterPayload(
            devicePublicKey = devicePublicKeyBase64,
            nonce = nonce,
            attestationToken = attestationProvider.getAttestationTokenOrNull(nonce),
            metadata = signedRequestPayload.deviceMetadata,
        )
        val signature = try {
            deviceKeyManager.signDer(signedRequestPayload.canonicalize(payload)).toBase64NoWrap()
        } catch (e: Exception) {
            TangemLogger.e("Failed to sign device-registration payload", e)
            raise(DeviceRegistrationError.SigningFailed(e))
        }

        val registerResponse = authApi.registerDevice(RegisterApiRequest(payload = payload, signature = signature))
        handleRegisterResponse(registerResponse)
    }

    private suspend fun Raise<DeviceRegistrationError>.handleRegisterResponse(
        response: ApiResponse<TokenApiResponse>,
    ) {
        when (response) {
            is ApiResponse.Success -> {
                val tokens = SessionTokensConverter.convertBack(response.data)
                try {
                    // Keep both writes inside one catch — if the second one fails, the flag stays
                    // `false` and the next launch retries cleanly. Worst case: tokens are persisted
                    // without the flag, and the retry mints fresh ones that overwrite them.
                    store.save(tokens)
                    appPreferencesStore.store(key = AuthPreferenceKeys.IS_DEVICE_REGISTERED_KEY, value = true)
                } catch (e: Exception) {
                    TangemLogger.e("Failed to persist device-registration tokens / flag", e)
                    raise(DeviceRegistrationError.PersistenceFailed(e))
                }
                TangemLogger.i("Device registered successfully")
            }
            is ApiResponse.Error -> {
                val authError = errorConverter.convert(response.cause)
                if (authError is AuthError.Conflict) {
                    // Device is already registered server-side (e.g. the local flag was lost on
                    // reinstall). Persist the flag to stop retrying; session tokens will be minted
                    // on demand via /authenticate.
                    TangemLogger.i("Device already registered server-side (409) — marking as registered")
                    markRegistered(onFailureLog = "Failed to persist device-registration flag after 409")
                    return
                }
                TangemLogger.e("/register request failed: $authError")
                raise(DeviceRegistrationError.Api(authError))
            }
        }
    }

    private suspend fun Raise<DeviceRegistrationError>.markRegistered(onFailureLog: String) {
        try {
            appPreferencesStore.store(key = AuthPreferenceKeys.IS_DEVICE_REGISTERED_KEY, value = true)
        } catch (e: Exception) {
            TangemLogger.e(onFailureLog, e)
            raise(DeviceRegistrationError.PersistenceFailed(e))
        }
    }
}
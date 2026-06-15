package com.tangem.lib.auth.session.internal

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.datasource.api.auth.AuthApi
import com.tangem.datasource.api.auth.models.request.NonceApiRequest
import com.tangem.datasource.api.auth.models.request.RegisterApiRequest
import com.tangem.datasource.api.auth.models.request.RegisterPayload
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.session.DeviceRegistrar
import com.tangem.lib.auth.session.DeviceRegistrationError
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
    private val errorConverter: AuthErrorConverter,
    private val appPreferencesStore: AppPreferencesStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : DeviceRegistrar {

    private val mutex = Mutex()

    override suspend fun register(): Either<DeviceRegistrationError, Unit> = withContext(dispatchers.io) {
        // `Mutex` guards against the unlikely case of two concurrent callers passing the
        // already-registered check together and consuming the same `/nonce/device` value twice.
        mutex.withLock { runRegister() }
    }

    private suspend fun runRegister(): Either<DeviceRegistrationError, Unit> = either {
        val isAlreadyRegistered = appPreferencesStore.getSyncOrDefault(
            key = PreferencesKeys.IS_DEVICE_REGISTERED_KEY,
            default = false,
        )
        if (isAlreadyRegistered) {
            TangemLogger.i("Device already registered — skipping /register")
            return@either
        }

        TangemLogger.i("Starting device registration")

        val devicePublicKey = deviceKeyManager.getPublicKey().getOrNull()
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
            attestationToken = null,
            metadata = signedRequestPayload.deviceMetadata,
        )
        val signature = try {
            deviceKeyManager.sign(signedRequestPayload.canonicalize(payload)).toBase64NoWrap()
        } catch (e: Exception) {
            TangemLogger.e("Failed to sign device-registration payload", e)
            raise(DeviceRegistrationError.SigningFailed(e))
        }

        val registerResponse = authApi.register(RegisterApiRequest(payload = payload, signature = signature))
        when (registerResponse) {
            is ApiResponse.Success -> {
                val tokens = SessionTokensConverter.convertBack(registerResponse.data)
                try {
                    // Keep both writes inside one catch — if the second one fails, the flag stays
                    // `false` and the next launch retries cleanly. Worst case: tokens are persisted
                    // without the flag, and the retry mints fresh ones that overwrite them.
                    store.save(tokens)
                    appPreferencesStore.store(key = PreferencesKeys.IS_DEVICE_REGISTERED_KEY, value = true)
                } catch (e: Exception) {
                    TangemLogger.e("Failed to persist device-registration tokens / flag", e)
                    raise(DeviceRegistrationError.PersistenceFailed(e))
                }
                TangemLogger.i("Device registered successfully")
            }
            is ApiResponse.Error -> {
                val authError = errorConverter.convert(registerResponse.cause)
                TangemLogger.e("/register request failed: $authError")
                raise(DeviceRegistrationError.Api(authError))
            }
        }
    }
}
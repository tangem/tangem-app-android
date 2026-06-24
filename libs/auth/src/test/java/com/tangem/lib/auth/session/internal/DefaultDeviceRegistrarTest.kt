package com.tangem.lib.auth.session.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import arrow.core.None
import arrow.core.Some
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.auth.AuthApi
import com.tangem.datasource.api.auth.models.request.RegisterApiRequest
import com.tangem.datasource.api.auth.models.response.NonceApiResponse
import com.tangem.datasource.api.auth.models.response.TokenApiResponse
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.session.DeviceRegistrationError
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultDeviceRegistrarTest {

    private val authApi: AuthApi = mockk()
    private val store: SessionTokensStore = mockk(relaxUnitFun = true)
    private val deviceKeyManager: DeviceKeyManager = mockk()
    private val nonceDecryptor: AuthNonceDecryptor = mockk()
    private val appInfoProvider: AppInfoProvider = mockk(relaxed = true)
    private val signedRequestPayload = SignedRequestPayload(appInfoProvider)
    private val errorConverter = AuthErrorConverter()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val preferencesDataStore = InMemoryPreferencesDataStore()
    private val appPreferencesStore = AppPreferencesStore(
        moshi = Moshi.Builder().build(),
        dispatchers = dispatchers,
        preferencesDataStore = preferencesDataStore,
    )

    private lateinit var registrar: DefaultDeviceRegistrar

    @BeforeEach
    fun setup() {
        clearMocks(authApi, store, deviceKeyManager, nonceDecryptor)
        preferencesDataStore.reset()
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
        registrar = DefaultDeviceRegistrar(
            authApi = authApi,
            store = store,
            deviceKeyManager = deviceKeyManager,
            nonceDecryptor = nonceDecryptor,
            signedRequestPayload = signedRequestPayload,
            errorConverter = errorConverter,
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatchers,
        )
    }

    @AfterEach
    fun teardown() = unmockkAll()

    @Test
    fun `register posts nonce + register and persists tokens and flag on success`() = runTest {
        stubHappyPath()

        val result = registrar.register()

        assertThat(result.isRight()).isTrue()
        coVerify { authApi.requestDeviceNonce(any()) }
        coVerify { authApi.registerDevice(any<RegisterApiRequest>()) }
        coVerify { store.save(any()) }
        assertThat(preferencesDataStore.current()[PreferencesKeys.IS_DEVICE_REGISTERED_KEY]).isTrue()
    }

    @Test
    fun `register short-circuits without network when the flag is already set`() = runTest {
        preferencesDataStore.edit { it[PreferencesKeys.IS_DEVICE_REGISTERED_KEY] = true }

        val result = registrar.register()

        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { authApi.requestDeviceNonce(any()) }
        coVerify(exactly = 0) { authApi.registerDevice(any<RegisterApiRequest>()) }
        coVerify(exactly = 0) { store.save(any()) }
    }

    @Test
    fun `register returns DeviceKeyUnavailable when keystore has no key`() = runTest {
        coEvery { deviceKeyManager.getPublicKey() } returns None

        val result = registrar.register()

        assertThat(result.leftOrNull()).isEqualTo(DeviceRegistrationError.DeviceKeyUnavailable)
        coVerify(exactly = 0) { authApi.requestDeviceNonce(any()) }
        coVerify(exactly = 0) { authApi.registerDevice(any<RegisterApiRequest>()) }
        assertThat(preferencesDataStore.current()[PreferencesKeys.IS_DEVICE_REGISTERED_KEY]).isNull()
    }

    @Test
    fun `register surfaces nonce-endpoint API error`() = runTest {
        coEvery { deviceKeyManager.getPublicKey() } returns Some(ByteArray(65))
        @Suppress("UNCHECKED_CAST")
        coEvery { authApi.requestDeviceNonce(any()) } returns ApiResponse.Error(
            cause = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.TOO_MANY_REQUESTS,
                message = "rate-limited",
                errorBody = null,
            ),
        ) as ApiResponse<NonceApiResponse>

        val result = registrar.register()

        assertThat(result.leftOrNull()).isInstanceOf(DeviceRegistrationError.Api::class.java)
        coVerify(exactly = 0) { authApi.registerDevice(any<RegisterApiRequest>()) }
        assertThat(preferencesDataStore.current()[PreferencesKeys.IS_DEVICE_REGISTERED_KEY]).isNull()
    }

    @Test
    fun `register returns NonceDecryptionFailed when decryptor throws`() = runTest {
        coEvery { deviceKeyManager.getPublicKey() } returns Some(ByteArray(65))
        coEvery { authApi.requestDeviceNonce(any()) } returns ApiResponse.Success(
            data = NonceApiResponse(cipheredNonce = "abc", expiresAt = "2024-01-01T00:00:00Z"),
        )
        coEvery { nonceDecryptor.decryptNonce("abc") } throws IllegalStateException("OAEP failed")

        val result = registrar.register()

        assertThat(result.leftOrNull()).isInstanceOf(DeviceRegistrationError.NonceDecryptionFailed::class.java)
        coVerify(exactly = 0) { authApi.registerDevice(any<RegisterApiRequest>()) }
    }

    @Test
    fun `register returns SigningFailed when signing throws`() = runTest {
        coEvery { deviceKeyManager.getPublicKey() } returns Some(ByteArray(65))
        coEvery { authApi.requestDeviceNonce(any()) } returns ApiResponse.Success(
            data = NonceApiResponse(cipheredNonce = "abc", expiresAt = "2024-01-01T00:00:00Z"),
        )
        coEvery { nonceDecryptor.decryptNonce("abc") } returns "decrypted"
        coEvery { deviceKeyManager.sign(any()) } throws IllegalStateException("Keystore offline")

        val result = registrar.register()

        assertThat(result.leftOrNull()).isInstanceOf(DeviceRegistrationError.SigningFailed::class.java)
        coVerify(exactly = 0) { authApi.registerDevice(any<RegisterApiRequest>()) }
    }

    @Test
    fun `register surfaces register-endpoint API error and does not touch tokens or flag`() = runTest {
        coEvery { deviceKeyManager.getPublicKey() } returns Some(ByteArray(65))
        coEvery { authApi.requestDeviceNonce(any()) } returns ApiResponse.Success(
            data = NonceApiResponse(cipheredNonce = "abc", expiresAt = "2024-01-01T00:00:00Z"),
        )
        coEvery { nonceDecryptor.decryptNonce("abc") } returns "decrypted"
        coEvery { deviceKeyManager.sign(any()) } returns ByteArray(64)
        @Suppress("UNCHECKED_CAST")
        coEvery { authApi.registerDevice(any<RegisterApiRequest>()) } returns ApiResponse.Error(
            cause = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.FORBIDDEN,
                message = "already registered",
                errorBody = null,
            ),
        ) as ApiResponse<TokenApiResponse>

        val result = registrar.register()

        assertThat(result.leftOrNull()).isInstanceOf(DeviceRegistrationError.Api::class.java)
        coVerify(exactly = 0) { store.save(any()) }
        assertThat(preferencesDataStore.current()[PreferencesKeys.IS_DEVICE_REGISTERED_KEY]).isNull()
    }

    @Test
    fun `register treats 409 Conflict as success, sets flag without persisting tokens`() = runTest {
        coEvery { deviceKeyManager.getPublicKey() } returns Some(ByteArray(65))
        coEvery { authApi.requestDeviceNonce(any()) } returns ApiResponse.Success(
            data = NonceApiResponse(cipheredNonce = "abc", expiresAt = "2024-01-01T00:00:00Z"),
        )
        coEvery { nonceDecryptor.decryptNonce("abc") } returns "decrypted"
        coEvery { deviceKeyManager.sign(any()) } returns ByteArray(64)
        @Suppress("UNCHECKED_CAST")
        coEvery { authApi.registerDevice(any<RegisterApiRequest>()) } returns ApiResponse.Error(
            cause = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.CONFLICT,
                message = "device already registered",
                errorBody = null,
            ),
        ) as ApiResponse<TokenApiResponse>

        val result = registrar.register()

        // Device is already registered server-side — no error, flag set, but no tokens minted here.
        assertThat(result.isRight()).isTrue()
        assertThat(preferencesDataStore.current()[PreferencesKeys.IS_DEVICE_REGISTERED_KEY]).isTrue()
        coVerify(exactly = 0) { store.save(any()) }
    }

    @Test
    fun `register returns PersistenceFailed when SessionTokensStore_save throws`() = runTest {
        stubHappyPath()
        coEvery { store.save(any()) } throws IllegalStateException("DataStore I/O")

        val result = registrar.register()

        assertThat(result.leftOrNull()).isInstanceOf(DeviceRegistrationError.PersistenceFailed::class.java)
        // Flag must stay unset so the next launch retries cleanly.
        assertThat(preferencesDataStore.current()[PreferencesKeys.IS_DEVICE_REGISTERED_KEY]).isNull()
    }

    private fun stubHappyPath() {
        coEvery { deviceKeyManager.getPublicKey() } returns Some(ByteArray(65))
        coEvery { authApi.requestDeviceNonce(any()) } returns ApiResponse.Success(
            data = NonceApiResponse(cipheredNonce = "abc", expiresAt = "2024-01-01T00:00:00Z"),
        )
        coEvery { nonceDecryptor.decryptNonce("abc") } returns "decrypted"
        coEvery { deviceKeyManager.sign(any()) } returns ByteArray(64)
        coEvery { authApi.registerDevice(any<RegisterApiRequest>()) } returns ApiResponse.Success(
            data = TokenApiResponse(
                accessToken = "fresh-access",
                accessTokenExpiresAt = "2024-01-01T00:00:00Z",
                refreshToken = "fresh-rt",
                refreshTokenExpiresAt = "2024-02-01T00:00:00Z",
                walletIds = listOf("w1"),
            ),
        )
    }

    /** Minimal in-memory [DataStore] implementation — only the surface area used by tests. */
    private class InMemoryPreferencesDataStore : DataStore<Preferences> {

        private var preferences: MutablePreferences = mutablePreferencesOf()

        override val data get() = flowOf(preferences)

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            preferences = transform(preferences).toMutablePreferences()
            return preferences
        }

        fun edit(block: (MutablePreferences) -> Unit) {
            preferences = preferences.toMutablePreferences().also(block)
        }

        fun current(): Preferences = preferences

        fun reset() {
            preferences = mutablePreferencesOf()
        }
    }
}
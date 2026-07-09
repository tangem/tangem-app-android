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
import com.tangem.datasource.api.auth.models.request.WalletRegistrationRequest
import com.tangem.datasource.api.auth.models.response.NonceApiResponse
import com.tangem.datasource.api.auth.models.response.TokenApiResponse
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.session.WalletRegistrationError
import com.tangem.lib.auth.session.WalletSignatureBundle
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultWalletRegistrarTest {

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

    private lateinit var registrar: DefaultWalletRegistrar

    private val mobileSigner = WalletSigner {
        WalletSignatureBundle(
            walletSignature = ByteArray(65) { 1 },
            walletSignatureSalt = ByteArray(16) { 2 },
            cardSignature = null,
            cardSignatureSalt = null,
            walletStatusByte = null,
        )
    }

    private val coldSigner = WalletSigner {
        WalletSignatureBundle(
            walletSignature = ByteArray(65) { 1 },
            walletSignatureSalt = ByteArray(16) { 2 },
            cardSignature = ByteArray(65) { 3 },
            cardSignatureSalt = ByteArray(16) { 4 },
            walletStatusByte = 0x82.toByte(),
        )
    }

    @BeforeEach
    fun setup() {
        clearMocks(authApi, store, deviceKeyManager, nonceDecryptor)
        preferencesDataStore.reset()
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            java.util.Base64.getEncoder().encodeToString(firstArg())
        }
        registrar = DefaultWalletRegistrar(
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
    fun `register MOBILE posts wallet and persists tokens and marker with null card fields`() = runTest {
        stubHappyPath()
        val slot = slot<WalletRegistrationRequest>()
        coEvery { authApi.registerWallet(capture(slot)) } returns tokenSuccess()

        val result = registrar.register(WALLET_ID, mobileSigner)

        assertThat(result.isRight()).isTrue()
        coVerify { authApi.requestWalletNonce(any()) }
        coVerify { store.save(any()) }
        assertThat(registeredIds()).contains(WALLET_ID)
        val request = slot.captured
        assertThat(request.walletId).isEqualTo(WALLET_ID)
        assertThat(request.walletSignature).isEqualTo(base64(ByteArray(65) { 1 }))
        assertThat(request.walletSignatureSalt).isEqualTo(base64(ByteArray(16) { 2 }))
        assertThat(request.cardSignature).isNull()
        assertThat(request.cardSignatureSalt).isNull()
        assertThat(request.walletStatus).isNull()
        assertThat(request.attestationToken).isNull()
    }

    @Test
    fun `register COLD posts wallet with card fields and single-byte walletStatus`() = runTest {
        stubHappyPath()
        val slot = slot<WalletRegistrationRequest>()
        coEvery { authApi.registerWallet(capture(slot)) } returns tokenSuccess()

        val result = registrar.register(WALLET_ID, coldSigner)

        assertThat(result.isRight()).isTrue()
        val request = slot.captured
        assertThat(request.cardSignature).isEqualTo(base64(ByteArray(65) { 3 }))
        assertThat(request.cardSignatureSalt).isEqualTo(base64(ByteArray(16) { 4 }))
        // walletStatus must be exactly one Base64-encoded byte (0x82).
        assertThat(request.walletStatus).isEqualTo(base64(byteArrayOf(0x82.toByte())))
    }

    @Test
    fun `registering two different wallets accumulates both ids in the marker set`() = runTest {
        stubHappyPath()
        coEvery { authApi.registerWallet(any()) } returns tokenSuccess()

        assertThat(registrar.register(WALLET_ID, mobileSigner).isRight()).isTrue()
        assertThat(registrar.register(OTHER_WALLET_ID, mobileSigner).isRight()).isTrue()

        // markRegistered must read-modify-write atomically, not overwrite the existing set.
        assertThat(registeredIds()).containsExactly(WALLET_ID, OTHER_WALLET_ID)
    }

    @Test
    fun `register short-circuits without network when walletId already registered`() = runTest {
        preferencesDataStore.edit { it[PreferencesKeys.REGISTERED_WALLET_IDS_KEY] = setOf(WALLET_ID) }

        val result = registrar.register(WALLET_ID, mobileSigner)

        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { authApi.requestWalletNonce(any()) }
        coVerify(exactly = 0) { authApi.registerWallet(any()) }
        coVerify(exactly = 0) { store.save(any()) }
    }

    @Test
    fun `register returns DeviceKeyUnavailable when keystore has no key`() = runTest {
        coEvery { deviceKeyManager.getPublicKey() } returns None

        val result = registrar.register(WALLET_ID, mobileSigner)

        assertThat(result.leftOrNull()).isEqualTo(WalletRegistrationError.DeviceKeyUnavailable)
        coVerify(exactly = 0) { authApi.requestWalletNonce(any()) }
        assertThat(registeredIds()).doesNotContain(WALLET_ID)
    }

    @Test
    fun `register surfaces nonce-endpoint API error`() = runTest {
        coEvery { deviceKeyManager.getPublicKey() } returns Some(ByteArray(65))
        @Suppress("UNCHECKED_CAST")
        coEvery { authApi.requestWalletNonce(any()) } returns ApiResponse.Error(
            cause = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.TOO_MANY_REQUESTS,
                message = "rate-limited",
                errorBody = null,
            ),
        ) as ApiResponse<NonceApiResponse>

        val result = registrar.register(WALLET_ID, mobileSigner)

        assertThat(result.leftOrNull()).isInstanceOf(WalletRegistrationError.Api::class.java)
        coVerify(exactly = 0) { authApi.registerWallet(any()) }
        assertThat(registeredIds()).doesNotContain(WALLET_ID)
    }

    @Test
    fun `register returns NonceDecryptionFailed when decryptor throws`() = runTest {
        coEvery { deviceKeyManager.getPublicKey() } returns Some(ByteArray(65))
        coEvery { authApi.requestWalletNonce(any()) } returns nonceSuccess()
        coEvery { nonceDecryptor.decryptNonce("abc") } throws IllegalStateException("OAEP failed")

        val result = registrar.register(WALLET_ID, mobileSigner)

        assertThat(result.leftOrNull()).isInstanceOf(WalletRegistrationError.NonceDecryptionFailed::class.java)
        coVerify(exactly = 0) { authApi.registerWallet(any()) }
    }

    @Test
    fun `register returns SigningFailed when signer throws (e g cancelled NFC or biometric)`() = runTest {
        coEvery { deviceKeyManager.getPublicKey() } returns Some(ByteArray(65))
        coEvery { authApi.requestWalletNonce(any()) } returns nonceSuccess()
        coEvery { nonceDecryptor.decryptNonce("abc") } returns "decrypted"
        val failingSigner = WalletSigner { throw IllegalStateException("user cancelled") }

        val result = registrar.register(WALLET_ID, failingSigner)

        assertThat(result.leftOrNull()).isInstanceOf(WalletRegistrationError.SigningFailed::class.java)
        coVerify(exactly = 0) { authApi.registerWallet(any()) }
    }

    @Test
    fun `register surfaces wallet-endpoint API error and does not touch tokens or marker`() = runTest {
        stubHappyPath()
        @Suppress("UNCHECKED_CAST")
        coEvery { authApi.registerWallet(any()) } returns ApiResponse.Error(
            cause = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.FORBIDDEN,
                message = "max wallets",
                errorBody = null,
            ),
        ) as ApiResponse<TokenApiResponse>

        val result = registrar.register(WALLET_ID, mobileSigner)

        assertThat(result.leftOrNull()).isInstanceOf(WalletRegistrationError.Api::class.java)
        coVerify(exactly = 0) { store.save(any()) }
        assertThat(registeredIds()).doesNotContain(WALLET_ID)
    }

    @Test
    fun `register treats 409 Conflict as success, marks registered without persisting tokens`() = runTest {
        stubHappyPath()
        @Suppress("UNCHECKED_CAST")
        coEvery { authApi.registerWallet(any()) } returns ApiResponse.Error(
            cause = ApiResponseError.HttpException(
                code = ApiResponseError.HttpException.Code.CONFLICT,
                message = "wallet already registered",
                errorBody = null,
            ),
        ) as ApiResponse<TokenApiResponse>

        val result = registrar.register(WALLET_ID, mobileSigner)

        assertThat(result.isRight()).isTrue()
        assertThat(registeredIds()).contains(WALLET_ID)
        coVerify(exactly = 0) { store.save(any()) }
    }

    @Test
    fun `register returns PersistenceFailed when SessionTokensStore_save throws`() = runTest {
        stubHappyPath()
        coEvery { authApi.registerWallet(any()) } returns tokenSuccess()
        coEvery { store.save(any()) } throws IllegalStateException("DataStore I/O")

        val result = registrar.register(WALLET_ID, mobileSigner)

        assertThat(result.leftOrNull()).isInstanceOf(WalletRegistrationError.PersistenceFailed::class.java)
        // Marker must stay unset so the next attempt retries cleanly.
        assertThat(registeredIds()).doesNotContain(WALLET_ID)
    }

    private fun stubHappyPath() {
        coEvery { deviceKeyManager.getPublicKey() } returns Some(ByteArray(65))
        coEvery { authApi.requestWalletNonce(any()) } returns nonceSuccess()
        coEvery { nonceDecryptor.decryptNonce("abc") } returns "decrypted"
    }

    private fun nonceSuccess() = ApiResponse.Success(
        data = NonceApiResponse(cipheredNonce = "abc", expiresAt = "2024-01-01T00:00:00Z"),
    )

    private fun tokenSuccess() = ApiResponse.Success(
        data = TokenApiResponse(
            accessToken = "fresh-access",
            accessTokenExpiresAt = "2024-01-01T00:00:00Z",
            refreshToken = "fresh-rt",
            refreshTokenExpiresAt = "2024-02-01T00:00:00Z",
            walletIds = listOf(WALLET_ID),
        ),
    )

    private fun registeredIds(): Set<String> =
        preferencesDataStore.current()[PreferencesKeys.REGISTERED_WALLET_IDS_KEY].orEmpty()

    private fun base64(bytes: ByteArray): String = java.util.Base64.getEncoder().encodeToString(bytes)

    private companion object {
        const val WALLET_ID = "wallet-1"
        const val OTHER_WALLET_ID = "wallet-2"
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
package com.tangem.lib.auth.social.internal

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.lib.auth.api.AuthApi
import com.tangem.lib.auth.api.models.response.NonceApiResponse
import com.tangem.lib.auth.api.models.response.TokenApiResponse
import com.tangem.lib.auth.attestation.AttestationProvider
import com.tangem.lib.auth.devicekey.DeviceKeyManager
import com.tangem.lib.auth.nonce.AuthNonceDecryptor
import com.tangem.lib.auth.session.SessionTokens
import com.tangem.lib.auth.session.SessionTokensStore
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.WalletRegistrationError
import com.tangem.lib.auth.session.WalletSignatureBundle
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.lib.auth.session.internal.AuthErrorConverter
import com.tangem.lib.auth.session.internal.WalletRegistrationLocks
import com.tangem.lib.auth.session.internal.SignedRequestPayload
import com.tangem.lib.auth.social.HpkeRecoveryCipher
import com.tangem.lib.auth.social.RecoveryKeyRsaCipher
import com.tangem.lib.auth.social.SignInNonce
import com.tangem.lib.auth.social.SocialRecoveryError
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import com.tangem.utils.info.AppInfoProvider
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import java.util.Base64 as JavaBase64

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSocialRecoveryClientTest {

    private val api: SocialRecoveryApi = mockk()
    private val authApi: AuthApi = mockk()
    private val deviceKeyManager: DeviceKeyManager = mockk()
    private val nonceDecryptor: AuthNonceDecryptor = mockk()
    private val attestationProvider: AttestationProvider = mockk()
    private val sessionTokensStore: SessionTokensStore = mockk(relaxUnitFun = true)
    private val walletRegistrar: WalletRegistrar = mockk()
    private val appInfoProvider: AppInfoProvider = mockk(relaxed = true)
    private val signedRequestPayload = SignedRequestPayload(appInfoProvider)
    private val locks = WalletRegistrationLocks()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val kmsKeyPair = KeyPairGenerator.getInstance("RSA")
        .apply { initialize(RSA_KEY_SIZE_BITS) }
        .generateKeyPair()

    private val client = DefaultSocialRecoveryClient(
        api = api,
        authApi = authApi,
        deviceKeyManager = deviceKeyManager,
        nonceDecryptor = nonceDecryptor,
        signedRequestPayload = signedRequestPayload,
        attestationProvider = attestationProvider,
        errorConverter = AuthErrorConverter(),
        sessionTokensStore = sessionTokensStore,
        walletRegistrar = walletRegistrar,
        locks = locks,
        dispatchers = dispatchers,
    )

    private val signer = WalletSigner {
        WalletSignatureBundle(
            walletSignature = ByteArray(size = 65) { 1 },
            walletSignatureSalt = ByteArray(size = 16) { 2 },
            cardSignature = null,
            cardSignatureSalt = null,
            walletStatusByte = null,
        )
    }

    @BeforeEach
    fun setup() {
        clearMocks(api, authApi, deviceKeyManager, nonceDecryptor, attestationProvider, sessionTokensStore)
        // the store answers with the set that ends up in effect
        coEvery { sessionTokensStore.save(any()) } answers { firstArg() }
        mockkAndroidBase64()
        coEvery { deviceKeyManager.getPublicKeyEncoded() } returns Some(ByteArray(size = 65))
        coEvery { deviceKeyManager.signDer(any()) } returns ByteArray(size = 70) { 9 }
        coEvery { nonceDecryptor.decryptNonce(CIPHERED_NONCE) } returns NONCE
        coEvery { attestationProvider.getAttestationToken(any()) } returns null
        coEvery { sessionTokensStore.get() } returns Some(sessionTokens())
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `GIVEN the service issues a nonce WHEN requestSignInNonce THEN the provider gets its digest`() = runTest {
        // Arrange
        coEvery { api.requestRecoverNonce(any()) } returns nonceSuccess()
        val digest = MessageDigest.getInstance("SHA-256").digest(NONCE.toByteArray(Charsets.UTF_8))

        // Act
        val result = client.requestSignInNonce()

        // Assert
        val expected = SignInNonce(
            forProvider = JavaBase64.getEncoder().encodeToString(digest),
            forRequest = NONCE,
        )
        assertThat(result.getOrNull()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN the backend sealed a key WHEN recoverKey THEN the plaintext key is returned`() = runTest {
        // Arrange
        val storedKey = ByteArray(size = 32) { it.toByte() }
        val request = stubRecoverSealing(storedKey)

        // Act
        val result = client.recoverKey(nonce = NONCE, providerIdToken = ID_TOKEN)

        // Assert
        assertThat(result.getOrNull()).isEqualTo(storedKey)
        assertThat(request.captured.nonce).isEqualTo(NONCE)
        assertThat(request.captured.providerIdToken).isEqualTo(ID_TOKEN)
        assertThat(request.captured.appAttestKeyId).isNull()
    }

    @Test
    fun `GIVEN an attestation token WHEN recoverKey THEN the signed payload matches the canonical form`() = runTest {
        // Arrange
        coEvery { attestationProvider.getAttestationToken(NONCE) } returns ATTESTATION_TOKEN
        val request = stubRecoverSealing(ByteArray(size = 32))
        val signed = slot<ByteArray>()
        coEvery { deviceKeyManager.signDer(capture(signed)) } returns ByteArray(size = 70) { 9 }

        // Act
        val result = client.recoverKey(nonce = NONCE, providerIdToken = ID_TOKEN)

        // Assert — the empty segment stands for the iOS-only appAttestKeyId, and there is no trailing colon
        val metadata = signedRequestPayload.deviceMetadata
        val expected = listOf(
            NONCE,
            request.captured.recoveryEncryptionPublicKey,
            ID_TOKEN,
            ATTESTATION_TOKEN,
            "",
            metadata.deviceModel,
            metadata.os,
            metadata.osVersion,
            metadata.appVersion,
            metadata.userAgent,
            metadata.locale,
            metadata.timezone,
        ).joinToString(separator = ":")
        assertThat(result.isRight()).isTrue()
        assertThat(signed.captured.toString(Charsets.UTF_8)).isEqualTo(expected)
    }

    @Test
    fun `GIVEN the identity has no wallet WHEN recoverKey THEN BindingNotFound is returned`() = runTest {
        // Arrange
        coEvery { api.recoverWallet(any()) } returns httpError(
            code = ApiResponseError.HttpException.Code.NOT_FOUND,
            message = "no binding for this identity",
        )

        // Act
        val result = client.recoverKey(nonce = NONCE, providerIdToken = ID_TOKEN)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(SocialRecoveryError.BindingNotFound)
    }

    @Test
    fun `GIVEN the identity points at another wallet WHEN recoverKey THEN BindingConflict is returned`() = runTest {
        // Arrange
        coEvery { api.recoverWallet(any()) } returns httpError(
            code = ApiResponseError.HttpException.Code.CONFLICT,
            message = "identity already bound",
        )

        // Act
        val result = client.recoverKey(nonce = NONCE, providerIdToken = ID_TOKEN)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(SocialRecoveryError.BindingConflict)
    }

    @Test
    fun `GIVEN no session WHEN recoverKey THEN NoActiveSession is returned and no nonce is consumed`() = runTest {
        // Arrange
        coEvery { sessionTokensStore.get() } returns None

        // Act
        val result = client.recoverKey(nonce = NONCE, providerIdToken = ID_TOKEN)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(SocialRecoveryError.NoActiveSession)
        coVerify(exactly = 0) { api.recoverWallet(any()) }
    }

    @Test
    fun `GIVEN a malformed envelope WHEN recoverKey THEN DecryptionFailed is returned`() = runTest {
        // Arrange
        coEvery { api.recoverWallet(any()) } returns ApiResponse.Success(
            data = WalletRecoverApiResponse(enc = "not base64!", ct = "not base64!"),
        )

        // Act
        val result = client.recoverKey(nonce = NONCE, providerIdToken = ID_TOKEN)

        // Assert
        assertThat(result.leftOrNull()).isInstanceOf(SocialRecoveryError.DecryptionFailed::class.java)
    }

    @Test
    fun `GIVEN the KMS key WHEN registerBinding THEN the sealed key is the one the KMS can open`() = runTest {
        // Arrange
        val recoveryKey = ByteArray(size = 32) { it.toByte() }
        stubRegistrationNonce()
        coEvery { api.getRecoveryPublicKey() } returns kmsKeySuccess(RecoveryKeyRsaCipher.SUPPORTED_ALGORITHM)
        val request = slot<SocialWalletRegistrationApiRequest>()
        coEvery { api.registerWalletBinding(capture(request)) } returns tokenSuccess()

        // Act
        val result = client.registerBinding(
            walletId = WALLET_ID,
            providerIdToken = ID_TOKEN,
            recoveryKey = recoveryKey,
            signer = signer,
        )

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(request.captured.walletId).isEqualTo(WALLET_ID)
        assertThat(request.captured.nonce).isEqualTo(NONCE)
        val sealed = JavaBase64.getDecoder().decode(request.captured.encryptedRecoveryKey)
        assertThat(decryptWithKms(sealed)).isEqualTo(recoveryKey)
        coVerify(exactly = 1) { sessionTokensStore.save(any()) }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["RSA-OAEP-512"])
    fun `GIVEN a KMS key not for the supported algorithm WHEN registerBinding THEN nothing is registered`(
        algorithm: String?,
    ) = runTest {
        // Arrange
        stubRegistrationNonce()
        coEvery { api.getRecoveryPublicKey() } returns kmsKeySuccess(algorithm = algorithm)

        // Act
        val result = client.registerBinding(
            walletId = WALLET_ID,
            providerIdToken = ID_TOKEN,
            recoveryKey = ByteArray(size = 32),
            signer = signer,
        )

        // Assert
        assertThat(result.leftOrNull()).isInstanceOf(SocialRecoveryError.Unknown::class.java)
        coVerify(exactly = 0) { api.registerWalletBinding(any()) }
    }

    @Test
    fun `GIVEN another call is rotating the session WHEN registerBinding THEN it waits for that call`() = runTest {
        // Arrange
        stubRegistrationNonce()
        coEvery { api.getRecoveryPublicKey() } returns kmsKeySuccess(RecoveryKeyRsaCipher.SUPPORTED_ALGORITHM)
        coEvery { api.registerWalletBinding(any()) } returns tokenSuccess()
        locks.sessionRotation.lock()

        // Act
        val registration = launch {
            client.registerBinding(
                walletId = WALLET_ID,
                providerIdToken = ID_TOKEN,
                recoveryKey = ByteArray(size = 32),
                signer = signer,
            )
        }
        runCurrent()

        // Assert — the wallet registration holding the lock must finish its own token write first
        coVerify(exactly = 0) { api.registerWalletBinding(any()) }

        locks.sessionRotation.unlock()
        registration.join()
        coVerify(exactly = 1) { api.registerWalletBinding(any()) }
        coVerify(exactly = 1) { sessionTokensStore.save(any()) }
    }

    @Test
    fun `GIVEN the wallet is being registered WHEN registerBinding THEN no second nonce is requested`() = runTest {
        // Arrange — the plain registrar holds the per-wallet lock for this very wallet
        stubRegistrationNonce()
        coEvery { api.getRecoveryPublicKey() } returns kmsKeySuccess(RecoveryKeyRsaCipher.SUPPORTED_ALGORITHM)
        coEvery { api.registerWalletBinding(any()) } returns tokenSuccess()
        locks.forWallet(WALLET_ID).lock()

        // Act
        val binding = launch {
            client.registerBinding(
                walletId = WALLET_ID,
                providerIdToken = ID_TOKEN,
                recoveryKey = ByteArray(size = 32),
                signer = signer,
            )
        }
        runCurrent()

        // Assert — both callers post to /wallet/register, so the wallet nonce must not be consumed twice
        coVerify(exactly = 0) { authApi.requestWalletNonce(any()) }

        locks.forWallet(WALLET_ID).unlock()
        binding.join()
        coVerify(exactly = 1) { authApi.requestWalletNonce(any()) }
    }

    @Test
    fun `GIVEN the rotated tokens cannot be stored WHEN registerBinding THEN the call fails`() = runTest {
        // Arrange
        stubRegistrationNonce()
        coEvery { api.getRecoveryPublicKey() } returns kmsKeySuccess(RecoveryKeyRsaCipher.SUPPORTED_ALGORITHM)
        coEvery { api.registerWalletBinding(any()) } returns tokenSuccess()
        coEvery { sessionTokensStore.save(any()) } throws IllegalStateException("storage is full")

        // Act
        val result = client.registerBinding(
            walletId = WALLET_ID,
            providerIdToken = ID_TOKEN,
            recoveryKey = ByteArray(size = 32),
            signer = signer,
        )

        // Assert — reporting success would leave the app holding the superseded session
        assertThat(result.leftOrNull()).isInstanceOf(SocialRecoveryError.Unknown::class.java)
    }

    @Test
    fun `GIVEN authentication is toggled off WHEN unbindWallet THEN the teardown is not reported as done`() =
        runTest {
            // Arrange
            coEvery { walletRegistrar.unregister(WALLET_ID) } returns Either.Left(WalletRegistrationError.Disabled)

            // Act
            val result = client.unbindWallet(WALLET_ID)

            // Assert — a binding made before the toggle flipped is still on the server
            assertThat(result.leftOrNull()).isEqualTo(SocialRecoveryError.NoActiveSession)
        }

    /** Answers the recover call the way the backend would: sealing [storedKey] to the request's own key */
    private fun stubRecoverSealing(storedKey: ByteArray): CapturingSlot<WalletRecoverApiRequest> {
        val request = slot<WalletRecoverApiRequest>()
        coEvery { api.recoverWallet(capture(request)) } answers {
            val envelope = HpkeRecoveryCipher.seal(
                recipientPublicKey = JavaBase64.getDecoder().decode(request.captured.recoveryEncryptionPublicKey),
                plaintext = storedKey,
            )
            ApiResponse.Success(
                data = WalletRecoverApiResponse(
                    enc = JavaBase64.getEncoder().encodeToString(envelope.enc),
                    ct = JavaBase64.getEncoder().encodeToString(envelope.ct),
                ),
            )
        }
        return request
    }

    private fun stubRegistrationNonce() {
        coEvery { authApi.requestWalletNonce(any()) } returns nonceSuccess()
    }

    private fun mockkAndroidBase64() {
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            JavaBase64.getEncoder().encodeToString(firstArg())
        }
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            val decoder = if (secondArg<Int>() and android.util.Base64.URL_SAFE != 0) {
                JavaBase64.getUrlDecoder()
            } else {
                JavaBase64.getDecoder()
            }
            decoder.decode(firstArg<String>())
        }
    }

    private fun decryptWithKms(ciphertext: ByteArray): ByteArray = Cipher
        .getInstance("RSA/ECB/OAEPPadding")
        .run {
            init(
                Cipher.DECRYPT_MODE,
                kmsKeyPair.private as RSAPrivateKey,
                OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT),
            )
            doFinal(ciphertext)
        }

    private fun kmsKeySuccess(algorithm: String?) = ApiResponse.Success(
        data = RecoveryPublicKeyApiResponse(
            modulus = (kmsKeyPair.public as RSAPublicKey).modulus.toBase64Url(),
            exponent = (kmsKeyPair.public as RSAPublicKey).publicExponent.toBase64Url(),
            algorithm = algorithm,
        ),
    )

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> httpError(code: ApiResponseError.HttpException.Code, message: String): ApiResponse<T> =
        ApiResponse.Error(
            cause = ApiResponseError.HttpException(code = code, message = message, errorBody = null),
        ) as ApiResponse<T>

    private fun nonceSuccess() = ApiResponse.Success(
        data = NonceApiResponse(cipheredNonce = CIPHERED_NONCE, expiresAt = "2024-01-01T00:00:00Z"),
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

    /** JWK numbers are unsigned big-endian, so a leading zero sign byte must be dropped */
    private fun BigInteger.toBase64Url(): String = JavaBase64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(toByteArray().dropWhile { it == 0.toByte() }.toByteArray())

    private fun sessionTokens() = SessionTokens(
        accessToken = "access",
        accessTokenExpiresAt = Instant.DISTANT_FUTURE,
        refreshToken = "refresh",
        refreshTokenExpiresAt = Instant.DISTANT_FUTURE,
        walletIds = emptyList(),
    )

    private companion object {
        const val RSA_KEY_SIZE_BITS = 2048
        const val WALLET_ID = "wallet-1"
        const val ID_TOKEN = "provider-id-token"
        const val ATTESTATION_TOKEN = "attestation-token"
        const val CIPHERED_NONCE = "ciphered"
        const val NONCE = "bm9uY2Ux"
    }
}
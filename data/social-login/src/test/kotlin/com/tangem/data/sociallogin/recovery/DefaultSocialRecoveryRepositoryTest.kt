package com.tangem.data.sociallogin.recovery

import android.util.Base64
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.sociallogin.models.RecoveryKey
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.session.WalletSigner
import com.tangem.lib.auth.session.WalletSignerProvider
import com.tangem.lib.auth.social.SocialRecoveryClient
import com.tangem.lib.auth.social.SocialRecoveryError
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.util.Base64 as JavaBase64

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSocialRecoveryRepositoryTest {

    private val client: SocialRecoveryClient = mockk()
    private val walletSignerProvider: WalletSignerProvider = mockk()

    private val repository = DefaultSocialRecoveryRepository(
        client = client,
        walletSignerProvider = walletSignerProvider,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(client, walletSignerProvider)
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers {
            JavaBase64.getEncoder().encodeToString(firstArg())
        }
    }

    @AfterEach
    fun tearDown() = unmockkAll()

    @Test
    fun `GIVEN the backend returns a key WHEN getRecoveryKey THEN it is wrapped as the recovery key`() = runTest {
        // Arrange
        val keyBytes = ByteArray(RecoveryKey.SIZE_BYTES) { it.toByte() }
        coEvery { client.recoverKey(nonce = NONCE, providerIdToken = ID_TOKEN_VALUE) } returns keyBytes.right()

        // Act
        val actual = repository.getRecoveryKey(ID_TOKEN)

        // Assert
        assertThat(actual).isEqualTo(RecoveryKey(keyBytes).right())
    }

    @Test
    fun `GIVEN the envelope opened into junk WHEN getRecoveryKey THEN decryption is reported as failed`() = runTest {
        // Arrange — anything but 32 bytes means what came out of the envelope is not K
        coEvery { client.recoverKey(any(), any()) } returns ByteArray(size = 8).right()

        // Act
        val actual = repository.getRecoveryKey(ID_TOKEN)

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(SocialLoginError.RecoveryKeyDecryptionFailed::class.java)
    }

    @Test
    fun `GIVEN a hot wallet WHEN registerRecoveryKey THEN the wallet is addressed by its base64 id`() = runTest {
        // Arrange
        val signer: WalletSigner = mockk()
        coEvery { walletSignerProvider.signerFor(BACKEND_WALLET_ID) } returns signer
        coEvery { client.registerBinding(any(), any(), any(), any()) } returns Unit.right()

        // Act
        val actual = repository.registerRecoveryKey(
            idToken = ID_TOKEN,
            walletId = WALLET_ID,
            recoveryKey = RECOVERY_KEY,
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            client.registerBinding(
                walletId = BACKEND_WALLET_ID,
                providerIdToken = ID_TOKEN_VALUE,
                recoveryKey = RECOVERY_KEY.value,
                signer = signer,
            )
        }
    }

    @Test
    fun `GIVEN no signer for the wallet WHEN registerRecoveryKey THEN nothing is bound`() = runTest {
        // Arrange — a cold wallet, or one this device cannot sign for
        coEvery { walletSignerProvider.signerFor(any()) } returns null

        // Act
        val actual = repository.registerRecoveryKey(
            idToken = ID_TOKEN,
            walletId = WALLET_ID,
            recoveryKey = RECOVERY_KEY,
        )

        // Assert — a local shortfall, not a rejection by a backend that was never called
        assertThat(actual).isEqualTo(SocialLoginError.Unknown().left())
        coVerify(exactly = 0) { client.registerBinding(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN the wallet id WHEN unbindWallet THEN the backend id is passed through`() = runTest {
        // Arrange
        coEvery { client.unbindWallet(any()) } returns Unit.right()

        // Act
        val actual = repository.unbindWallet(WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) { client.unbindWallet(BACKEND_WALLET_ID) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN the transport fails WHEN getRecoveryKey THEN the error is translated`(model: ErrorModel) = runTest {
        // Arrange
        coEvery { client.recoverKey(any(), any()) } returns model.transportError.left()

        // Act
        val actual = repository.getRecoveryKey(ID_TOKEN)

        // Assert
        assertThat(actual).isEqualTo(model.expected.left())
    }

    data class ErrorModel(val transportError: SocialRecoveryError, val expected: SocialLoginError)

    private fun provideTestModels(): List<ErrorModel> {
        val cause = IllegalStateException("boom")

        return listOf(
            ErrorModel(SocialRecoveryError.BindingNotFound, SocialLoginError.BindingNotFound),
            ErrorModel(SocialRecoveryError.BindingConflict, SocialLoginError.BindingConflict),
            ErrorModel(SocialRecoveryError.NoActiveSession, SocialLoginError.ServerError()),
            ErrorModel(SocialRecoveryError.DeviceKeyUnavailable, SocialLoginError.ServerError()),
            ErrorModel(SocialRecoveryError.SigningFailed(cause), SocialLoginError.ServerError(cause)),
            ErrorModel(
                SocialRecoveryError.DecryptionFailed(cause),
                SocialLoginError.RecoveryKeyDecryptionFailed(cause),
            ),
            ErrorModel(SocialRecoveryError.Api(AuthError.NetworkError), SocialLoginError.NetworkError),
            ErrorModel(SocialRecoveryError.Api(AuthError.Unknown(cause)), SocialLoginError.ServerError()),
            ErrorModel(SocialRecoveryError.Unknown(cause), SocialLoginError.Unknown(cause)),
        )
    }

    private companion object {
        const val NONCE = "server-nonce"
        const val ID_TOKEN_VALUE = "provider-id-token"
        val ID_TOKEN = SocialProviderIdToken(value = ID_TOKEN_VALUE, nonce = NONCE)
        val WALLET_ID = UserWalletId(value = ByteArray(size = 32) { it.toByte() })
        val BACKEND_WALLET_ID: String = JavaBase64.getEncoder().encodeToString(WALLET_ID.value)
        val RECOVERY_KEY = RecoveryKey(ByteArray(RecoveryKey.SIZE_BYTES) { 7 })
    }
}
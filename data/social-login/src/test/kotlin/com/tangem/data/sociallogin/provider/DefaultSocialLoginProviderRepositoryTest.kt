package com.tangem.data.sociallogin.provider

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.google.auth.GoogleIdToken
import com.tangem.google.auth.GoogleIdTokenError
import com.tangem.google.auth.GoogleIdTokenProvider
import com.tangem.google.drive.GoogleDriveTokenProvider
import com.tangem.lib.auth.session.AuthError
import com.tangem.lib.auth.social.SignInNonce
import com.tangem.lib.auth.social.SocialRecoveryError
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSocialLoginProviderRepositoryTest {

    private val googleIdTokenProvider: GoogleIdTokenProvider = mockk()
    private val appDataTokenProvider: GoogleDriveTokenProvider = mockk(relaxed = true)
    private val nonceProvider: SocialLoginNonceProvider = mockk()

    private val repository = DefaultSocialLoginProviderRepository(
        googleIdTokenProvider = googleIdTokenProvider,
        appDataTokenProvider = appDataTokenProvider,
        nonceProvider = nonceProvider,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(googleIdTokenProvider, appDataTokenProvider, nonceProvider)
        coEvery { nonceProvider.getNonce() } returns NONCE.right()
    }

    @Test
    fun `GIVEN google sign-in available WHEN getAvailableProviders THEN google is offered`() = runTest {
        // Arrange
        coEvery { googleIdTokenProvider.isAvailable() } returns true

        // Act
        val actual = repository.getAvailableProviders()

        // Assert
        assertThat(actual).containsExactly(SocialLoginProvider.Google)
    }

    @Test
    fun `GIVEN google sign-in unavailable WHEN getAvailableProviders THEN nothing is offered`() = runTest {
        // Arrange
        coEvery { googleIdTokenProvider.isAvailable() } returns false

        // Act
        val actual = repository.getAvailableProviders()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN sign-in succeeds WHEN signIn THEN id token is minted with the hashed nonce`() = runTest {
        // Arrange
        coEvery { googleIdTokenProvider.getIdToken(HASHED_NONCE) } returns ID_TOKEN.right()

        // Act
        val actual = repository.signIn(SocialLoginProvider.Google)

        // Assert
        assertThat(actual).isEqualTo(SocialProviderIdToken(value = ID_TOKEN.value, nonce = PLAIN_NONCE).right())
        coVerify(exactly = 1) { googleIdTokenProvider.getIdToken(HASHED_NONCE) }
    }

    @Test
    fun `GIVEN sign-in succeeds WHEN signIn THEN the drive authorization moves to the same account`() = runTest {
        // Arrange — a token cached for the account of a previous attempt would outlive the new pin
        coEvery { googleIdTokenProvider.getIdToken(HASHED_NONCE) } returns ID_TOKEN.right()

        // Act
        repository.signIn(SocialLoginProvider.Google)

        // Assert
        coVerify(exactly = 1) { appDataTokenProvider.switchAccount(ACCOUNT_NAME) }
    }

    @Test
    fun `GIVEN sign-in fails WHEN signIn THEN no account is pinned`() = runTest {
        // Arrange
        coEvery { googleIdTokenProvider.getIdToken(HASHED_NONCE) } returns GoogleIdTokenError.Canceled.left()

        // Act
        repository.signIn(SocialLoginProvider.Google)

        // Assert
        coVerify(exactly = 0) { appDataTokenProvider.switchAccount(any()) }
    }

    @ParameterizedTest
    @MethodSource("provideNonceErrorModels")
    fun `GIVEN the nonce request fails WHEN signIn THEN the transport error is kept`(model: NonceErrorModel) =
        runTest {
            // Arrange
            coEvery { nonceProvider.getNonce() } returns model.clientError.left()

            // Act
            val actual = repository.signIn(SocialLoginProvider.Google)

            // Assert
            assertThat(actual).isEqualTo(model.expected.left())
            coVerify(exactly = 0) { googleIdTokenProvider.getIdToken(any()) }
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN sign-in fails WHEN signIn THEN error is mapped`(model: ErrorModel) = runTest {
        // Arrange
        coEvery { googleIdTokenProvider.getIdToken(HASHED_NONCE) } returns model.providerError.left()

        // Act
        val actual = repository.signIn(SocialLoginProvider.Google)

        // Assert
        assertThat(actual).isEqualTo(model.expected.left())
    }

    @Test
    fun `GIVEN google provider WHEN signOut THEN credential state and the cached Drive token are cleared`() = runTest {
        // Arrange
        coEvery { googleIdTokenProvider.signOut() } returns Unit

        // Act
        repository.signOut(SocialLoginProvider.Google)

        // Assert
        coVerify(exactly = 1) { googleIdTokenProvider.signOut() }
        // invalidate() drops the pinned account too, so a retry cannot reuse the abandoned one
        coVerify(exactly = 1) { appDataTokenProvider.invalidate() }
    }

    data class ErrorModel(val providerError: GoogleIdTokenError, val expected: SocialLoginError)

    data class NonceErrorModel(val clientError: SocialRecoveryError, val expected: SocialLoginError)

    private fun provideNonceErrorModels(): List<NonceErrorModel> = listOf(
        NonceErrorModel(SocialRecoveryError.Api(AuthError.NetworkError), SocialLoginError.NetworkError),
        NonceErrorModel(
            clientError = SocialRecoveryError.Api(AuthError.ServerUnavailable(problem = null)),
            expected = SocialLoginError.ServerError(),
        ),
        NonceErrorModel(SocialRecoveryError.DeviceKeyUnavailable, SocialLoginError.ServerError()),
    )

    private fun provideTestModels(): List<ErrorModel> {
        val cause = IllegalStateException("unexpected")

        return listOf(
            ErrorModel(GoogleIdTokenError.Canceled, SocialLoginError.SignInCanceled),
            ErrorModel(GoogleIdTokenError.NoCredential, SocialLoginError.NoProviderAccount),
            ErrorModel(GoogleIdTokenError.Unavailable, SocialLoginError.ProviderUnavailable),
            ErrorModel(GoogleIdTokenError.Unknown(cause), SocialLoginError.SignInFailed(cause)),
        )
    }

    private companion object {
        const val PLAIN_NONCE = "server-nonce"
        const val HASHED_NONCE = "hashed-server-nonce"
        val NONCE = SignInNonce(forProvider = HASHED_NONCE, forRequest = PLAIN_NONCE)
        const val ACCOUNT_NAME = "user@gmail.com"
        val ID_TOKEN = GoogleIdToken(value = "google-id-token", accountName = ACCOUNT_NAME)
    }
}
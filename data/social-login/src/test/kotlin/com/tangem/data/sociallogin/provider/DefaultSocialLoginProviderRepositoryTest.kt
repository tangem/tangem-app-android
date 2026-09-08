package com.tangem.data.sociallogin.provider

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.sociallogin.models.SocialProviderIdToken
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.google.auth.GoogleIdTokenError
import com.tangem.google.auth.GoogleIdTokenProvider
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultSocialLoginProviderRepositoryTest {

    private val googleIdTokenProvider: GoogleIdTokenProvider = mockk()
    private val nonceProvider: SocialLoginNonceProvider = mockk()

    private val repository = DefaultSocialLoginProviderRepository(
        googleIdTokenProvider = googleIdTokenProvider,
        nonceProvider = nonceProvider,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(googleIdTokenProvider, nonceProvider)
        coEvery { nonceProvider.getNonce() } returns NONCE
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
    fun `GIVEN sign-in succeeds WHEN signIn THEN id token requested with the server nonce`() = runTest {
        // Arrange
        coEvery { googleIdTokenProvider.getIdToken(NONCE) } returns ID_TOKEN.right()

        // Act
        val actual = repository.signIn(SocialLoginProvider.Google)

        // Assert
        assertThat(actual).isEqualTo(SocialProviderIdToken(ID_TOKEN).right())
        coVerify(exactly = 1) { googleIdTokenProvider.getIdToken(NONCE) }
    }

    @Test
    fun `GIVEN backend issues no nonce WHEN signIn THEN id token requested unbound`() = runTest {
        // Arrange
        coEvery { nonceProvider.getNonce() } returns null
        coEvery { googleIdTokenProvider.getIdToken(null) } returns ID_TOKEN.right()

        // Act
        repository.signIn(SocialLoginProvider.Google)

        // Assert
        coVerify(exactly = 1) { googleIdTokenProvider.getIdToken(null) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN sign-in fails WHEN signIn THEN error is mapped`(model: ErrorModel) = runTest {
        // Arrange
        coEvery { googleIdTokenProvider.getIdToken(NONCE) } returns model.providerError.left()

        // Act
        val actual = repository.signIn(SocialLoginProvider.Google)

        // Assert
        assertThat(actual).isEqualTo(model.expected.left())
    }

    @Test
    fun `GIVEN google provider WHEN signOut THEN credential state is cleared`() = runTest {
        // Arrange
        coEvery { googleIdTokenProvider.signOut() } returns Unit

        // Act
        repository.signOut(SocialLoginProvider.Google)

        // Assert
        coVerify(exactly = 1) { googleIdTokenProvider.signOut() }
    }

    data class ErrorModel(val providerError: GoogleIdTokenError, val expected: SocialLoginError)

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
        const val NONCE = "server-nonce"
        const val ID_TOKEN = "google-id-token"
    }
}
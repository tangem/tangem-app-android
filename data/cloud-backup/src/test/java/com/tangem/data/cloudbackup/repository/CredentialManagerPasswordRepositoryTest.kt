package com.tangem.data.cloudbackup.repository

import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CreateCredentialRequest
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialNoCreateOptionException
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.utils.ForegroundActivityProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CredentialManagerPasswordRepositoryTest {

    private val credentialManager: CredentialManager = mockk()
    private val activity: AppCompatActivity = mockk()
    private val foregroundActivityProvider: ForegroundActivityProvider = mockk()

    private val repository = CredentialManagerPasswordRepository(
        credentialManager = credentialManager,
        foregroundActivityProvider = foregroundActivityProvider,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(credentialManager, foregroundActivityProvider)
        every { foregroundActivityProvider.foregroundActivity } returns activity
    }

    @Test
    fun `GIVEN provider stores the credential WHEN savePassword THEN true AND request carries the username`() =
        runTest {
            // Arrange
            val request = slot<CreateCredentialRequest>()
            coEvery { credentialManager.createCredential(activity, capture(request)) } returns mockk()

            // Act
            val actual = repository.savePassword(username = "My Wallet (1)", password = PASSWORD.toCharArray())

            // Assert
            assertThat(actual).isTrue()
            val passwordRequest = request.captured as CreatePasswordRequest
            assertThat(passwordRequest.id).isEqualTo("My Wallet (1)")
            assertThat(passwordRequest.password).isEqualTo(PASSWORD)
        }

    @Test
    fun `GIVEN the user cancels WHEN savePassword THEN false`() = runTest {
        // Arrange
        coEvery {
            credentialManager.createCredential(any(), any<CreateCredentialRequest>())
        } throws CreateCredentialCancellationException()

        // Act
        val actual = repository.savePassword(username = "Wallet", password = PASSWORD.toCharArray())

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN no credential provider WHEN savePassword THEN false`() = runTest {
        // Arrange
        coEvery {
            credentialManager.createCredential(any(), any<CreateCredentialRequest>())
        } throws CreateCredentialNoCreateOptionException()

        // Act
        val actual = repository.savePassword(username = "Wallet", password = PASSWORD.toCharArray())

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN the provider fails outside the credentials contract WHEN savePassword THEN false`() = runTest {
        // Arrange
        coEvery {
            credentialManager.createCredential(any(), any<CreateCredentialRequest>())
        } throws IllegalStateException("Google services are unavailable")

        // Act
        val actual = repository.savePassword(username = "Wallet", password = PASSWORD.toCharArray())

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN no foreground activity WHEN savePassword THEN false AND no request is made`() = runTest {
        // Arrange
        every { foregroundActivityProvider.foregroundActivity } returns null

        // Act
        val actual = repository.savePassword(username = "Wallet", password = PASSWORD.toCharArray())

        // Assert
        assertThat(actual).isFalse()
        coVerify(exactly = 0) { credentialManager.createCredential(any(), any<CreateCredentialRequest>()) }
    }

    private companion object {
        const val PASSWORD = "Str0ng!Pass"
    }
}
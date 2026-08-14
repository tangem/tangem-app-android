package com.tangem.data.cloudbackup.datasource

import android.content.Context
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.cloudbackup.models.CloudBackupError
import com.tangem.google.GoogleServicesHelper
import com.tangem.test.core.ProvideTestModels
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultGoogleDriveTokenProviderTest {

    private val authorizer: GoogleDriveAuthorizer = mockk()
    private val api: GoogleDriveApi = mockk(relaxed = true)
    private val context: Context = mockk()

    private lateinit var provider: DefaultGoogleDriveTokenProvider

    @BeforeEach
    fun setUp() {
        clearMocks(authorizer, api)
        mockkObject(GoogleServicesHelper)
        every { GoogleServicesHelper.checkGoogleServicesAvailability(any()) } returns true
        provider = DefaultGoogleDriveTokenProvider(
            authorizer = authorizer,
            api = api,
            context = context,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(GoogleServicesHelper)
    }

    @Test
    fun `GIVEN play services unavailable WHEN getAccessToken THEN CloudUnavailable and no authorize`() = runTest {
        // Arrange
        every { GoogleServicesHelper.checkGoogleServicesAvailability(any()) } returns false

        // Act
        val actual = provider.getAccessToken()

        // Assert
        assertThat(actual).isEqualTo(CloudBackupError.CloudUnavailable.left())
        coVerify(exactly = 0) { authorizer.authorize() }
    }

    @Test
    fun `GIVEN authorized WHEN getAccessToken twice THEN authorized once AND second call returns cached`() = runTest {
        // Arrange
        coEvery { authorizer.authorize() } returns GoogleDriveAuthResult(TOKEN).right()

        // Act
        val first = provider.getAccessToken()
        val second = provider.getAccessToken()

        // Assert
        assertThat(first).isEqualTo(TOKEN.right())
        assertThat(second).isEqualTo(TOKEN.right())
        coVerify(exactly = 1) { authorizer.authorize() }
    }

    @Test
    fun `GIVEN silent request WHEN getAccessToken THEN authorize called non-interactively`() = runTest {
        // Arrange
        coEvery { authorizer.authorize(interactive = false) } returns CloudBackupError.AuthRequired.left()

        // Act
        val actual = provider.getAccessToken(interactive = false)

        // Assert
        assertThat(actual).isEqualTo(CloudBackupError.AuthRequired.left())
        coVerify(exactly = 1) { authorizer.authorize(interactive = false) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN authorize fails WHEN getAccessToken THEN error propagated AND nothing cached`(error: CloudBackupError) =
        runTest {
            // Arrange
            coEvery { authorizer.authorize() } returns error.left()

            // Act
            val actual = provider.getAccessToken()
            provider.getAccessToken()

            // Assert — a failed authorization is not cached, so the next call authorizes again
            assertThat(actual).isEqualTo(error.left())
            coVerify(exactly = 2) { authorizer.authorize() }
        }

    @Test
    fun `GIVEN cached token WHEN invalidate THEN token dropped AND re-auth on next call`() = runTest {
        // Arrange
        coEvery { authorizer.authorize() } returns GoogleDriveAuthResult(TOKEN).right()
        coEvery { authorizer.clearToken(TOKEN) } just Runs
        every { authorizer.clearAuthorization() } just Runs
        provider.getAccessToken()

        // Act
        provider.invalidate()
        provider.getAccessToken()

        // Assert
        coVerify(exactly = 1) { authorizer.clearToken(TOKEN) }
        verify(exactly = 1) { authorizer.clearAuthorization() }
        coVerify(exactly = 2) { authorizer.authorize() }
    }

    @Test
    fun `GIVEN cached token WHEN signOut THEN token revoked AND dropped AND state cleared`() = runTest {
        // Arrange
        coEvery { authorizer.authorize() } returns GoogleDriveAuthResult(TOKEN).right()
        coEvery { authorizer.clearToken(TOKEN) } just Runs
        every { authorizer.clearAuthorization() } just Runs
        provider.getAccessToken()

        // Act
        provider.signOut()

        // Assert
        coVerify(exactly = 1) { api.revokeToken(TOKEN) }
        coVerify(exactly = 1) { authorizer.clearToken(TOKEN) }
        verify(exactly = 1) { authorizer.clearAuthorization() }
    }

    @Test
    fun `GIVEN no cached token WHEN signOut THEN nothing revoked AND state cleared`() = runTest {
        // Arrange
        every { authorizer.clearAuthorization() } just Runs

        // Act
        provider.signOut()

        // Assert
        coVerify(exactly = 0) { api.revokeToken(any()) }
        verify(exactly = 1) { authorizer.clearAuthorization() }
    }

    private fun provideTestModels() = listOf(
        CloudBackupError.AuthCanceled,
        CloudBackupError.AuthRequired,
        CloudBackupError.AuthPermissionsMissing,
        CloudBackupError.NetworkError,
        CloudBackupError.CloudUnavailable,
        CloudBackupError.Unknown(),
    )

    private companion object {
        const val TOKEN = "ya29.access-token"
    }
}
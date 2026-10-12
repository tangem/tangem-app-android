package com.tangem.google.drive

import android.content.Context
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.connection.NetworkConnectionManager
import com.tangem.google.GoogleServicesHelper
import com.tangem.google.auth.GoogleAuthError
import com.tangem.google.auth.GoogleAuthResult
import com.tangem.google.auth.GoogleAuthScope
import com.tangem.google.auth.GoogleAuthorizer
import com.tangem.test.core.ProvideTestModels
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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

    private val authorizer: GoogleAuthorizer = mockk()
    private val api: GoogleDriveApi = mockk(relaxed = true)
    private val context: Context = mockk()
    private val networkConnectionManager: NetworkConnectionManager = mockk()

    private lateinit var provider: DefaultGoogleDriveTokenProvider

    @BeforeEach
    fun setUp() {
        clearMocks(authorizer, api, networkConnectionManager)
        mockkObject(GoogleServicesHelper)
        every { GoogleServicesHelper.checkGoogleServicesAvailability(any()) } returns true
        every { networkConnectionManager.isOnline } returns true
        provider = DefaultGoogleDriveTokenProvider(
            authorizer = authorizer,
            api = api,
            context = context,
            networkConnectionManager = networkConnectionManager,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(GoogleServicesHelper)
    }

    @Test
    fun `GIVEN play services unavailable WHEN getAccessToken THEN Unavailable and no authorize`() = runTest {
        // Arrange
        every { GoogleServicesHelper.checkGoogleServicesAvailability(any()) } returns false

        // Act
        val actual = provider.getAccessToken()

        // Assert
        assertThat(actual).isEqualTo(GoogleAuthError.Unavailable.left())
        coVerify(exactly = 0) { authorizer.authorize(any(), any()) }
    }

    @Test
    fun `GIVEN device offline WHEN getAccessToken THEN NetworkError and no authorize`() = runTest {
        // Arrange
        every { networkConnectionManager.isOnline } returns false

        // Act
        val actual = provider.getAccessToken()

        // Assert
        assertThat(actual).isEqualTo(GoogleAuthError.NetworkError.left())
        coVerify(exactly = 0) { authorizer.authorize(any(), any()) }
    }

    @Test
    fun `GIVEN the default provider WHEN getAccessToken THEN only the drive file scope is asked for`() = runTest {
        // Arrange
        coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(TOKEN).right()

        // Act
        provider.getAccessToken()

        // Assert
        coVerify(exactly = 1) {
            authorizer.authorize(
                scopes = listOf(DefaultGoogleDriveTokenProvider.DRIVE_FILE_SCOPE),
                interactive = any(),
            )
        }
    }

    @Test
    fun `GIVEN the hidden folder provider WHEN getAccessToken THEN only the appdata scope is asked for`() = runTest {
        // Arrange — the scope is the whole permission boundary of the hidden folder, so it is pinned here
        coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(TOKEN).right()
        val appDataProvider = DefaultGoogleDriveTokenProvider(
            authorizer = authorizer,
            api = api,
            context = context,
            networkConnectionManager = networkConnectionManager,
            scopes = listOf(DefaultGoogleDriveTokenProvider.DRIVE_APP_DATA_SCOPE),
        )

        // Act
        appDataProvider.getAccessToken()

        // Assert
        coVerify(exactly = 1) {
            authorizer.authorize(
                scopes = listOf(DefaultGoogleDriveTokenProvider.DRIVE_APP_DATA_SCOPE),
                interactive = any(),
            )
        }
    }

    @Test
    fun `GIVEN cached token AND device offline WHEN getAccessToken THEN cached token returned`() = runTest {
        // Arrange
        coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(TOKEN).right()
        provider.getAccessToken()
        every { networkConnectionManager.isOnline } returns false

        // Act
        val actual = provider.getAccessToken()

        // Assert
        assertThat(actual).isEqualTo(TOKEN.right())
    }

    @Test
    fun `GIVEN authorized WHEN getAccessToken twice THEN authorized once AND second call returns cached`() = runTest {
        // Arrange
        coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(TOKEN).right()

        // Act
        val first = provider.getAccessToken()
        val second = provider.getAccessToken()

        // Assert
        assertThat(first).isEqualTo(TOKEN.right())
        assertThat(second).isEqualTo(TOKEN.right())
        coVerify(exactly = 1) { authorizer.authorize(any(), any()) }
    }

    @Test
    fun `GIVEN any request WHEN getAccessToken THEN only the drive file scope is requested`() = runTest {
        // Arrange
        coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(TOKEN).right()

        // Act
        provider.getAccessToken()

        // Assert
        coVerify(exactly = 1) { authorizer.authorize(scopes = listOf(DRIVE_FILE_SCOPE), interactive = true) }
    }

    @Test
    fun `GIVEN silent request WHEN getAccessToken THEN authorize called non-interactively`() = runTest {
        // Arrange
        coEvery { authorizer.authorize(any(), interactive = false) } returns GoogleAuthError.AuthRequired.left()

        // Act
        val actual = provider.getAccessToken(interactive = false)

        // Assert
        assertThat(actual).isEqualTo(GoogleAuthError.AuthRequired.left())
        coVerify(exactly = 1) { authorizer.authorize(any(), interactive = false) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN authorize fails WHEN getAccessToken THEN error propagated AND nothing cached`(error: GoogleAuthError) =
        runTest {
            // Arrange
            coEvery { authorizer.authorize(any(), any()) } returns error.left()

            // Act
            val actual = provider.getAccessToken()
            provider.getAccessToken()

            // Assert — a failed authorization is not cached, so the next call authorizes again
            assertThat(actual).isEqualTo(error.left())
            coVerify(exactly = 2) { authorizer.authorize(any(), any()) }
        }

    @Test
    fun `GIVEN cached token WHEN invalidate THEN token dropped AND re-auth on next call`() = runTest {
        // Arrange
        coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(TOKEN).right()
        coEvery { authorizer.clearToken(TOKEN) } just Runs
        every { authorizer.clearAuthorization() } just Runs
        provider.getAccessToken()

        // Act
        provider.invalidate()
        provider.getAccessToken()

        // Assert
        coVerify(exactly = 1) { authorizer.clearToken(TOKEN) }
        verify(exactly = 1) { authorizer.clearAuthorization() }
        coVerify(exactly = 2) { authorizer.authorize(any(), any()) }
    }

    @Test
    fun `GIVEN cached token WHEN switchAccount THEN token dropped before the account is pinned`() = runTest {
        // Arrange — a token cached for the previous account must not survive the switch
        coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(TOKEN).right()
        coEvery { authorizer.clearToken(TOKEN) } just Runs
        every { authorizer.clearAuthorization() } just Runs
        every { authorizer.selectAccount(ACCOUNT_NAME) } just Runs
        provider.getAccessToken()

        // Act
        provider.switchAccount(ACCOUNT_NAME)
        provider.getAccessToken()

        // Assert
        coVerifyOrder {
            authorizer.clearToken(TOKEN)
            authorizer.clearAuthorization()
            authorizer.selectAccount(ACCOUNT_NAME)
        }
        coVerify(exactly = 2) { authorizer.authorize(any(), any()) }
    }

    @Test
    fun `GIVEN cached token WHEN signOut THEN token revoked AND dropped AND state cleared`() = runTest {
        // Arrange
        coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(TOKEN).right()
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
        GoogleAuthError.AuthCanceled,
        GoogleAuthError.AuthRequired,
        GoogleAuthError.PermissionsMissing,
        GoogleAuthError.NetworkError,
        GoogleAuthError.Unavailable,
        GoogleAuthError.Unknown(),
    )

    private companion object {
        const val TOKEN = "ya29.access-token"
        const val ACCOUNT_NAME = "user@gmail.com"
        val DRIVE_FILE_SCOPE = GoogleAuthScope("https://www.googleapis.com/auth/drive.file")
    }
}
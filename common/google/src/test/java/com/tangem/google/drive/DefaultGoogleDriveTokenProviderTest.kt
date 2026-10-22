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
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultGoogleDriveTokenProviderTest {

    private val authorizer: GoogleAuthorizer = mockk()
    private val context: Context = mockk()
    private val networkConnectionManager: NetworkConnectionManager = mockk()

    private lateinit var provider: DefaultGoogleDriveTokenProvider

    @BeforeEach
    fun setUp() {
        clearMocks(authorizer, networkConnectionManager)
        mockkObject(GoogleServicesHelper)
        every { GoogleServicesHelper.checkGoogleServicesAvailability(any()) } returns true
        every { networkConnectionManager.isOnlineSync } returns true
        every { authorizer.selectedAccountName } returns null
        provider = DefaultGoogleDriveTokenProvider(
            authorizer = authorizer,
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
        every { networkConnectionManager.isOnlineSync } returns false

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
        stubAuthorizePinningAccount()
        provider.getAccessToken()
        every { networkConnectionManager.isOnlineSync } returns false

        // Act
        val actual = provider.getAccessToken()

        // Assert
        assertThat(actual).isEqualTo(TOKEN.right())
    }

    @Test
    fun `GIVEN authorized WHEN getAccessToken twice THEN authorized once AND second call returns cached`() = runTest {
        // Arrange
        stubAuthorizePinningAccount()

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
    fun `GIVEN cached token WHEN invalidateToken THEN token dropped AND account stays pinned`() = runTest {
        // Arrange
        coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(TOKEN).right()
        coEvery { authorizer.clearToken(TOKEN) } just Runs
        provider.getAccessToken()

        // Act
        provider.invalidateToken()
        provider.getAccessToken()

        // Assert
        coVerify(exactly = 1) { authorizer.clearToken(TOKEN) }
        verify(exactly = 0) { authorizer.clearAuthorization() }
        coVerify(exactly = 2) { authorizer.authorize(any(), any()) }
    }

    @Test
    fun `GIVEN an account pinned WHEN switchAccount to another THEN the authorization moves to it`() = runTest {
        // Arrange
        every { authorizer.selectedAccountName } returns OTHER_ACCOUNT_NAME
        every { authorizer.clearAuthorization() } just Runs
        every { authorizer.selectAccount(ACCOUNT_NAME) } just Runs

        // Act
        provider.switchAccount(ACCOUNT_NAME)

        // Assert
        coVerifyOrder {
            authorizer.clearAuthorization()
            authorizer.selectAccount(ACCOUNT_NAME)
        }
    }

    @Test
    fun `GIVEN the account is pinned already WHEN switchAccount to it THEN the cached token stays`() = runTest {
        // Arrange
        coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(TOKEN).right()
        every { authorizer.selectedAccountName } returns ACCOUNT_NAME
        provider.getAccessToken()

        // Act
        provider.switchAccount(ACCOUNT_NAME)
        provider.getAccessToken()

        // Assert
        coVerify(exactly = 1) { authorizer.authorize(any(), any()) }
        verify(exactly = 0) { authorizer.clearAuthorization() }
    }

    @Test
    fun `GIVEN a pinned account WHEN switchAccount to null THEN the pin is dropped`() = runTest {
        // Arrange
        every { authorizer.clearAuthorization() } just Runs
        every { authorizer.selectedAccountName } returns ACCOUNT_NAME

        // Act
        provider.switchAccount(accountName = null)

        // Assert
        verify(exactly = 1) { authorizer.clearAuthorization() }
        verify(exactly = 0) { authorizer.selectAccount(any()) }
    }

    @Test
    fun `GIVEN a token cached for the pinned account WHEN getAccessToken for another account THEN it is authorized in it`() =
        runTest {
            // Arrange
            every { authorizer.selectedAccountName } returns OTHER_ACCOUNT_NAME
            coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(OTHER_TOKEN).right()
            coEvery { authorizer.authorizeAccount(ACCOUNT_NAME, any(), any()) } returns GoogleAuthResult(TOKEN).right()
            provider.getAccessToken()

            // Act
            val actual = withContext(GoogleDriveAccount(ACCOUNT_NAME)) { provider.getAccessToken(interactive = false) }

            // Assert
            assertThat(actual).isEqualTo(TOKEN.right())
            coVerify(exactly = 1) {
                authorizer.authorizeAccount(ACCOUNT_NAME, listOf(DRIVE_FILE_SCOPE), interactive = false)
            }
        }

    @Test
    fun `GIVEN a token cached for an account WHEN the pinned account changes THEN the account keeps its token`() =
        runTest {
            // Arrange — a screen re-pinning the account must not move an operation of a wallet in the middle of it
            coEvery { authorizer.authorizeAccount(ACCOUNT_NAME, any(), any()) } returns GoogleAuthResult(TOKEN).right()
            every { authorizer.clearAuthorization() } just Runs
            every { authorizer.selectAccount(OTHER_ACCOUNT_NAME) } just Runs
            withContext(GoogleDriveAccount(ACCOUNT_NAME)) { provider.getAccessToken() }

            // Act
            provider.switchAccount(OTHER_ACCOUNT_NAME)
            val actual = withContext(GoogleDriveAccount(ACCOUNT_NAME)) { provider.getAccessToken() }

            // Assert
            assertThat(actual).isEqualTo(TOKEN.right())
            coVerify(exactly = 1) { authorizer.authorizeAccount(any(), any(), any()) }
        }

    @Test
    fun `GIVEN tokens of two accounts WHEN invalidateToken for one THEN only its token is dropped`() = runTest {
        // Arrange
        every { authorizer.selectedAccountName } returns OTHER_ACCOUNT_NAME
        coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(OTHER_TOKEN).right()
        coEvery { authorizer.authorizeAccount(ACCOUNT_NAME, any(), any()) } returns GoogleAuthResult(TOKEN).right()
        coEvery { authorizer.clearToken(TOKEN) } just Runs
        provider.getAccessToken()
        withContext(GoogleDriveAccount(ACCOUNT_NAME)) { provider.getAccessToken() }

        // Act
        withContext(GoogleDriveAccount(ACCOUNT_NAME)) { provider.invalidateToken() }
        provider.getAccessToken()

        // Assert
        coVerify(exactly = 1) { authorizer.clearToken(TOKEN) }
        coVerify(exactly = 0) { authorizer.clearToken(OTHER_TOKEN) }
        coVerify(exactly = 1) { authorizer.authorize(any(), any()) }
    }

    @Test
    fun `GIVEN an account picked WHEN currentAccountName THEN it is the authorizer's account`() = runTest {
        // Arrange
        every { authorizer.selectedAccountName } returns ACCOUNT_NAME

        // Act
        val actual = provider.currentAccountName()

        // Assert
        assertThat(actual).isEqualTo(ACCOUNT_NAME)
    }

    @Test
    fun `GIVEN an account picked WHEN currentAccountName for the account Google picks THEN null`() = runTest {
        // Arrange
        every { authorizer.selectedAccountName } returns ACCOUNT_NAME

        // Act
        val actual = withContext(GoogleDriveAccount(name = null)) { provider.currentAccountName() }

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN a token got silently in the account Google picks WHEN interactive getAccessToken without a pin THEN authorized again`() =
        runTest {
            // Arrange
            coEvery { authorizer.authorizeAccount(null, any(), any()) } returns GoogleAuthResult(OTHER_TOKEN).right()
            coEvery { authorizer.authorize(any(), any()) } returns GoogleAuthResult(TOKEN).right()
            withContext(GoogleDriveAccount(name = null)) { provider.getAccessToken(interactive = false) }

            // Act
            val actual = provider.getAccessToken(interactive = true)

            // Assert
            assertThat(actual).isEqualTo(TOKEN.right())
            coVerify(exactly = 1) { authorizer.authorize(listOf(DRIVE_FILE_SCOPE), interactive = true) }
        }

    private fun stubAuthorizePinningAccount() {
        coEvery { authorizer.authorize(any(), any()) } answers {
            every { authorizer.selectedAccountName } returns ACCOUNT_NAME
            GoogleAuthResult(TOKEN).right()
        }
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
        const val OTHER_TOKEN = "ya29.other-access-token"
        const val ACCOUNT_NAME = "user@gmail.com"
        const val OTHER_ACCOUNT_NAME = "other@gmail.com"
        val DRIVE_FILE_SCOPE = GoogleAuthScope("https://www.googleapis.com/auth/drive.file")
    }
}
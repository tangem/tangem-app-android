package com.tangem.google.auth

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResult
import arrow.core.left
import arrow.core.right
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationClient
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GoogleIdentityAuthorizerTest {

    private val context: Context = mockk()
    private val bridge: GoogleAuthActivityResultBridge = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val authorizer = GoogleIdentityAuthorizer(context, bridge, dispatchers)

    private val scopes = listOf(GoogleAuthScope("https://www.googleapis.com/auth/drive.file"))

    private lateinit var builder: AuthorizationRequest.Builder

    @BeforeEach
    fun setup() {
        clearMocks(bridge)
        mockkStatic(Identity::class, AuthorizationRequest::class, GoogleAuthUtil::class, AccountManager::class)
        builder = mockk()
        every { AuthorizationRequest.builder() } returns builder
        every { builder.setRequestedScopes(any()) } returns builder
        every { builder.setAccount(any()) } returns builder
        every { builder.build() } returns mockk()
        every {
            AccountManager.newChooseAccountIntent(any(), any(), any(), any(), any(), any(), any())
        } returns mockk()
        coEvery { bridge.launchAccountPicker(any()) } returns accountPickerResult(name = "user@gmail.com")
        authorizer.clearAuthorization()
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `GIVEN token granted WHEN authorize THEN returns access token`() = runTest {
        // Arrange
        stubAuthorize(successTask(authResult(hasResolution = false, token = "tok")))

        // Act
        val actual = authorizer.authorize(scopes, interactive = true)

        // Assert
        assertThat(actual).isEqualTo(GoogleAuthResult(accessToken = "tok").right())
    }

    @Test
    fun `GIVEN no token WHEN authorize THEN PermissionsMissing`() = runTest {
        // Arrange
        stubAuthorize(successTask(authResult(hasResolution = false, token = null)))

        // Act
        val actual = authorizer.authorize(scopes, interactive = true)

        // Assert
        assertThat(actual).isEqualTo(GoogleAuthError.PermissionsMissing.left())
    }

    @Test
    fun `GIVEN resolution required AND non-interactive WHEN authorize THEN AuthRequired`() = runTest {
        // Arrange
        stubAuthorize(successTask(authResult(hasResolution = true, token = null)))

        // Act
        val actual = authorizer.authorize(scopes, interactive = false)

        // Assert
        assertThat(actual).isEqualTo(GoogleAuthError.AuthRequired.left())
    }

    @Test
    fun `GIVEN resolution required AND user cancels WHEN authorize THEN AuthCanceled`() = runTest {
        // Arrange
        val result = authResult(hasResolution = true, token = null)
        every { result.pendingIntent } returns mockk(relaxed = true)
        stubAuthorize(successTask(result))
        coEvery { bridge.launch(any()) } returns mockk { every { resultCode } returns Activity.RESULT_CANCELED }

        // Act
        val actual = authorizer.authorize(scopes, interactive = true)

        // Assert
        assertThat(actual).isEqualTo(GoogleAuthError.AuthCanceled.left())
    }

    @Test
    fun `GIVEN no consent launcher WHEN authorize THEN Unknown instead of cancellation`() = runTest {
        // Arrange
        val result = authResult(hasResolution = true, token = null)
        every { result.pendingIntent } returns mockk(relaxed = true)
        stubAuthorize(successTask(result))
        coEvery { bridge.launch(any()) } throws GoogleAuthLauncherUnavailableException()

        // Act
        val actual = authorizer.authorize(scopes, interactive = true)

        // Assert
        assertThat(actual.isLeft()).isTrue()
        assertThat(actual.leftOrNull()).isInstanceOf(GoogleAuthError.Unknown::class.java)
    }

    @Test
    fun `GIVEN ApiException WHEN authorize THEN mapped error`() = runTest {
        // Arrange
        val apiException = mockk<ApiException>(relaxed = true) { every { statusCode } returns CommonStatusCodes.NETWORK_ERROR }
        stubAuthorize(failureTask(apiException))

        // Act
        val actual = authorizer.authorize(scopes, interactive = true)

        // Assert
        assertThat(actual).isEqualTo(GoogleAuthError.NetworkError.left())
    }

    @Test
    fun `GIVEN IOException WHEN authorize THEN NetworkError`() = runTest {
        // Arrange
        stubAuthorize(failureTask(IOException()))

        // Act
        val actual = authorizer.authorize(scopes, interactive = true)

        // Assert
        assertThat(actual).isEqualTo(GoogleAuthError.NetworkError.left())
    }

    @Test
    fun `GIVEN interactive WHEN authorize THEN account is picked and request pinned to it`() = runTest {
        // Arrange
        stubAuthorize(successTask(authResult(hasResolution = false, token = "tok")))

        // Act
        authorizer.authorize(scopes, interactive = true)

        // Assert
        coVerify(exactly = 1) { bridge.launchAccountPicker(any()) }
        verify(exactly = 1) { builder.setAccount(any()) }
    }

    @Test
    fun `GIVEN account already picked WHEN authorize again THEN picker is not shown twice`() = runTest {
        // Arrange
        stubAuthorize(successTask(authResult(hasResolution = false, token = "tok")))
        authorizer.authorize(scopes, interactive = true)

        // Act
        authorizer.authorize(scopes, interactive = true)

        // Assert
        coVerify(exactly = 1) { bridge.launchAccountPicker(any()) }
    }

    @Test
    fun `GIVEN picked account WHEN clearAuthorization THEN next authorize asks for the account again`() = runTest {
        // Arrange
        stubAuthorize(successTask(authResult(hasResolution = false, token = "tok")))
        authorizer.authorize(scopes, interactive = true)

        // Act
        authorizer.clearAuthorization()
        authorizer.authorize(scopes, interactive = true)

        // Assert
        coVerify(exactly = 2) { bridge.launchAccountPicker(any()) }
    }

    @Test
    fun `GIVEN account picker cancelled WHEN authorize THEN AuthCanceled`() = runTest {
        // Arrange
        stubAuthorize(successTask(authResult(hasResolution = false, token = "tok")))
        coEvery { bridge.launchAccountPicker(any()) } returns
            mockk { every { resultCode } returns Activity.RESULT_CANCELED }

        // Act
        val actual = authorizer.authorize(scopes, interactive = true)

        // Assert
        assertThat(actual).isEqualTo(GoogleAuthError.AuthCanceled.left())
    }

    @Test
    fun `GIVEN account picker returns no account name WHEN authorize THEN AuthCanceled`() = runTest {
        // Arrange
        stubAuthorize(successTask(authResult(hasResolution = false, token = "tok")))
        coEvery { bridge.launchAccountPicker(any()) } returns accountPickerResult(name = null)

        // Act
        val actual = authorizer.authorize(scopes, interactive = true)

        // Assert
        assertThat(actual).isEqualTo(GoogleAuthError.AuthCanceled.left())
        verify(exactly = 0) { builder.setAccount(any()) }
    }

    @Test
    fun `GIVEN account picker returned no name WHEN authorize again THEN picker is shown again`() = runTest {
        // Arrange
        stubAuthorize(successTask(authResult(hasResolution = false, token = "tok")))
        coEvery { bridge.launchAccountPicker(any()) } returns accountPickerResult(name = null)
        authorizer.authorize(scopes, interactive = true)

        // Act
        authorizer.authorize(scopes, interactive = true)

        // Assert
        coVerify(exactly = 2) { bridge.launchAccountPicker(any()) }
    }

    @Test
    fun `GIVEN no picker launcher WHEN authorize THEN falls back to the implicit account`() = runTest {
        // Arrange
        stubAuthorize(successTask(authResult(hasResolution = false, token = "tok")))
        coEvery { bridge.launchAccountPicker(any()) } throws GoogleAuthLauncherUnavailableException()

        // Act
        val actual = authorizer.authorize(scopes, interactive = true)

        // Assert
        assertThat(actual).isEqualTo(GoogleAuthResult(accessToken = "tok").right())
        verify(exactly = 0) { builder.setAccount(any()) }
    }

    @Test
    fun `GIVEN non-interactive WHEN authorize THEN no account picker is shown`() = runTest {
        // Arrange
        stubAuthorize(successTask(authResult(hasResolution = false, token = "tok")))

        // Act
        authorizer.authorize(scopes, interactive = false)

        // Assert
        coVerify(exactly = 0) { bridge.launchAccountPicker(any()) }
        verify(exactly = 0) { builder.setAccount(any()) }
    }

    @Test
    fun `GIVEN token WHEN clearToken THEN cleared via GoogleAuthUtil`() = runTest {
        // Arrange
        every { GoogleAuthUtil.clearToken(any<Context>(), any<String>()) } just Runs

        // Act
        authorizer.clearToken("tok")

        // Assert
        verify(exactly = 1) { GoogleAuthUtil.clearToken(context, "tok") }
    }

    private fun accountPickerResult(name: String?): ActivityResult = mockk {
        every { resultCode } returns Activity.RESULT_OK
        every { data } returns mockk { every { getStringExtra(AccountManager.KEY_ACCOUNT_NAME) } returns name }
    }

    private fun authResult(hasResolution: Boolean, token: String?): AuthorizationResult = mockk {
        every { hasResolution() } returns hasResolution
        every { accessToken } returns token
    }

    private fun stubAuthorize(task: Task<AuthorizationResult>) {
        val client = mockk<AuthorizationClient>()
        every { Identity.getAuthorizationClient(any<Context>()) } returns client
        every { client.authorize(any()) } returns task
    }

    private fun successTask(result: AuthorizationResult): Task<AuthorizationResult> {
        val task = mockk<Task<AuthorizationResult>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<AuthorizationResult>>()) } answers {
            firstArg<OnSuccessListener<AuthorizationResult>>().onSuccess(result)
            task
        }
        every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
        every { task.addOnCanceledListener(any<OnCanceledListener>()) } returns task
        return task
    }

    private fun failureTask(error: Exception): Task<AuthorizationResult> {
        val task = mockk<Task<AuthorizationResult>>()
        every { task.addOnSuccessListener(any<OnSuccessListener<AuthorizationResult>>()) } returns task
        every { task.addOnFailureListener(any<OnFailureListener>()) } answers {
            firstArg<OnFailureListener>().onFailure(error)
            task
        }
        every { task.addOnCanceledListener(any<OnCanceledListener>()) } returns task
        return task
    }
}
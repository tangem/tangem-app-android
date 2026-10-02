package com.tangem.data.mcp.repository

import android.util.Base64
import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.domain.mcp.models.McpDeviceVerificationResult
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.lib.auth.api.AuthApi
import com.tangem.lib.auth.api.models.request.McpDeviceVerificationConsent
import com.tangem.lib.auth.api.models.response.McpDeviceVerificationResponse
import com.tangem.lib.auth.api.models.response.McpSessionResponse
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Base64 as JavaBase64
import com.tangem.lib.auth.api.models.response.McpDeviceVerificationResult as McpDeviceVerificationResultResponse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMcpRepositoryTest {

    private val authApi: AuthApi = mockk()

    private val repository = DefaultMcpRepository(authApi = authApi)

    @BeforeEach
    fun setUp() {
        clearMocks(authApi)
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers { JavaBase64.getEncoder().encodeToString(firstArg()) }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetDeviceVerification {

        @Test
        fun `GIVEN verification response WHEN getDeviceVerification THEN maps it field by field`() = runTest {
            // Arrange
            coEvery { authApi.getMcpDeviceVerification(USER_CODE) } returns ApiResponse.Success(VERIFICATION_RESPONSE)

            // Act
            val actual = repository.getDeviceVerification(USER_CODE)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(VERIFICATION))
        }

        @Test
        fun `GIVEN api error WHEN getDeviceVerification THEN the cause is passed through`() = runTest {
            // Arrange
            coEvery { authApi.getMcpDeviceVerification(USER_CODE) } returns apiError(NETWORK_ERROR)

            // Act
            val actual = repository.getDeviceVerification(USER_CODE)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(NETWORK_ERROR))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ApproveDeviceVerification {

        @Test
        fun `GIVEN a verification WHEN approve THEN echoes the consent with a Base64 wallet id`() = runTest {
            // Arrange
            coEvery { authApi.approveMcpDeviceVerification(any(), any(), any()) } returns
                ApiResponse.Success(McpDeviceVerificationResultResponse(isSuccess = true))

            // Act
            val actual = repository.approveDeviceVerification(verification = VERIFICATION, walletId = WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(McpDeviceVerificationResult(isSuccess = true)))
            coVerify(exactly = 1) {
                authApi.approveMcpDeviceVerification(
                    userCode = USER_CODE,
                    state = STATE,
                    consent = McpDeviceVerificationConsent(
                        clientId = CLIENT_ID,
                        scope = SCOPE,
                        walletId = BASE64_WALLET_ID,
                    ),
                )
            }
        }

        @Test
        fun `GIVEN service refuses WHEN approve THEN the refusal is a result, not a failure`() = runTest {
            // Arrange
            coEvery { authApi.approveMcpDeviceVerification(any(), any(), any()) } returns
                ApiResponse.Success(McpDeviceVerificationResultResponse(isSuccess = false))

            // Act
            val actual = repository.approveDeviceVerification(verification = VERIFICATION, walletId = WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(McpDeviceVerificationResult(isSuccess = false)))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Sessions {

        @Test
        fun `GIVEN sessions response WHEN getSessions THEN maps every session`() = runTest {
            // Arrange
            coEvery { authApi.getMcpSessions() } returns ApiResponse.Success(listOf(SESSION_RESPONSE))

            // Act
            val actual = repository.getSessions()

            // Assert
            assertThat(actual).isEqualTo(Either.Right(listOf(SESSION)))
        }

        @Test
        fun `GIVEN revoke succeeds WHEN revokeSession THEN calls the api with the id`() = runTest {
            // Arrange
            coEvery { authApi.revokeMcpSession(any()) } returns ApiResponse.Success(Unit)

            // Act
            val actual = repository.revokeSession(SESSION_ID)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(Unit))
            coVerify(exactly = 1) { authApi.revokeMcpSession(sessionId = SESSION_ID) }
        }

        @Test
        fun `GIVEN api error WHEN revokeSession THEN the cause is passed through`() = runTest {
            // Arrange
            coEvery { authApi.revokeMcpSession(any()) } returns apiError(SERVER_ERROR)

            // Act
            val actual = repository.revokeSession(SESSION_ID)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(SERVER_ERROR))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> apiError(cause: ApiResponseError): ApiResponse<T> =
        ApiResponse.Error(cause) as ApiResponse<T>

    private companion object {
        const val USER_CODE = "WDJB-MJHT"
        const val CLIENT_ID = "mcp-client-42"
        const val ORIGIN = "claude.ai"
        const val CLIENT_NAME = "Claude Code"
        const val SCOPE = "wallet:read wallet:sign"
        const val STATE = "state-token"
        const val SESSION_ID = "b3f1c0d2"

        val WALLET_ID = UserWalletId(stringValue = "011")
        val BASE64_WALLET_ID: String = JavaBase64.getEncoder().encodeToString(WALLET_ID.value)

        val NETWORK_ERROR = ApiResponseError.NetworkException()
        val SERVER_ERROR = ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.INTERNAL_SERVER_ERROR,
            message = null,
            errorBody = null,
        )

        val VERIFICATION_RESPONSE = McpDeviceVerificationResponse(
            clientId = CLIENT_ID,
            clientOrigin = ORIGIN,
            clientName = CLIENT_NAME,
            isClientNameVerified = true,
            scope = SCOPE,
            state = STATE,
            userCode = USER_CODE,
        )

        val VERIFICATION = McpDeviceVerification(
            clientId = CLIENT_ID,
            clientOrigin = ORIGIN,
            clientName = CLIENT_NAME,
            isClientNameVerified = true,
            scope = SCOPE,
            state = STATE,
            userCode = USER_CODE,
        )

        val SESSION_RESPONSE = McpSessionResponse(
            id = SESSION_ID,
            clientId = CLIENT_ID,
            clientOrigin = ORIGIN,
            clientName = CLIENT_NAME,
            scopes = listOf("wallet:read"),
            clientIp = "203.0.113.7",
            clientCountry = "DE",
            clientUserAgent = "claude-code/1.0.0",
        )

        val SESSION = McpSession(
            id = SESSION_ID,
            clientId = CLIENT_ID,
            clientOrigin = ORIGIN,
            clientName = CLIENT_NAME,
            scopes = listOf("wallet:read"),
            clientIp = "203.0.113.7",
            clientCountry = "DE",
            clientUserAgent = "claude-code/1.0.0",
        )
    }
}
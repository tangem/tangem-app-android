package com.tangem.data.mcp.repository

import android.util.Base64
import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.domain.mcp.models.McpDeviceVerificationResult
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.data.mcp.api.McpApi
import com.tangem.data.mcp.api.models.request.McpDeviceVerificationConsent
import com.tangem.data.mcp.api.models.response.McpDeviceVerificationResponse
import com.tangem.data.mcp.api.models.response.McpSessionResponse
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Base64 as JavaBase64
import com.tangem.data.mcp.api.models.response.McpDeviceVerificationResult as McpDeviceVerificationResultResponse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMcpRepositoryTest {

    private val mcpApi: McpApi = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val appScope = object : AppCoroutineScope, CoroutineScope by testScope {}

    private val repository = DefaultMcpRepository(
        mcpApi = mcpApi,
        userWalletsListRepository = userWalletsListRepository,
        appScope = appScope,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(mcpApi, userWalletsListRepository)
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(
            mockk { every { walletId } returns WALLET_ID },
        )
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
            coEvery { mcpApi.getDeviceVerification(USER_CODE) } returns ApiResponse.Success(VERIFICATION_RESPONSE)

            // Act
            val actual = repository.getDeviceVerification(USER_CODE)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(VERIFICATION))
        }

        @Test
        fun `GIVEN api error WHEN getDeviceVerification THEN the cause is passed through`() = runTest {
            // Arrange
            coEvery { mcpApi.getDeviceVerification(USER_CODE) } returns apiError(NETWORK_ERROR)

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
        fun `GIVEN approve succeeds WHEN approve THEN the session list is re-read`() = runTest {
            // Arrange
            coEvery { mcpApi.getSessions() } returns ApiResponse.Success(listOf(SESSION_RESPONSE))
            coEvery { mcpApi.approveDeviceVerification(any(), any(), any()) } returns
                ApiResponse.Success(McpDeviceVerificationResultResponse(isSuccess = true))

            // Act
            repository.approveDeviceVerification(verification = VERIFICATION, walletId = WALLET_ID)

            // Assert
            coVerify(exactly = 1) { mcpApi.getSessions() }
        }

        @Test
        fun `GIVEN a verification WHEN approve THEN echoes the consent with a Base64 wallet id`() = runTest {
            // Arrange
            coEvery { mcpApi.getSessions() } returns ApiResponse.Success(listOf(SESSION_RESPONSE))
            coEvery { mcpApi.approveDeviceVerification(any(), any(), any()) } returns
                ApiResponse.Success(McpDeviceVerificationResultResponse(isSuccess = true))

            // Act
            val actual = repository.approveDeviceVerification(verification = VERIFICATION, walletId = WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(McpDeviceVerificationResult(isSuccess = true)))
            coVerify(exactly = 1) {
                mcpApi.approveDeviceVerification(
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
            coEvery { mcpApi.approveDeviceVerification(any(), any(), any()) } returns
                ApiResponse.Success(McpDeviceVerificationResultResponse(isSuccess = false))

            // Act
            val actual = repository.approveDeviceVerification(verification = VERIFICATION, walletId = WALLET_ID)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(McpDeviceVerificationResult(isSuccess = false)))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RejectDeviceVerification {

        @Test
        fun `GIVEN a verification WHEN reject THEN calls the api with its user code and state`() = runTest {
            // Arrange
            coEvery { mcpApi.rejectDeviceVerification(any(), any()) } returns ApiResponse.Success(Unit)

            // Act
            val actual = repository.rejectDeviceVerification(verification = VERIFICATION)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(Unit))
            coVerify(exactly = 1) {
                mcpApi.rejectDeviceVerification(userCode = USER_CODE, state = STATE)
            }
        }

        @Test
        fun `GIVEN api error WHEN reject THEN the cause is passed through`() = runTest {
            // Arrange
            coEvery { mcpApi.rejectDeviceVerification(any(), any()) } returns apiError(SERVER_ERROR)

            // Act
            val actual = repository.rejectDeviceVerification(verification = VERIFICATION)

            // Assert
            assertThat(actual).isEqualTo(Either.Left(SERVER_ERROR))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Sessions {

        @Test
        fun `GIVEN sessions response WHEN getSessions THEN maps every session`() = runTest {
            // Arrange
            coEvery { mcpApi.getSessions() } returns ApiResponse.Success(listOf(SESSION_RESPONSE))

            // Act
            val actual = repository.getSessions().first()

            // Assert
            assertThat(actual.map { it.copy(createdAtMillis = 0) }).containsExactly(SESSION)
        }

        @Test
        fun `GIVEN no wallets WHEN getSessions THEN the stubbed wallet cannot be picked and nothing is mapped`() =
            runTest {
                // Arrange
                coEvery { mcpApi.getSessions() } returns ApiResponse.Success(listOf(SESSION_RESPONSE))
                coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()

                // Act
                val actual = repository.getSessions().first()

                // Assert
                assertThat(actual).isEmpty()
            }

        @Test
        fun `GIVEN a read in flight WHEN getSessions collected again THEN the read is not duplicated`() = runTest {
            // Arrange
            val release = CompletableDeferred<Unit>()
            coEvery { mcpApi.getSessions() } coAnswers {
                release.await()
                ApiResponse.Success(listOf(SESSION_RESPONSE))
            }

            // Act
            repository.getSessions().first()
            repository.getSessions().first()
            release.complete(Unit)

            // Assert
            coVerify(exactly = 1) { mcpApi.getSessions() }
        }

        @Test
        fun `GIVEN the previous read finished WHEN getSessions collected again THEN the list is read again`() =
            runTest {
                // Arrange
                coEvery { mcpApi.getSessions() } returns ApiResponse.Success(listOf(SESSION_RESPONSE))

                // Act
                repository.getSessions().first()
                repository.getSessions().first()

                // Assert
                coVerify(exactly = 2) { mcpApi.getSessions() }
            }

        @Test
        fun `GIVEN revoke succeeds WHEN revokeSession THEN calls the api and re-reads the list`() = runTest {
            // Arrange
            coEvery { mcpApi.getSessions() } returns ApiResponse.Success(listOf(SESSION_RESPONSE))
            coEvery { mcpApi.revokeSession(any()) } returns ApiResponse.Success(Unit)
            repository.getSessions().first()

            // Act
            val actual = repository.revokeSession(SESSION_ID)

            // Assert
            assertThat(actual).isEqualTo(Either.Right(Unit))
            coVerify(exactly = 1) { mcpApi.revokeSession(sessionId = SESSION_ID) }
            coVerify(exactly = 2) { mcpApi.getSessions() }
        }

        @Test
        fun `GIVEN revoke fails WHEN revokeSession THEN the list is not re-read`() = runTest {
            // Arrange
            coEvery { mcpApi.getSessions() } returns ApiResponse.Success(listOf(SESSION_RESPONSE))
            coEvery { mcpApi.revokeSession(any()) } returns apiError(SERVER_ERROR)
            repository.getSessions().first()

            // Act
            repository.revokeSession(SESSION_ID)

            // Assert
            coVerify(exactly = 1) { mcpApi.getSessions() }
        }

        @Test
        fun `GIVEN api error WHEN revokeSession THEN the cause is passed through`() = runTest {
            // Arrange
            coEvery { mcpApi.revokeSession(any()) } returns apiError(SERVER_ERROR)

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
        const val ICON_URL = "https://tangem.api/agents/claudeCode.png"
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
            clientIconUrl = ICON_URL,
            scopes = listOf("wallet:read"),
            clientIp = "203.0.113.7",
            clientCountry = "DE",
            clientUserAgent = "claude-code/1.0.0",
        )

        val SESSION = McpSession(
            id = SESSION_ID,
            walletId = WALLET_ID,
            createdAtMillis = 0,
            clientId = CLIENT_ID,
            clientOrigin = ORIGIN,
            clientName = CLIENT_NAME,
            clientIconUrl = ICON_URL,
            scopes = listOf("wallet:read"),
            clientIp = "203.0.113.7",
            clientCountry = "DE",
            clientUserAgent = "claude-code/1.0.0",
        )
    }
}
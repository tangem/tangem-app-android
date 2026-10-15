package com.tangem.data.mcp.repository

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.mcp.api.McpApi
import com.tangem.data.mcp.converter.McpClientConverter
import com.tangem.data.mcp.api.models.response.McpClientResponse
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMcpRepositoryAgentTest {

    private val mcpApi: McpApi = mockk()
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val appScope = object : AppCoroutineScope, CoroutineScope by testScope {}

    private val repository = DefaultMcpRepository(
        mcpApi = mcpApi,
        appScope = appScope,
        dispatchers = TestingCoroutineDispatcherProvider(),
        mcpClientConverter = McpClientConverter(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(mcpApi)
    }

    @Test
    fun `GIVEN known client WHEN getAgent THEN the agent carries what the server knows`() = runTest {
        // Arrange
        val response = McpClientResponse(
            clientId = CLIENT_ID,
            clientOrigin = "claude.ai",
            clientName = "Claude Code",
            clientIconUrl = "https://icons.tangem.com/claude.png",
        )
        coEvery { mcpApi.getClient(CLIENT_ID) } returns ApiResponse.Success(response)

        // Act
        val actual = repository.getAgent(CLIENT_ID)

        // Assert
        val expected = McpAgent(
            clientId = CLIENT_ID,
            origin = "claude.ai",
            name = "Claude Code",
            iconUrl = "https://icons.tangem.com/claude.png",
        )
        assertThat(actual).isEqualTo(expected)
        coVerify(exactly = 1) { mcpApi.getClient(CLIENT_ID) }
    }

    @Test
    fun `GIVEN client without name and icon WHEN getAgent THEN the missing parts stay empty`() = runTest {
        // Arrange
        val response = McpClientResponse(
            clientId = CLIENT_ID,
            clientOrigin = "agent.example.com",
            clientName = null,
            clientIconUrl = null,
        )
        coEvery { mcpApi.getClient(CLIENT_ID) } returns ApiResponse.Success(response)

        // Act
        val actual = repository.getAgent(CLIENT_ID)

        // Assert
        val expected = McpAgent(clientId = CLIENT_ID, origin = "agent.example.com", name = null, iconUrl = null)
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN lookup fails WHEN getAgent THEN the agent is unknown`(error: ApiResponseError) = runTest {
        // Arrange
        coEvery { mcpApi.getClient(CLIENT_ID) } returns apiError(error)

        // Act
        val actual = repository.getAgent(CLIENT_ID)

        // Assert
        assertThat(actual).isEqualTo(McpAgent.unknown(CLIENT_ID))
    }

    @Suppress("unused")
    private fun provideTestModels(): List<ApiResponseError> = listOf(
        ApiResponseError.NetworkException(),
        ApiResponseError.TimeoutException(),
        ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.NOT_FOUND,
            message = "Client not found",
            errorBody = null,
        ),
        ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.TOO_MANY_REQUESTS,
            message = null,
            errorBody = null,
        ),
    )

    @Suppress("UNCHECKED_CAST")
    private fun apiError(cause: ApiResponseError): ApiResponse<McpClientResponse> =
        ApiResponse.Error(cause) as ApiResponse<McpClientResponse>

    private companion object {
        const val CLIENT_ID = "https://claude.ai/.well-known/oauth-client"
    }
}
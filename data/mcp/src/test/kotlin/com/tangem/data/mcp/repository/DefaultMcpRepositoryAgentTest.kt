package com.tangem.data.mcp.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.mcp.api.McpApi
import com.tangem.data.mcp.api.models.response.McpClientResponse
import com.tangem.data.mcp.converter.McpAgentEntityConverter
import com.tangem.data.mcp.converter.McpClientConverter
import com.tangem.data.mcp.store.McpAgentStore
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.datastore.createAppPreferencesStore
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMcpRepositoryAgentTest {

    private val mcpApi: McpApi = mockk()
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private val appScope = object : AppCoroutineScope, CoroutineScope by testScope {}
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private lateinit var preferencesDataStore: DataStore<Preferences>
    private lateinit var repository: DefaultMcpRepository

    @BeforeEach
    fun setUp() {
        clearMocks(mcpApi)
        preferencesDataStore = MockStateDataStore(default = emptyPreferences())
        repository = createRepository()
    }

    @Test
    fun `GIVEN known client WHEN getAgent THEN the agent carries what the server knows`() = runTest {
        // Arrange
        coEvery { mcpApi.getClient(CLIENT_ID) } returns ApiResponse.Success(CLIENT_RESPONSE)

        // Act
        val actual = repository.getAgent(CLIENT_ID)

        // Assert
        assertThat(actual).isEqualTo(AGENT)
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
    fun `GIVEN lookup fails WHEN getAgent THEN the agent is unknown and not stored`(error: ApiResponseError) = runTest {
        // Arrange
        coEvery { mcpApi.getClient(CLIENT_ID) } returns apiError(error)

        // Act
        val actual = repository.getAgent(CLIENT_ID)

        // Assert
        assertThat(actual).isEqualTo(McpAgent.unknown(CLIENT_ID))
        assertThat(createStore().get(CLIENT_ID)).isNull()
    }

    @Test
    fun `GIVEN agent stored WHEN getAgent THEN the stored agent is returned without asking the server`() = runTest {
        // Arrange
        createStore().store(AGENT)

        // Act
        val actual = repository.getAgent(CLIENT_ID)

        // Assert
        assertThat(actual).isEqualTo(AGENT)
        coVerify(exactly = 0) { mcpApi.getClient(any()) }
    }

    @Test
    fun `GIVEN agent looked up WHEN getAgent on a new repository THEN the agent comes from the store`() = runTest {
        // Arrange
        coEvery { mcpApi.getClient(CLIENT_ID) } returns ApiResponse.Success(CLIENT_RESPONSE)
        repository.getAgent(CLIENT_ID)

        // Act
        val actual = createRepository().getAgent(CLIENT_ID)

        // Assert
        assertThat(actual).isEqualTo(AGENT)
        coVerify(exactly = 1) { mcpApi.getClient(CLIENT_ID) }
    }

    @Test
    fun `GIVEN lookup failed WHEN getAgent again THEN the server is asked again`() = runTest {
        // Arrange
        coEvery { mcpApi.getClient(CLIENT_ID) } returnsMany listOf(
            apiError(ApiResponseError.NetworkException()),
            ApiResponse.Success(CLIENT_RESPONSE),
        )
        repository.getAgent(CLIENT_ID)

        // Act
        val actual = repository.getAgent(CLIENT_ID)

        // Assert
        assertThat(actual).isEqualTo(AGENT)
        coVerify(exactly = 2) { mcpApi.getClient(CLIENT_ID) }
    }

    @Test
    fun `GIVEN other agent stored WHEN getAgent THEN the server is asked and both agents are kept`() = runTest {
        // Arrange
        val otherAgent = McpAgent(clientId = "other-client", origin = "other.example.com", name = "Other", iconUrl = null)
        createStore().store(otherAgent)
        coEvery { mcpApi.getClient(CLIENT_ID) } returns ApiResponse.Success(CLIENT_RESPONSE)

        // Act
        val actual = repository.getAgent(CLIENT_ID)

        // Assert
        assertThat(actual).isEqualTo(AGENT)
        val store = createStore()
        assertThat(listOf(store.get(CLIENT_ID), store.get("other-client"))).containsExactly(AGENT, otherAgent)
    }

    @Test
    fun `GIVEN stored agents unreadable WHEN getAgent THEN the server is asked and the agent is stored`() = runTest {
        // Arrange
        preferencesDataStore.updateData { preferences ->
            preferences.toMutablePreferences().apply { this[stringPreferencesKey(name = "mcpAgents")] = "{" }
        }
        coEvery { mcpApi.getClient(CLIENT_ID) } returns ApiResponse.Success(CLIENT_RESPONSE)

        // Act
        val actual = repository.getAgent(CLIENT_ID)

        // Assert
        assertThat(actual).isEqualTo(AGENT)
        assertThat(createStore().get(CLIENT_ID)).isEqualTo(AGENT)
    }

    @Test
    fun `GIVEN storage fails WHEN getAgent THEN the agent from the server is returned`() = runTest {
        // Arrange
        val failingDataStore: DataStore<Preferences> = mockk {
            every { data } returns flow { throw IOException("Disk is gone") }
            coEvery { updateData(any()) } throws IOException("Disk is gone")
        }
        preferencesDataStore = failingDataStore
        coEvery { mcpApi.getClient(CLIENT_ID) } returns ApiResponse.Success(CLIENT_RESPONSE)

        // Act
        val actual = createRepository().getAgent(CLIENT_ID)

        // Assert
        assertThat(actual).isEqualTo(AGENT)
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

    private fun createRepository() = DefaultMcpRepository(
        mcpApi = mcpApi,
        appScope = appScope,
        dispatchers = dispatchers,
        mcpClientConverter = McpClientConverter(),
        agentStore = createStore(),
    )

    private fun createStore() = McpAgentStore(
        appPreferencesStore = createAppPreferencesStore(
            dispatchers = dispatchers,
            preferencesDataStore = preferencesDataStore,
        ),
        json = Json,
        converter = McpAgentEntityConverter(),
    )

    @Suppress("UNCHECKED_CAST")
    private fun apiError(cause: ApiResponseError): ApiResponse<McpClientResponse> =
        ApiResponse.Error(cause) as ApiResponse<McpClientResponse>

    private companion object {
        const val CLIENT_ID = "https://claude.ai/.well-known/oauth-client"

        val CLIENT_RESPONSE = McpClientResponse(
            clientId = CLIENT_ID,
            clientOrigin = "claude.ai",
            clientName = "Claude Code",
            clientIconUrl = "https://icons.tangem.com/claude.png",
        )

        val AGENT = McpAgent(
            clientId = CLIENT_ID,
            origin = "claude.ai",
            name = "Claude Code",
            iconUrl = "https://icons.tangem.com/claude.png",
        )
    }
}
package com.tangem.domain.mcp.models.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.mcp.models.McpSession
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class McpSessionsAnalyticsEventTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN agent identity WHEN agent disconnected created THEN agent name falls back from origin to name`(
        model: AgentNameModel,
    ) {
        // Act
        val event = McpSessionsAnalyticsEvent.AgentDisconnected(session(origin = model.origin, name = model.name))

        // Assert
        assertThat(event.id to event.params)
            .isEqualTo("[MCP Sessions] Agent Disconnected" to mapOf("Agent Name" to model.expected))
    }

    private fun provideTestModels() = listOf(
        AgentNameModel(origin = "claude.ai", name = "Claude", expected = "claude.ai"),
        AgentNameModel(origin = "claude.ai", name = null, expected = "claude.ai"),
        AgentNameModel(origin = null, name = "Claude", expected = "Claude"),
        AgentNameModel(origin = null, name = null, expected = "Unknown"),
    )

    private fun session(origin: String?, name: String?) = McpSession(
        id = "session-id",
        walletId = UserWalletId("aabbcc"),
        createdAtMillis = 0L,
        clientId = "client-id",
        clientOrigin = origin,
        clientName = name,
        clientIconUrl = null,
        scopes = emptyList(),
        clientIp = null,
        clientCountry = null,
        clientUserAgent = null,
    )

    data class AgentNameModel(val origin: String?, val name: String?, val expected: String)
}
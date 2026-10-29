package com.tangem.domain.mcp.models.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.mcp.models.McpDeviceVerification
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class McpAuthorizationAnalyticsEventTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Contract {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN authorization event WHEN created THEN id and params match the spec`(model: ContractModel) {
            // Act
            val event = model.create()

            // Assert
            assertThat(event.id to event.params).isEqualTo(model.expectedId to model.expectedParams)
        }

        private fun provideTestModels() = listOf(
            ContractModel(
                name = "Connection Received",
                create = { McpAuthorizationAnalyticsEvent.ConnectionReceived() },
                expectedId = "[MCP Authorization] Connection Received",
                expectedParams = emptyMap(),
            ),
            ContractModel(
                name = "Agent Connection Requested",
                create = { McpAuthorizationAnalyticsEvent.AgentConnectionRequested(verification()) },
                expectedId = "[MCP Authorization] Agent Connection Requested",
                expectedParams = mapOf("Agent Name" to "claude.ai"),
            ),
            ContractModel(
                name = "Button - Approve",
                create = { McpAuthorizationAnalyticsEvent.ButtonApprove(verification()) },
                expectedId = "[MCP Authorization] Button - Approve",
                expectedParams = mapOf("Agent Name" to "claude.ai"),
            ),
            ContractModel(
                name = "Agent Connected",
                create = { McpAuthorizationAnalyticsEvent.AgentConnected(verification()) },
                expectedId = "[MCP Authorization] Agent Connected",
                expectedParams = mapOf("Agent Name" to "claude.ai"),
            ),
            ContractModel(
                name = "Button - Decline",
                create = { McpAuthorizationAnalyticsEvent.ButtonDecline(verification()) },
                expectedId = "[MCP Authorization] Button - Decline",
                expectedParams = mapOf("Agent Name" to "claude.ai"),
            ),
            ContractModel(
                name = "Authorization Failed",
                create = { McpAuthorizationAnalyticsEvent.AuthorizationFailed(errorCode = "404") },
                expectedId = "[MCP Authorization] Authorization Failed",
                expectedParams = mapOf("Error Code" to "404"),
            ),
            ContractModel(
                name = "Notice - Push Permission Required",
                create = { McpAuthorizationAnalyticsEvent.NoticePushPermissionRequired() },
                expectedId = "[MCP Authorization] Notice - Push Permission Required",
                expectedParams = emptyMap(),
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AgentName {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN agent identity WHEN agent event created THEN agent name falls back from origin to name`(
            model: AgentNameModel,
        ) {
            // Act
            val event = model.create(verification(origin = model.agentOrigin, name = model.agentName))

            // Assert
            assertThat(event.params).isEqualTo(mapOf("Agent Name" to model.expected))
        }

        private fun provideTestModels(): List<AgentNameModel> {
            val events = listOf<Pair<String, (McpDeviceVerification) -> AnalyticsEvent>>(
                "Agent Connection Requested" to McpAuthorizationAnalyticsEvent::AgentConnectionRequested,
                "Button - Approve" to McpAuthorizationAnalyticsEvent::ButtonApprove,
                "Agent Connected" to McpAuthorizationAnalyticsEvent::AgentConnected,
                "Button - Decline" to McpAuthorizationAnalyticsEvent::ButtonDecline,
            )
            val identities = listOf(
                Triple("claude.ai", "Claude", "claude.ai"),
                Triple("claude.ai", null, "claude.ai"),
                Triple(null, "Claude", "Claude"),
                Triple(null, null, "Unknown"),
            )

            return events.flatMap { (eventName, create) ->
                identities.map { (origin, name, expected) ->
                    AgentNameModel(
                        case = "$eventName, origin=$origin, name=$name",
                        create = create,
                        agentOrigin = origin,
                        agentName = name,
                        expected = expected,
                    )
                }
            }
        }
    }

    private fun verification(origin: String? = "claude.ai", name: String? = "Claude") = McpDeviceVerification(
        clientId = "client-id",
        clientOrigin = origin,
        clientName = name,
        clientIconUrl = null,
        scope = "scope",
        state = "state",
        userCode = "user-code",
    )

    data class ContractModel(
        val name: String,
        val create: () -> AnalyticsEvent,
        val expectedId: String,
        val expectedParams: Map<String, String>,
    ) {
        override fun toString(): String = name
    }

    data class AgentNameModel(
        val case: String,
        val create: (McpDeviceVerification) -> AnalyticsEvent,
        val agentOrigin: String?,
        val agentName: String?,
        val expected: String,
    ) {
        override fun toString(): String = case
    }
}
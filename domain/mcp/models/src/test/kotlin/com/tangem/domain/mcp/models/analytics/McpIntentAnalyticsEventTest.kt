package com.tangem.domain.mcp.models.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.domain.mcp.models.analytics.McpIntentAnalyticsEvent.IntentType
import com.tangem.domain.mcp.models.analytics.McpIntentAnalyticsEvent.Reason
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class McpIntentAnalyticsEventTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IntentReceived {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN intent source WHEN intent received built THEN it carries its type and source`(
            model: IntentReceivedModel,
        ) {
            // Act
            val event = McpIntentAnalyticsEvent.IntentReceived(intentId = INTENT_ID, source = model.source)

            // Assert
            assertThat(event.id).isEqualTo("[MCP Intent] Intent Received")
            assertThat(event.params).isEqualTo(
                mapOf("Intent Type" to model.expectedType, "Source" to model.expectedSource, "Intent Id" to INTENT_ID),
            )
        }

        private fun provideTestModels() = listOf(
            IntentReceivedModel(source = exchangeSource, expectedType = "exchange", expectedSource = "express"),
            IntentReceivedModel(source = sendSource, expectedType = "send", expectedSource = "transaction"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IntentScreenOpened {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN agent WHEN screen opened built THEN agent name is the one the sheet shows`(model: AgentNameModel) {
            // Act
            val event = McpIntentAnalyticsEvent.IntentScreenOpened(
                intentId = INTENT_ID,
                type = IntentType.Send,
                agent = McpAgent(clientId = "client", origin = model.origin, name = model.name, iconUrl = null),
                blockchain = "Ethereum",
                token = "USDT",
            )

            // Assert
            assertThat(event.id).isEqualTo("[MCP Intent] Intent Screen Opened")
            assertThat(event.params).isEqualTo(
                mapOf(
                    "Intent Type" to "send",
                    "Agent Name" to model.expectedAgentName,
                    "Blockchain" to "Ethereum",
                    "Token" to "USDT",
                    "Intent Id" to INTENT_ID,
                ),
            )
        }

        private fun provideTestModels() = listOf(
            AgentNameModel(origin = "mcp.claude.com", name = "Claude", expectedAgentName = "mcp.claude.com"),
            AgentNameModel(origin = null, name = "Claude", expectedAgentName = "Claude"),
            AgentNameModel(origin = null, name = null, expectedAgentName = "Unknown"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IntentLoadFailed {

        @Test
        fun `GIVEN error code WHEN load failed built THEN error code is sent`() {
            // Act
            val event = McpIntentAnalyticsEvent.IntentLoadFailed(
                intentId = INTENT_ID,
                type = IntentType.Exchange,
                reason = Reason.NotFound,
                errorCode = "2600",
            )

            // Assert
            assertThat(event.id).isEqualTo("[MCP Intent] Intent Load Failed")
            assertThat(event.params).isEqualTo(
                mapOf(
                    "Intent Type" to "exchange",
                    "Reason" to "Not Found",
                    "Error Code" to "2600",
                    "Intent Id" to INTENT_ID,
                ),
            )
        }

        @Test
        fun `GIVEN no error code WHEN load failed built THEN error code is left out`() {
            // Act
            val event = McpIntentAnalyticsEvent.IntentLoadFailed(
                intentId = INTENT_ID,
                type = IntentType.Send,
                reason = Reason.Unreachable,
            )

            // Assert
            assertThat(event.id).isEqualTo("[MCP Intent] Intent Load Failed")
            assertThat(event.params).isEqualTo(
                mapOf("Intent Type" to "send", "Reason" to "Unreachable", "Intent Id" to INTENT_ID),
            )
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN reason WHEN load failed built THEN reason is sent as the spec names it`(model: ReasonModel) {
            // Act
            val event = McpIntentAnalyticsEvent.IntentLoadFailed(
                intentId = INTENT_ID,
                type = IntentType.Send,
                reason = model.reason,
            )

            // Assert
            assertThat(event.params["Reason"]).isEqualTo(model.expectedValue)
        }

        @Test
        fun `GIVEN reason cases WHEN compared with the enum THEN every reason has a case`() {
            // Act
            val coveredReasons = provideTestModels().map(ReasonModel::reason)

            // Assert
            assertThat(coveredReasons).containsExactlyElementsIn(Reason.entries)
        }

        private fun provideTestModels() = listOf(
            ReasonModel(Reason.NotFound, "Not Found"),
            ReasonModel(Reason.WrongWallet, "Wrong Wallet"),
            ReasonModel(Reason.Unreachable, "Unreachable"),
            ReasonModel(Reason.AlreadyProcessed, "Already Processed"),
            ReasonModel(Reason.TokenMissing, "Token Missing"),
            ReasonModel(Reason.InvalidAmount, "Invalid Amount"),
            ReasonModel(Reason.SendNotOpened, "Send Not Opened"),
            ReasonModel(Reason.Unknown, "Unknown"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ButtonContinue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN intent type WHEN continue built THEN it carries the type`(model: IntentTypeModel) {
            // Act
            val event = McpIntentAnalyticsEvent.ButtonContinue(intentId = INTENT_ID, type = model.type)

            // Assert
            assertThat(event.id).isEqualTo("[MCP Intent] Button - Continue")
            assertThat(event.params).isEqualTo(mapOf("Intent Type" to model.expectedValue, "Intent Id" to INTENT_ID))
        }

        @Test
        fun `GIVEN intent type cases WHEN compared with the enum THEN every type has a case`() {
            // Act
            val coveredTypes = provideTestModels().map(IntentTypeModel::type)

            // Assert
            assertThat(coveredTypes).containsExactlyElementsIn(IntentType.entries)
        }

        private fun provideTestModels() = listOf(
            IntentTypeModel(IntentType.Exchange, "exchange"),
            IntentTypeModel(IntentType.Send, "send"),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IntentCompleted {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN modification WHEN completed built THEN intent modified is yes or no`(model: ModifiedModel) {
            // Act
            val event = McpIntentAnalyticsEvent.IntentCompleted(
                intentId = INTENT_ID,
                blockchain = "Ethereum",
                token = "USDT",
                isModified = model.isModified,
            )

            // Assert
            assertThat(event.id).isEqualTo("[MCP Intent] Intent Completed")
            assertThat(event.params).isEqualTo(
                mapOf(
                    "Intent Type" to "send",
                    "Blockchain" to "Ethereum",
                    "Token" to "USDT",
                    "Intent Modified" to model.expectedValue,
                    "Intent Id" to INTENT_ID,
                ),
            )
        }

        private fun provideTestModels() = listOf(
            ModifiedModel(isModified = true, expectedValue = "Yes"),
            ModifiedModel(isModified = false, expectedValue = "No"),
        )
    }

    @Nested
    inner class IntentReportFailed {

        @Test
        fun `GIVEN error code WHEN report failed built THEN it carries the send type and the code`() {
            // Act
            val event = McpIntentAnalyticsEvent.IntentReportFailed(intentId = INTENT_ID, errorCode = "no_hash")

            // Assert
            assertThat(event.id).isEqualTo("[MCP Intent] Intent Report Failed")
            assertThat(event.params).isEqualTo(
                mapOf("Intent Type" to "send", "Error Code" to "no_hash", "Intent Id" to INTENT_ID),
            )
        }
    }

    data class IntentReceivedModel(
        val source: McpRequest.Intent.Source,
        val expectedType: String,
        val expectedSource: String,
    )

    data class AgentNameModel(val origin: String?, val name: String?, val expectedAgentName: String)

    data class ReasonModel(val reason: Reason, val expectedValue: String)

    data class IntentTypeModel(val type: IntentType, val expectedValue: String)

    data class ModifiedModel(val isModified: Boolean, val expectedValue: String)

    private companion object {
        const val INTENT_ID = "my_intent_id123"

        val exchangeSource = McpRequest.Intent.Source.Express(McpRequest.Intent.Source.Express.Type.EXCHANGE)
        val sendSource = McpRequest.Intent.Source.Transaction(McpRequest.Intent.Source.Transaction.Type.SEND)
    }
}
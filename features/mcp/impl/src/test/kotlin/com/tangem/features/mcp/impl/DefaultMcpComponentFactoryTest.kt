package com.tangem.features.mcp.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.mcp.impl.pair.McpPairComponent
import com.tangem.features.send.api.mcp.SendIntentComponent
import com.tangem.features.swap.mcp.ExchangeIntentComponent
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMcpComponentFactoryTest {

    private val context: AppComponentContext = mockk()
    private val pairComponent: McpPairComponent = mockk()
    private val intentComponent: ExchangeIntentComponent = mockk()
    private val sendIntentComponent: SendIntentComponent = mockk()

    private val pairRequest = McpRequest.Pair(userCode = "WDJB-MJHT")
    private val intentRequest = McpRequest.Intent(
        id = "my_intent_id123",
        source = McpRequest.Intent.Source.Express(McpRequest.Intent.Source.Express.Type.EXCHANGE),
    )
    private val sendIntentRequest = McpRequest.Intent(
        id = "my_send_intent_id456",
        source = McpRequest.Intent.Source.Transaction(McpRequest.Intent.Source.Transaction.Type.SEND),
    )

    private val pairFactory: McpPairComponent.Factory = mockk {
        every { create(context, McpPairComponent.Params(pairRequest)) } returns pairComponent
    }
    private val intentFactory: ExchangeIntentComponent.Factory = mockk()
    private val sendIntentFactory: SendIntentComponent.Factory = mockk()

    private val factory = DefaultMcpComponentFactory(
        mcpPairComponentFactory = pairFactory,
        exchangeIntentComponentFactory = intentFactory,
        sendIntentComponentFactory = sendIntentFactory,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(intentFactory, sendIntentFactory)
    }

    @Test
    fun `GIVEN pair request WHEN create THEN pair component is returned`() {
        // Act
        val actual = factory.create(context, pairRequest)

        // Assert
        assertThat(actual).isSameInstanceAs(pairComponent)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN intent request WHEN create THEN component receives the requested wallet`(request: McpRequest.Intent) {
        // Arrange
        val expected = when (request.source) {
            is McpRequest.Intent.Source.Express -> {
                every {
                    intentFactory.create(
                        context,
                        ExchangeIntentComponent.Params(intentId = request.id, walletId = request.walletId),
                    )
                } returns intentComponent
                intentComponent
            }
            is McpRequest.Intent.Source.Transaction -> {
                every {
                    sendIntentFactory.create(
                        context,
                        SendIntentComponent.Params(intentId = request.id, walletId = request.walletId),
                    )
                } returns sendIntentComponent
                sendIntentComponent
            }
        }

        // Act
        val actual = factory.create(context, request)

        // Assert
        assertThat(actual).isSameInstanceAs(expected)
    }

    private fun provideTestModels() = listOf(intentRequest, sendIntentRequest).flatMap { request ->
        listOf(request, request.copy(walletId = UserWalletId("aabbcc")))
    }
}
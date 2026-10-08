package com.tangem.features.mcp.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.features.mcp.impl.intent.McpIntentComponent
import com.tangem.features.mcp.impl.pair.McpPairComponent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class DefaultMcpComponentFactoryTest {

    private val context: AppComponentContext = mockk()
    private val pairComponent: McpPairComponent = mockk()
    private val intentComponent: McpIntentComponent = mockk()

    private val pairRequest = McpRequest.Pair(userCode = "WDJB-MJHT")
    private val intentRequest = McpRequest.Intent(
        id = "my_intent_id123",
        source = McpRequest.Intent.Source.Express(McpRequest.Intent.Source.Express.Type.EXCHANGE),
    )

    private val pairFactory: McpPairComponent.Factory = mockk {
        every { create(context, McpPairComponent.Params(pairRequest)) } returns pairComponent
    }
    private val intentFactory: McpIntentComponent.Factory = mockk {
        every { create(context, McpIntentComponent.Params(intentRequest)) } returns intentComponent
    }

    private val factory = DefaultMcpComponentFactory(
        mcpPairComponentFactory = pairFactory,
        mcpIntentComponentFactory = intentFactory,
    )

    @Test
    fun `GIVEN pair request WHEN create THEN pair component is returned`() {
        // Act
        val actual = factory.create(context, pairRequest)

        // Assert
        assertThat(actual).isSameInstanceAs(pairComponent)
    }

    @Test
    fun `GIVEN intent request WHEN create THEN intent component is returned`() {
        // Act
        val actual = factory.create(context, intentRequest)

        // Assert
        assertThat(actual).isSameInstanceAs(intentComponent)
    }
}
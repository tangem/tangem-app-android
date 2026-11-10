package com.tangem.features.walletconnect.connections.routing

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WcInnerRouteTest {

    private val json = Json

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN mcp request in the slot WHEN app goes to background THEN slot state is saved and restored`(
        route: WcInnerRoute,
    ) {
        // Act
        val restored = json.decodeFromString(
            WcInnerRoute.serializer(),
            json.encodeToString(WcInnerRoute.serializer(), route),
        )

        // Assert
        assertThat(restored).isEqualTo(route)
    }

    private fun provideTestModels(): List<WcInnerRoute> = listOf(
        WcInnerRoute.McpRequestRoute(McpRequest.Pair(userCode = "ABCD")),
        WcInnerRoute.McpRequestRoute(
            McpRequest.Intent(
                id = "intent-id",
                source = McpRequest.Intent.Source.Express(McpRequest.Intent.Source.Express.Type.EXCHANGE),
            ),
        ),
        WcInnerRoute.McpRequestRoute(
            McpRequest.Intent(
                id = "intent-id",
                source = McpRequest.Intent.Source.Transaction(McpRequest.Intent.Source.Transaction.Type.SEND),
            ),
        ),
        WcInnerRoute.UnsupportedMethodAlert,
    )
}
package com.tangem.domain.mcp.models

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class McpRequestTest {

    private val json = Json

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN request WHEN saved and restored as the navigation state THEN it stays the same`(request: McpRequest) {
        // Act
        val restored = json.decodeFromString(
            McpRequest.serializer(),
            json.encodeToString(McpRequest.serializer(), request),
        )

        // Assert
        assertThat(restored).isEqualTo(request)
    }

    private fun provideTestModels() = listOf(
        McpRequest.Pair(userCode = "ABCD"),
        McpRequest.Intent(
            id = "intent-id",
            source = McpRequest.Intent.Source.Express(McpRequest.Intent.Source.Express.Type.EXCHANGE),
        ),
        McpRequest.Intent(
            id = "intent-id",
            source = McpRequest.Intent.Source.Transaction(McpRequest.Intent.Source.Transaction.Type.SEND),
        ),
    )
}
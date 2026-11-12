package com.tangem.data.mcp.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.mcp.api.models.response.McpIntentHistoryItemResponse
import com.tangem.data.mcp.api.models.response.McpIntentHistoryResponse
import com.tangem.domain.mcp.models.McpIntentHistoryItem
import com.tangem.domain.mcp.models.McpIntentHistoryPage
import com.tangem.domain.mcp.models.McpIntentStatus
import com.tangem.domain.mcp.models.McpRequest.Intent.Source
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class McpIntentHistoryConverterTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Item {

        @ParameterizedTest
        @ProvideTestModels
        fun toDomain(model: ItemModel) {
            // Arrange
            val response = page(item(source = model.source, status = model.status))

            // Act
            val actual = response.toDomain()

            // Assert
            assertThat(actual.items).isEqualTo(listOfNotNull(model.expected))
        }

        private fun provideTestModels() = listOf(
            ItemModel(
                source = "transaction",
                status = "CREATED",
                expected = domainItem(Source.Transaction(Source.Transaction.Type.SEND), McpIntentStatus.Created),
            ),
            ItemModel(
                source = "express",
                status = "COMPLETED",
                expected = domainItem(Source.Express(Source.Express.Type.EXCHANGE), McpIntentStatus.Completed),
            ),
            ItemModel(source = "yield", status = "CREATED", expected = null),
            ItemModel(source = "express", status = "EXPIRED", expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NextOffset {

        @Test
        fun `GIVEN more intents than read WHEN toDomain THEN next page starts after the read ones`() {
            // Arrange
            val response = McpIntentHistoryResponse(
                intents = listOf(item("express", "CREATED"), item("yield", "CREATED")),
                total = 5,
                limit = 2,
                offset = 2,
            )

            // Act
            val actual = response.toDomain()

            // Assert
            assertThat(actual.nextOffset).isEqualTo(4)
        }

        @Test
        fun `GIVEN last page WHEN toDomain THEN there is no next page`() {
            // Arrange
            val response = McpIntentHistoryResponse(
                intents = listOf(item("express", "CREATED")),
                total = 3,
                limit = 2,
                offset = 2,
            )

            // Act
            val actual = response.toDomain()

            // Assert
            assertThat(actual).isEqualTo(
                McpIntentHistoryPage(
                    items = listOf(domainItem(Source.Express(Source.Express.Type.EXCHANGE), McpIntentStatus.Created)),
                    nextOffset = null,
                ),
            )
        }

        @Test
        fun `GIVEN empty page that claims more WHEN toDomain THEN reading stops`() {
            // Arrange
            val response = McpIntentHistoryResponse(intents = emptyList(), total = 10, limit = 2, offset = 4)

            // Act
            val actual = response.toDomain()

            // Assert
            assertThat(actual.nextOffset).isNull()
        }
    }

    data class ItemModel(val source: String, val status: String, val expected: McpIntentHistoryItem?)

    private fun page(item: McpIntentHistoryItemResponse) =
        McpIntentHistoryResponse(intents = listOf(item), total = 1, limit = 20, offset = 0)

    private fun item(source: String, status: String) = McpIntentHistoryItemResponse(
        intentId = INTENT_ID,
        source = source,
        status = status,
        sessionId = SESSION_ID,
        createdAt = CREATED_AT_SECONDS,
    )

    private fun domainItem(source: Source, status: McpIntentStatus) = McpIntentHistoryItem(
        intentId = INTENT_ID,
        source = source,
        status = status,
        sessionId = SESSION_ID,
        createdAtMillis = CREATED_AT_SECONDS * 1000,
    )

    private companion object {
        const val INTENT_ID = "intent-id"
        const val SESSION_ID = "session-id"
        const val CREATED_AT_SECONDS = 1_789_117_759L
    }
}
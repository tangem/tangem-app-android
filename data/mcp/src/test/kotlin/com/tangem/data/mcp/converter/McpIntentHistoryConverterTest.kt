package com.tangem.data.mcp.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.data.mcp.api.models.response.McpIntentHistoryItemResponse
import com.tangem.data.mcp.api.models.response.McpIntentHistoryMetaResponse
import com.tangem.data.mcp.api.models.response.McpIntentHistoryResponse
import com.tangem.domain.mcp.models.McpIntentHistoryItem
import com.tangem.domain.mcp.models.McpIntentHistoryPage
import com.tangem.domain.mcp.models.McpIntentStatus
import com.tangem.domain.mcp.models.McpRequest.Intent.Source
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class McpIntentHistoryConverterTest {

    @Test
    fun `GIVEN backend history response WHEN decoded THEN items metadata and ISO timestamps are mapped`() {
        // Arrange
        val json = Json { ignoreUnknownKeys = true }
        val response = """
            {
              "items": [
                {
                  "intentId": "newer",
                  "source": "express",
                  "type": "exchange",
                  "status": "COMPLETED",
                  "deeplink": "tangem://intent?id=newer&source=express&type=exchange",
                  "sessionId": "session-id",
                  "createdAt": "2026-10-01T10:00:00.000Z"
                },
                {
                  "intentId": "older",
                  "source": "transaction",
                  "type": "send",
                  "status": "CREATED",
                  "deeplink": "tangem://intent?id=older&source=transaction&type=send",
                  "sessionId": "session-id",
                  "createdAt": "2026-09-30T08:15:42.317Z"
                }
              ],
              "meta": { "total": 2, "page": 1, "limit": 50, "hasNext": false }
            }
        """.trimIndent()

        // Act
        val actual = json.decodeFromString<McpIntentHistoryResponse>(response).toDomain()

        // Assert
        assertThat(actual).isEqualTo(
            McpIntentHistoryPage(
                items = listOf(
                    domainItem(
                        source = Source.Express(Source.Express.Type.EXCHANGE),
                        status = McpIntentStatus.Completed,
                        id = "newer",
                        createdAtMillis = 1_790_848_800_000L,
                    ),
                    domainItem(source = Source.Transaction(Source.Transaction.Type.SEND), id = "older"),
                ),
                nextPage = null,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Item {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN history item WHEN converted THEN only supported valid intents are retained`(model: ItemModel) {
            // Arrange
            val response = page(
                item(source = model.source, type = model.type, status = model.status, createdAt = model.createdAt),
            )

            // Act
            val actual = response.toDomain()

            // Assert
            assertThat(actual.items).isEqualTo(listOfNotNull(model.expected))
        }

        private fun provideTestModels() = listOf(
            ItemModel(
                source = "transaction",
                type = "send",
                expected = domainItem(Source.Transaction(Source.Transaction.Type.SEND)),
            ),
            ItemModel(
                source = "express",
                type = "exchange",
                status = "COMPLETED",
                expected = domainItem(Source.Express(Source.Express.Type.EXCHANGE), McpIntentStatus.Completed),
            ),
            ItemModel(source = "yield", type = "supply", expected = null),
            ItemModel(source = "express", type = "onramp", expected = null),
            ItemModel(source = "transaction", type = "exchange", expected = null),
            ItemModel(source = "express", type = "exchange", status = "EXPIRED", expected = null),
            ItemModel(source = "express", type = "exchange", createdAt = "not-a-date", expected = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NextPage {

        @Test
        fun `GIVEN more pages WHEN converted THEN metadata determines the next one-based page`() {
            // Arrange
            val response = McpIntentHistoryResponse(
                items = listOf(item(), item(source = "yield", type = "supply")),
                meta = McpIntentHistoryMetaResponse(total = 5, page = 2, limit = 2, hasNext = true),
            )

            // Act
            val actual = response.toDomain()

            // Assert
            assertThat(actual).isEqualTo(
                McpIntentHistoryPage(items = listOf(domainItem(Source.Express(Source.Express.Type.EXCHANGE))), nextPage = 3),
            )
        }

        @Test
        fun `GIVEN last page WHEN converted THEN there is no next page`() {
            // Arrange
            val response = McpIntentHistoryResponse(
                items = listOf(item()),
                meta = McpIntentHistoryMetaResponse(total = 3, page = 2, limit = 2, hasNext = false),
            )

            // Act
            val actual = response.toDomain()

            // Assert
            assertThat(actual).isEqualTo(
                McpIntentHistoryPage(
                    items = listOf(domainItem(Source.Express(Source.Express.Type.EXCHANGE))),
                    nextPage = null,
                ),
            )
        }

        @Test
        fun `GIVEN all items filtered out WHEN converted THEN the next page is retained`() {
            // Arrange
            val response = McpIntentHistoryResponse(
                items = listOf(item(source = "yield", type = "supply")),
                meta = McpIntentHistoryMetaResponse(total = 3, page = 1, limit = 1, hasNext = true),
            )

            // Act
            val actual = response.toDomain()

            // Assert
            assertThat(actual).isEqualTo(McpIntentHistoryPage(items = emptyList(), nextPage = 2))
        }
    }

    data class ItemModel(
        val source: String,
        val type: String,
        val status: String = "CREATED",
        val createdAt: String = CREATED_AT,
        val expected: McpIntentHistoryItem?,
    )

    private fun page(item: McpIntentHistoryItemResponse) = McpIntentHistoryResponse(
        items = listOf(item),
        meta = McpIntentHistoryMetaResponse(total = 1, page = 1, limit = 20, hasNext = false),
    )

    private fun item(
        source: String = "express",
        type: String = "exchange",
        status: String = "CREATED",
        createdAt: String = CREATED_AT,
    ) = McpIntentHistoryItemResponse(
        intentId = INTENT_ID,
        source = source,
        type = type,
        status = status,
        sessionId = SESSION_ID,
        createdAt = createdAt,
    )

    private fun domainItem(
        source: Source,
        status: McpIntentStatus = McpIntentStatus.Created,
        id: String = INTENT_ID,
        createdAtMillis: Long = CREATED_AT_MILLIS,
    ) = McpIntentHistoryItem(
        intentId = id,
        source = source,
        status = status,
        sessionId = SESSION_ID,
        createdAtMillis = createdAtMillis,
    )

    private companion object {
        const val INTENT_ID = "intent-id"
        const val SESSION_ID = "session-id"
        const val CREATED_AT = "2026-09-30T08:15:42.317Z"
        const val CREATED_AT_MILLIS = 1_790_756_142_317L
    }
}
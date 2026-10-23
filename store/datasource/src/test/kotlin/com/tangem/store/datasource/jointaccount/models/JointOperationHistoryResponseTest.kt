package com.tangem.store.datasource.jointaccount.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tangem.core.remote.moshi.SkipMalformedElementsAdapterFactory
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

/** The json bodies are the examples of the backend draft (joint-accounts-operation-history.openapi.yaml) */
internal class JointOperationHistoryResponseTest {

    private val moshi = Moshi.Builder()
        .add(SkipMalformedElementsAdapterFactory)
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `GIVEN the first history page WHEN parse THEN whole page matches contract`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "items": [
                {
                  "operationId": "6f1d0a2b-3c4e-4f5a-8b9c-0d1e2f3a4b5c",
                  "chainId": 8453,
                  "status": "awaiting_signatures",
                  "safeTx": {
                    "to": "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf",
                    "value": "1000000000000000",
                    "data": "0x",
                    "operation": 0,
                    "safeTxGas": "21000",
                    "baseGas": "30000",
                    "gasPrice": "2000000000",
                    "gasToken": "0x0000000000000000000000000000000000000000",
                    "refundReceiver": "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                    "nonce": "8"
                  },
                  "initiator": "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                  "threshold": 2,
                  "votes": [
                    {
                      "address": "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                      "vote": "approve",
                      "signature": "0x5b0c1b",
                      "votedAt": "2026-10-15T09:58:12.000Z"
                    }
                  ],
                  "createdAt": "2026-10-15T09:58:12.000Z",
                  "cryptoAccountId": "A7B3C9D2",
                  "safeAddress": "0x8f3ac21b7e5f4552091a69125d5dfcb7b8c26590",
                  "txHash": null,
                  "finishedAt": null,
                  "cancelReason": null
                }
              ],
              "pagination": {
                "endCursor": "eyJjIjoiMjAyNi0xMC0xNVQwOTo1ODoxMi4xMjM0NTYrMDAiLCJpIjoiNmYxZDBhMmIifQ",
                "startDeltaCursor": "eyJ1IjoiMjAyNi0xMC0xNVQwOTo1OTowMCswMCIsImkiOiIifQ",
                "hasMore": true
              }
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(JointOperationHistoryResponse::class.java).fromJson(json)

        // Assert
        val expected = JointOperationHistoryResponse(
            items = listOf(
                JointOperationHistoryItemDto(
                    operationId = "6f1d0a2b-3c4e-4f5a-8b9c-0d1e2f3a4b5c",
                    chainId = 8453,
                    safeTx = createSafeTx(
                        to = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf",
                        value = "1000000000000000",
                        data = "0x",
                        safeTxGas = "21000",
                        nonce = "8",
                    ),
                    status = "awaiting_signatures",
                    initiator = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                    createdAt = "2026-10-15T09:58:12.000Z",
                    votes = listOf(
                        JointOperationDto.VoteDto(
                            address = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                            vote = "approve",
                            votedAt = "2026-10-15T09:58:12.000Z",
                        ),
                    ),
                    cryptoAccountId = "A7B3C9D2",
                    safeAddress = "0x8f3ac21b7e5f4552091a69125d5dfcb7b8c26590",
                    txHash = null,
                    finishedAt = null,
                ),
            ),
            pagination = JointOperationHistoryResponse.Pagination(
                endCursor = "eyJjIjoiMjAyNi0xMC0xNVQwOTo1ODoxMi4xMjM0NTYrMDAiLCJpIjoiNmYxZDBhMmIifQ",
                startDeltaCursor = "eyJ1IjoiMjAyNi0xMC0xNVQwOTo1OTowMCswMCIsImkiOiIifQ",
                hasMore = true,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN an empty history WHEN parse THEN no items and no cursors`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "items": [],
              "pagination": { "endCursor": null, "startDeltaCursor": null, "hasMore": false }
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(JointOperationHistoryResponse::class.java).fromJson(json)

        // Assert
        val expected = JointOperationHistoryResponse(
            items = emptyList(),
            pagination = JointOperationHistoryResponse.Pagination(
                endCursor = null,
                startDeltaCursor = null,
                hasMore = false,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN a delta page with finished operations WHEN parse THEN whole page matches contract`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "items": [
                {
                  "operationId": "0b4f2c1e-6a3d-4f5b-9c8e-1d2a3b4c5d6e",
                  "chainId": 8453,
                  "status": "executed",
                  "safeTx": {
                    "to": "0x833589fcd6edb6e08f4c7c32d4f71b54bda02913",
                    "value": "0",
                    "data": "0xa9059cbb",
                    "operation": 0,
                    "safeTxGas": "65000",
                    "baseGas": "30000",
                    "gasPrice": "2000000000",
                    "gasToken": "0x0000000000000000000000000000000000000000",
                    "refundReceiver": "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                    "nonce": "7"
                  },
                  "initiator": "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                  "threshold": 2,
                  "votes": [
                    {
                      "address": "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                      "vote": "approve",
                      "signature": null,
                      "votedAt": "2026-09-08T09:20:37.000Z"
                    },
                    {
                      "address": "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf",
                      "vote": "approve",
                      "signature": null,
                      "votedAt": "2026-09-08T09:23:51.000Z"
                    }
                  ],
                  "createdAt": "2026-09-08T09:20:37.000Z",
                  "cryptoAccountId": "A7B3C9D2",
                  "safeAddress": "0x8f3ac21b7e5f4552091a69125d5dfcb7b8c26590",
                  "txHash": "0x4e3a3754410177e6937ef1f84bba68ea139e8d1a2258c5f85db9f1cd715a1bdd",
                  "finishedAt": "2026-09-08T09:24:02.000Z",
                  "cancelReason": null
                },
                {
                  "operationId": "6f1d0a2b-3c4e-4f5a-8b9c-0d1e2f3a4b5c",
                  "chainId": 8453,
                  "status": "cancelled",
                  "safeTx": {
                    "to": "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf",
                    "value": "1000000000000000",
                    "data": "0x",
                    "operation": 0,
                    "safeTxGas": "21000",
                    "baseGas": "30000",
                    "gasPrice": "2000000000",
                    "gasToken": "0x0000000000000000000000000000000000000000",
                    "refundReceiver": "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                    "nonce": "8"
                  },
                  "initiator": "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                  "threshold": 2,
                  "votes": [
                    {
                      "address": "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf",
                      "vote": "reject",
                      "signature": null,
                      "votedAt": "2026-10-15T10:02:00.000Z"
                    }
                  ],
                  "createdAt": "2026-10-15T09:58:12.000Z",
                  "cryptoAccountId": "A7B3C9D2",
                  "safeAddress": "0x8f3ac21b7e5f4552091a69125d5dfcb7b8c26590",
                  "txHash": null,
                  "finishedAt": "2026-10-15T10:02:00.000Z",
                  "cancelReason": "threshold"
                }
              ],
              "pagination": { "startCursor": "eyJ1IjoiMjAyNi0xMC0xNVQxMDowMjowMCswMCJ9", "hasMore": false }
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(JointOperationHistoryDeltaResponse::class.java).fromJson(json)

        // Assert
        val expected = JointOperationHistoryDeltaResponse(
            items = listOf(
                JointOperationHistoryItemDto(
                    operationId = "0b4f2c1e-6a3d-4f5b-9c8e-1d2a3b4c5d6e",
                    chainId = 8453,
                    safeTx = createSafeTx(
                        to = "0x833589fcd6edb6e08f4c7c32d4f71b54bda02913",
                        value = "0",
                        data = "0xa9059cbb",
                        safeTxGas = "65000",
                        nonce = "7",
                    ),
                    status = "executed",
                    initiator = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                    createdAt = "2026-09-08T09:20:37.000Z",
                    votes = listOf(
                        JointOperationDto.VoteDto(
                            address = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                            vote = "approve",
                            votedAt = "2026-09-08T09:20:37.000Z",
                        ),
                        JointOperationDto.VoteDto(
                            address = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf",
                            vote = "approve",
                            votedAt = "2026-09-08T09:23:51.000Z",
                        ),
                    ),
                    cryptoAccountId = "A7B3C9D2",
                    safeAddress = "0x8f3ac21b7e5f4552091a69125d5dfcb7b8c26590",
                    txHash = "0x4e3a3754410177e6937ef1f84bba68ea139e8d1a2258c5f85db9f1cd715a1bdd",
                    finishedAt = "2026-09-08T09:24:02.000Z",
                ),
                JointOperationHistoryItemDto(
                    operationId = "6f1d0a2b-3c4e-4f5a-8b9c-0d1e2f3a4b5c",
                    chainId = 8453,
                    safeTx = createSafeTx(
                        to = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf",
                        value = "1000000000000000",
                        data = "0x",
                        safeTxGas = "21000",
                        nonce = "8",
                    ),
                    status = "cancelled",
                    initiator = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                    createdAt = "2026-10-15T09:58:12.000Z",
                    votes = listOf(
                        JointOperationDto.VoteDto(
                            address = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf",
                            vote = "reject",
                            votedAt = "2026-10-15T10:02:00.000Z",
                        ),
                    ),
                    cryptoAccountId = "A7B3C9D2",
                    safeAddress = "0x8f3ac21b7e5f4552091a69125d5dfcb7b8c26590",
                    txHash = null,
                    finishedAt = "2026-10-15T10:02:00.000Z",
                ),
            ),
            pagination = JointOperationHistoryDeltaResponse.Pagination(
                startCursor = "eyJ1IjoiMjAyNi0xMC0xNVQxMDowMjowMCswMCJ9",
                hasMore = false,
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun createSafeTx(to: String, value: String, data: String, safeTxGas: String, nonce: String) =
        JointOperationDto.SafeTxDto(
            to = to,
            value = value,
            data = data,
            operation = 0,
            safeTxGas = safeTxGas,
            baseGas = "30000",
            gasPrice = "2000000000",
            gasToken = "0x0000000000000000000000000000000000000000",
            refundReceiver = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
            nonce = nonce,
        )
}
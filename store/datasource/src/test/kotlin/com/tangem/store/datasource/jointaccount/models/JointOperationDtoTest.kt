package com.tangem.store.datasource.jointaccount.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class JointOperationDtoTest {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun `GIVEN an operation json WHEN parse THEN whole object matches contract`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "operationId": "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
              "chainId": 137,
              "safeTx": {
                "to": "0x1111111111111111111111111111111111111111",
                "value": "115792089237316195423570985008687907853269984665640564039457584007913129639935",
                "data": "0x",
                "operation": 0,
                "safeTxGas": "112500",
                "baseGas": "410000",
                "gasPrice": "60000000000",
                "gasToken": "0x0000000000000000000000000000000000000000",
                "refundReceiver": "0x2222222222222222222222222222222222222222",
                "nonce": "4"
              },
              "status": "awaiting_signatures",
              "initiator": "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
              "threshold": 2,
              "votes": [
                {
                  "address": "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                  "vote": "approve",
                  "signature": "0x9284",
                  "votedAt": "2026-09-07T10:00:00Z"
                }
              ],
              "createdAt": "2026-09-07T10:00:00Z"
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(JointOperationDto::class.java).fromJson(json)

        // Assert
        val expected = JointOperationDto(
            operationId = "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
            chainId = 137,
            safeTx = JointOperationDto.SafeTxDto(
                to = "0x1111111111111111111111111111111111111111",
                value = "115792089237316195423570985008687907853269984665640564039457584007913129639935",
                data = "0x",
                operation = 0,
                safeTxGas = "112500",
                baseGas = "410000",
                gasPrice = "60000000000",
                gasToken = "0x0000000000000000000000000000000000000000",
                refundReceiver = "0x2222222222222222222222222222222222222222",
                nonce = "4",
            ),
            status = "awaiting_signatures",
            initiator = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
            createdAt = "2026-09-07T10:00:00Z",
            votes = listOf(
                JointOperationDto.VoteDto(
                    address = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf",
                    vote = "approve",
                    votedAt = "2026-09-07T10:00:00Z",
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN an operation with a digest field WHEN parse THEN the field has nowhere to land`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "operationId": "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
              "chainId": 1,
              "digest": "0xdeadbeef",
              "safeTx": {
                "to": "0x1111111111111111111111111111111111111111",
                "value": "0",
                "data": "0x",
                "operation": 0,
                "safeTxGas": "1",
                "baseGas": "1",
                "gasPrice": "1",
                "gasToken": "0x0000000000000000000000000000000000000000",
                "refundReceiver": "0x2222222222222222222222222222222222222222",
                "nonce": "0",
                "digest": "0xdeadbeef"
              },
              "status": "pending"
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(JointOperationDto::class.java).fromJson(json)

        // Assert: parsing succeeds and no field of the hierarchy carries the digest
        assertThat(actual).isNotNull()
        assertThat(JointOperationDto::class.java.declaredFields.map { it.name }).doesNotContain("digest")
        assertThat(JointOperationDto.SafeTxDto::class.java.declaredFields.map { it.name }).doesNotContain("digest")
    }

    @Test
    fun `GIVEN an account with operations WHEN parse THEN they ride inside the account`() {
        // Arrange
        @Language("JSON")
        val json = """
            {
              "cryptoAccountId": "4B2F1C8A9E7D6053A1B4C7E2F8D9A0B3C5E7F1A2D4B6C8E0F2A4B6C8D0E2F4A6",
              "membersCount": 2,
              "threshold": 2,
              "safeAddress": "0x5afe000000000000000000000000000000000001",
              "status": "active",
              "members": [],
              "operations": [
                {
                  "operationId": "3f1a2b4c-5d6e-7f80-91a2-b3c4d5e6f708",
                  "chainId": 1,
                  "safeTx": {
                    "to": "0x1111111111111111111111111111111111111111",
                    "value": "1",
                    "data": "0x",
                    "operation": 0,
                    "safeTxGas": "1",
                    "baseGas": "1",
                    "gasPrice": "1",
                    "gasToken": "0x0000000000000000000000000000000000000000",
                    "refundReceiver": "0x2222222222222222222222222222222222222222",
                    "nonce": "0"
                  },
                  "status": "awaiting_signatures"
                }
              ]
            }
        """.trimIndent()

        // Act
        val actual = moshi.adapter(JointAccountDto::class.java).fromJson(json)

        // Assert
        assertThat(actual?.operations).hasSize(1)
        assertThat(actual?.operations?.single()?.votes).isEmpty()
    }
}
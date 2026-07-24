package com.tangem.features.walletconnect.transaction.converter

import com.google.common.truth.Truth
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import kotlinx.collections.immutable.toImmutableList
import org.junit.jupiter.api.Test

class TransactionParamsConverterTest {

    private val converter = TransactionParamsConverter()

    @Test
    fun `GIVEN sign typed data transaction params WHEN convert THEN return correct data for UI`() {
        val value = """
            [
              "0xcd5F26C95e84279d0ce8E6dd9030d0b2171b6101",
              {
                "domain": {
                  "name": "Ether Mail",
                  "version": "1",
                  "chainId": 1,
                  "verifyingContract": "0xcccccccccccccccccccccccccccccccccccccccc"
                },
                "message": {
                  "from": {
                    "name": "Cow",
                    "wallet": "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826"
                  },
                  "to": {
                    "name": "Bob",
                    "wallet": "0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB"
                  },
                  "contents": "Hello, Bob!"
                },
                "primaryType": "Mail"
              }
            ]
        """.trimIndent()
        val expected = listOf(
            WcTransactionRequestBlockUM(
                info = listOf(
                    WcTransactionRequestInfoItemUM(
                        title = TextReference.Str("PrimaryType"),
                        description = "Mail",
                    ),
                ).toImmutableList(),
            ),
            WcTransactionRequestBlockUM(
                info = listOf(
                    WcTransactionRequestInfoItemUM(TextReference.Str("Domain")),
                    WcTransactionRequestInfoItemUM(TextReference.Str("chainId"), "1"),
                    WcTransactionRequestInfoItemUM(TextReference.Str("name"), "Ether Mail"),
                    WcTransactionRequestInfoItemUM(TextReference.Str("version"), "1"),
                    WcTransactionRequestInfoItemUM(
                        TextReference.Str("verifyingContract"),
                        "0xcccccccccccccccccccccccccccccccccccccccc",
                    ),
                ).toImmutableList(),
            ),
            WcTransactionRequestBlockUM(
                info = listOf(
                    WcTransactionRequestInfoItemUM(TextReference.Str("Message")),
                    WcTransactionRequestInfoItemUM(TextReference.Str("contents"), "Hello, Bob!"),
                ).toImmutableList(),
            ),
            WcTransactionRequestBlockUM(
                info = listOf(
                    WcTransactionRequestInfoItemUM(TextReference.Str("From")),
                    WcTransactionRequestInfoItemUM(
                        TextReference.Str("wallet"),
                        "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826",
                    ),
                    WcTransactionRequestInfoItemUM(TextReference.Str("name"), "Cow"),
                ).toImmutableList(),
            ),
            WcTransactionRequestBlockUM(
                info = listOf(
                    WcTransactionRequestInfoItemUM(TextReference.Str("To")),
                    WcTransactionRequestInfoItemUM(
                        TextReference.Str("wallet"),
                        "0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB",
                    ),
                    WcTransactionRequestInfoItemUM(TextReference.Str("name"), "Bob"),
                ).toImmutableList(),
            ),
        )
        Truth.assertThat(converter.convert(value)).isEqualTo(expected)
    }

    @Test
    fun `GIVEN EIP-2612 Permit typed data WHEN convert THEN spender and value are rendered`() {
        // Arrange — a Permit granting an attacker an unlimited (max uint256) allowance.
        val attacker = "0x00000000000000000000000000000000DeaDBeef"
        val maxUint256 = "115792089237316195423570985008687907853269984665640564039457584007913129639935"
        val value = """
            [
              "0x1120387688B85249e6Aa9542Be01b123bb9471d9",
              {
                "domain": {
                  "name": "USD Coin",
                  "version": "2",
                  "chainId": 1,
                  "verifyingContract": "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
                },
                "message": {
                  "owner": "0x1120387688B85249e6Aa9542Be01b123bb9471d9",
                  "spender": "$attacker",
                  "value": "$maxUint256",
                  "nonce": 0,
                  "deadline": "1999999999"
                },
                "primaryType": "Permit"
              }
            ]
        """.trimIndent()

        // Act
        val items = converter.convert(value).flatMap { it.info }

        // Assert — the dangerous fields must be visible to the user.
        Truth.assertThat(items).containsAtLeast(
            WcTransactionRequestInfoItemUM(TextReference.Str("spender"), attacker),
            WcTransactionRequestInfoItemUM(TextReference.Str("value"), maxUint256),
        )
    }

    @Test
    fun `GIVEN nested structs and scalar arrays WHEN convert THEN deep fields and array items are rendered`() {
        // Arrange — a Permit2-style payload: a deeply-nested amount and a scalar array of recipients.
        val value = """
            [
              "0x1120387688B85249e6Aa9542Be01b123bb9471d9",
              {
                "domain": { "name": "Permit2", "chainId": 1 },
                "message": {
                  "spender": "0x00000000000000000000000000000000DeaDBeef",
                  "sigDeadline": "1999999999",
                  "details": [
                    { "token": "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", "amount": "1461501637330902918203684832716283019655932542975" }
                  ],
                  "recipients": ["0xAAA0000000000000000000000000000000000001", "0xBBB0000000000000000000000000000000000002"]
                },
                "primaryType": "PermitBatch"
              }
            ]
        """.trimIndent()

        // Act
        val items = converter.convert(value).flatMap { it.info }

        // Assert — the spender, the depth-2 nested amount, and the scalar-array elements are all visible.
        Truth.assertThat(items).containsAtLeast(
            WcTransactionRequestInfoItemUM(
                TextReference.Str("spender"),
                "0x00000000000000000000000000000000DeaDBeef",
            ),
            WcTransactionRequestInfoItemUM(
                TextReference.Str("amount"),
                "1461501637330902918203684832716283019655932542975",
            ),
            WcTransactionRequestInfoItemUM(
                TextReference.Str("recipients[0]"),
                "0xAAA0000000000000000000000000000000000001",
            ),
            WcTransactionRequestInfoItemUM(
                TextReference.Str("recipients[1]"),
                "0xBBB0000000000000000000000000000000000002",
            ),
        )
    }

    @Test
    fun `GIVEN send or approve transaction params WHEN convert THEN return correct data for UI`() {
        val value = """
            [
              {
                "data": "0x095ea7b3000000000000000000000000f0d4c12a5768d806021f80a262b4d39d26c58b8dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "from": "0xcd5F26C95e84279d0ce8E6dd9030d0b2171b6101",
                "to": "0x7d1afa7b718fb893db30a3abc0cfc608aacfebb0",
                "value": "0x0"
              }
            ]
        """.trimIndent()

        val expected = listOf(
            WcTransactionRequestBlockUM(
                info = listOf(
                    WcTransactionRequestInfoItemUM(
                        title = TextReference.Str("Data"),
                        description = "0x095ea7b3000000000000000000000000f0d4c12a5768d806021f80a262b4d39d26c58b8dffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                    ),
                ).toImmutableList(),
            ),
            WcTransactionRequestBlockUM(
                info = listOf(
                    WcTransactionRequestInfoItemUM(
                        title = TextReference.Str("From"),
                        description = "0xcd5F26C95e84279d0ce8E6dd9030d0b2171b6101",
                    ),
                ).toImmutableList(),
            ),
            WcTransactionRequestBlockUM(
                info = listOf(
                    WcTransactionRequestInfoItemUM(
                        title = TextReference.Str("To"),
                        description = "0x7d1afa7b718fb893db30a3abc0cfc608aacfebb0",
                    ),
                ).toImmutableList(),
            ),
            WcTransactionRequestBlockUM(
                info = listOf(
                    WcTransactionRequestInfoItemUM(
                        title = TextReference.Str("Value"),
                        description = "0x0",
                    ),
                ).toImmutableList(),
            ),
        )

        Truth.assertThat(converter.convert(value)).isEqualTo(expected)
    }

    @Test
    fun `GIVEN Permit2 PermitBatch WHEN convert THEN every detail object is rendered`() {
        // Arrange — an array of struct objects (PermitBatch.details): each element must produce a block.
        val value = """
            [
              "0x1120387688B85249e6Aa9542Be01b123bb9471d9",
              {
                "domain": { "name": "Permit2", "chainId": 1 },
                "message": {
                  "spender": "0x00000000000000000000000000000000DeaDBeef",
                  "details": [
                    { "token": "0xAAA0000000000000000000000000000000000001", "amount": "111" },
                    { "token": "0xBBB0000000000000000000000000000000000002", "amount": "222" }
                  ]
                },
                "primaryType": "PermitBatch"
              }
            ]
        """.trimIndent()

        // Act
        val items = converter.convert(value).flatMap { it.info }

        // Assert — both nested detail objects survive (token + amount of each element).
        Truth.assertThat(items).containsAtLeast(
            WcTransactionRequestInfoItemUM(TextReference.Str("token"), "0xAAA0000000000000000000000000000000000001"),
            WcTransactionRequestInfoItemUM(TextReference.Str("amount"), "111"),
            WcTransactionRequestInfoItemUM(TextReference.Str("token"), "0xBBB0000000000000000000000000000000000002"),
            WcTransactionRequestInfoItemUM(TextReference.Str("amount"), "222"),
        )
    }

    @Test
    fun `GIVEN malformed JSON WHEN convert THEN returns empty and does not throw`() {
        // Act — invalid JSON must be swallowed (logged), never crash the signing screen.
        val result = converter.convert("{ this is : not valid json ]")

        // Assert
        Truth.assertThat(result).isEmpty()
    }

    @Test
    fun `GIVEN empty array WHEN convert THEN returns empty`() {
        Truth.assertThat(converter.convert("[]")).isEmpty()
    }

    @Test
    fun `GIVEN blank input WHEN convert THEN returns empty and does not throw`() {
        Truth.assertThat(converter.convert("")).isEmpty()
    }
}
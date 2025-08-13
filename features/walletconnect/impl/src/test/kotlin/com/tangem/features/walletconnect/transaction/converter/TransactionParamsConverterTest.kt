package com.tangem.features.walletconnect.transaction.converter

import com.google.common.truth.Truth
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestBlockUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoItemUM
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test

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
                    WcTransactionRequestInfoItemUM(TextReference.Str("Contents"), "Hello, Bob!"),
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
}
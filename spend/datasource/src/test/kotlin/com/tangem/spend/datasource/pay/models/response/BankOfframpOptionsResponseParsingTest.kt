package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BankOfframpOptionsResponseParsingTest {

    private val json = NetworkJsonFactory.create()

    @Test
    fun `GIVEN SPEI destination WHEN parsed THEN rail, currency and fee calculation type read`() {
        // Arrange
        val raw = """{"result":{"sources":[],"destinations":[{"rail":"SPEI","currency":"MXN",""" +
            """"fee":{"name":"SPEI","calculation_type":"PERCENTAGE","amount":1,"currency":"USD",""" +
            """"description":"SPEI offramp fee"},"eta":{"min_days":0,"max_days":0}}]},"error":null}"""

        // Act
        val actual = json.decodeFromString(BankOfframpOptionsResponse.serializer(), raw)

        // Assert
        assertThat(actual).isEqualTo(
            BankOfframpOptionsResponse(
                result = BankOfframpOptionsResponse.Result(
                    sources = emptyList(),
                    destinations = listOf(
                        BankOfframpOptionsResponse.Destination(
                            rail = "SPEI",
                            currency = "MXN",
                            fee = BankOfframpOptionsResponse.Fee(
                                name = "SPEI",
                                calculationType = "PERCENTAGE",
                                amount = BigDecimal("1"),
                                currency = "USD",
                                description = "SPEI offramp fee",
                            ),
                            eta = BankOfframpOptionsResponse.Eta(minDays = 0, maxDays = 0),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN destination without currency and calculation type WHEN parsed THEN both are null`() {
        // Arrange
        val raw = """{"result":{"destinations":[{"rail":"ACH","fee":{"name":"ACH","amount":1.50,""" +
            """"currency":"USD"},"eta":{"min_days":1,"max_days":3}}]},"error":null}"""

        // Act
        val actual = json.decodeFromString(BankOfframpOptionsResponse.serializer(), raw)

        // Assert
        assertThat(actual).isEqualTo(
            BankOfframpOptionsResponse(
                result = BankOfframpOptionsResponse.Result(
                    sources = null,
                    destinations = listOf(
                        BankOfframpOptionsResponse.Destination(
                            rail = "ACH",
                            currency = null,
                            fee = BankOfframpOptionsResponse.Fee(
                                name = "ACH",
                                calculationType = null,
                                amount = BigDecimal("1.50"),
                                currency = "USD",
                                description = null,
                            ),
                            eta = BankOfframpOptionsResponse.Eta(minDays = 1, maxDays = 3),
                        ),
                    ),
                ),
            ),
        )
    }
}
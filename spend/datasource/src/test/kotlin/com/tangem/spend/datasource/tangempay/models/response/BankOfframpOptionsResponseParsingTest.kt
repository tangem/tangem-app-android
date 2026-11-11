package com.tangem.spend.datasource.tangempay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BankOfframpOptionsResponseParsingTest {

    private val json = NetworkJsonFactory.create()

    @Test
    fun `GIVEN SPEI destination WHEN parsed THEN rail, currency, both fee parts and amount limits read`() {
        // Arrange
        val raw = """{"result":{"sources":[],"destinations":[{"rail":"SPEI","currency":"MXN",""" +
            """"fee":{"type":"SPEI_OFFRAMP","group":"OFFRAMP","name":"SPEI","calculation_type":"PERCENTAGE",""" +
            """"amount":1,"fixed_amount":0.5,"currency":"USD","description":"SPEI offramp fee"},""" +
            """"eta":{"min_days":0,"max_days":0},"amount_limits":{"min":10,"max":null,"currency":"USD"}}]},""" +
            """"error":null}"""

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
                                fixedAmount = BigDecimal("0.5"),
                                currency = "USD",
                                description = "SPEI offramp fee",
                            ),
                            eta = BankOfframpOptionsResponse.Eta(minDays = 0, maxDays = 0),
                            amountLimits = AmountLimitsResponse(min = BigDecimal("10"), max = null, currency = "USD"),
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN destination without currency, calculation type and limits WHEN parsed THEN they are null`() {
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
                                fixedAmount = null,
                                currency = "USD",
                                description = null,
                            ),
                            eta = BankOfframpOptionsResponse.Eta(minDays = 1, maxDays = 3),
                            amountLimits = null,
                        ),
                    ),
                ),
            ),
        )
    }
}
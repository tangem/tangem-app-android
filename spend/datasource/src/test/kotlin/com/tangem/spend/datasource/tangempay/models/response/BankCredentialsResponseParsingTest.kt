package com.tangem.spend.datasource.tangempay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BankCredentialsResponseParsingTest {

    private val json = NetworkJsonFactory.create()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN bank credentials JSON WHEN parsed THEN the expected response is built`(model: ParsingModel) {
        // Act
        val actual = json.decodeFromString(BankCredentialsResponse.serializer(), model.raw)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN all rails JSON WHEN parsed THEN one result per route in response order`() {
        // Arrange
        val raw = """{"result":[{$BASE_FIELDS,"rail":"ACH"},""" +
            """{"type":"fiat","account_number":"646180157000000004","rail":"SPEI",""" +
            """"fee":{"type":"variable","amount":"1.00"},"deposit_fee":{"type":"SPEI_ONRAMP",""" +
            """"group":"ONRAMP","name":"SPEI","calculation_type":"PERCENTAGE","amount":1.00,"fixed_amount":10,""" +
            """"currency":"MXN","description":"SPEI onramp fee"},"amount_limits":{"min":2,"max":null,""" +
            """"currency":"USD"},"exchange_rate":{"rate":18.541575,"base":"USD","quote":"MXN"}},""" +
            """{"type":"onchain","address":"0x5aE3fA2b1c9D8e7F6a5B4c3D2e1F0a9B8c7D6e5F"}],"error":null}"""

        // Act
        val actual = json.decodeFromString(BankCredentialsListResponse.serializer(), raw)

        // Assert
        val expected = BankCredentialsListResponse(
            result = listOf(
                baseResult().copy(rail = "ACH"),
                emptyResult().copy(
                    type = "fiat",
                    accountNumber = "646180157000000004",
                    rail = "SPEI",
                    fee = fee(type = "variable", amount = "1.00"),
                    depositFee = fee(
                        type = "SPEI_ONRAMP",
                        group = "ONRAMP",
                        name = "SPEI",
                        calculationType = "PERCENTAGE",
                        amount = "1.00",
                        fixedAmount = "10",
                        currency = "MXN",
                        description = "SPEI onramp fee",
                    ),
                    amountLimits = AmountLimitsResponse(min = BigDecimal("2"), max = null, currency = "USD"),
                    exchangeRate = BankCredentialsResponse.ExchangeRate(
                        rate = "18.541575",
                        base = "USD",
                        quote = "MXN",
                    ),
                ),
                emptyResult().copy(type = "onchain"),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        ParsingModel(
            raw = """{"result":{$BASE_FIELDS,"rail":"ACH","transfer_message":"REF-A1B2C3D4",""" +
                """"fee":{"type":"flat","amount":"1.00"},"deposit_limit":{"amount":"10000","period":"day"}},""" +
                """"error":null}""",
            expected = BankCredentialsResponse(
                result = baseResult().copy(
                    rail = "ACH",
                    transferMessage = "REF-A1B2C3D4",
                    fee = fee(type = "flat", amount = "1.00"),
                    depositLimit = BankCredentialsResponse.DepositLimit(amount = "10000", period = "day"),
                ),
            ),
        ),
        ParsingModel(
            raw = """{"result":{$BASE_FIELDS,"rail":"ACH","fee":{"type":"ACH_ONRAMP","group":"ONRAMP",""" +
                """"name":"ACH","calculation_type":"FIXED","amount":0.00,"fixed_amount":null,"currency":"USD",""" +
                """"description":"ACH onramp fee"},"amount_limits":{"min":2,"max":null,"currency":"USD"}},""" +
                """"error":null}""",
            expected = BankCredentialsResponse(
                result = baseResult().copy(
                    rail = "ACH",
                    fee = fee(
                        type = "ACH_ONRAMP",
                        group = "ONRAMP",
                        name = "ACH",
                        calculationType = "FIXED",
                        amount = "0.00",
                        currency = "USD",
                        description = "ACH onramp fee",
                    ),
                    amountLimits = AmountLimitsResponse(min = BigDecimal("2"), max = null, currency = "USD"),
                ),
            ),
        ),
        ParsingModel(
            raw = """{"result":{$BASE_FIELDS,"rail":"ACH","fee":{"type":"flat","amount":"0.00"},""" +
                """"deposit_limit":{"amount":10000,"period":"day","currency":"USD"},""" +
                """"exchange_rate":{"rate":17.25,"base":"USD","quote":"MXN"}},"error":null}""",
            expected = BankCredentialsResponse(
                result = baseResult().copy(
                    rail = "ACH",
                    fee = fee(type = "flat", amount = "0.00"),
                    depositLimit = BankCredentialsResponse.DepositLimit(amount = "10000", period = "day"),
                    exchangeRate = BankCredentialsResponse.ExchangeRate(rate = "17.25", base = "USD", quote = "MXN"),
                ),
            ),
        ),
        ParsingModel(
            raw = """{"result":{$BASE_FIELDS},"error":null}""",
            expected = BankCredentialsResponse(result = baseResult()),
        ),
        ParsingModel(
            raw = """{"result":{$BASE_FIELDS,"rail":null,"transfer_message":null,"fee":null,""" +
                """"deposit_fee":null,"deposit_limit":null,"amount_limits":null},"error":null}""",
            expected = BankCredentialsResponse(result = baseResult()),
        ),
        ParsingModel(
            raw = """{"result":null,"error":null}""",
            expected = BankCredentialsResponse(result = null),
        ),
    )

    private fun baseResult() = BankCredentialsResponse.Result(
        type = "fiat",
        beneficiaryName = "RAIN PAYMENTS INC",
        beneficiaryAddress = "123 Finance St, New York, NY 10001, US",
        beneficiaryBankName = "Partner Bank, N.A.",
        beneficiaryBankAddress = "456 Bank Ave, New York, NY 10002, US",
        accountNumber = "9876543210",
        routingNumber = "021000021",
        rail = null,
        transferMessage = null,
        fee = null,
        depositFee = null,
        depositLimit = null,
        amountLimits = null,
        exchangeRate = null,
    )

    private fun emptyResult() = BankCredentialsResponse.Result(
        type = null,
        beneficiaryName = null,
        beneficiaryAddress = null,
        beneficiaryBankName = null,
        beneficiaryBankAddress = null,
        accountNumber = null,
        routingNumber = null,
        rail = null,
        transferMessage = null,
        fee = null,
        depositFee = null,
        depositLimit = null,
        amountLimits = null,
        exchangeRate = null,
    )

    @Suppress("LongParameterList")
    private fun fee(
        type: String? = null,
        group: String? = null,
        name: String? = null,
        calculationType: String? = null,
        amount: String,
        fixedAmount: String? = null,
        currency: String? = null,
        description: String? = null,
    ) = FeesResponse.Fee(
        type = type,
        group = group,
        name = name,
        paymentMethodId = null,
        calculationType = calculationType,
        amount = BigDecimal(amount),
        fixedAmount = fixedAmount?.let(::BigDecimal),
        currency = currency,
        description = description,
    )

    data class ParsingModel(val raw: String, val expected: BankCredentialsResponse)

    private companion object {
        const val BASE_FIELDS = """"type":"fiat","beneficiary_name":"RAIN PAYMENTS INC",""" +
            """"beneficiary_address":"123 Finance St, New York, NY 10001, US",""" +
            """"beneficiary_bank_name":"Partner Bank, N.A.",""" +
            """"beneficiary_bank_address":"456 Bank Ave, New York, NY 10002, US",""" +
            """"account_number":"9876543210","routing_number":"021000021""""
    }
}
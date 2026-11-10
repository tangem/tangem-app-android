package com.tangem.spend.datasource.tangempay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

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
            """"fee":{"type":"variable","amount":"1.00"},"deposit_limit":{"amount":10000,"period":"day",""" +
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
                    fee = BankCredentialsResponse.Fee(type = "variable", amount = "1.00"),
                    depositLimit = BankCredentialsResponse.DepositLimit(amount = "10000", period = "day"),
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
                    fee = BankCredentialsResponse.Fee(type = "flat", amount = "1.00"),
                    depositLimit = BankCredentialsResponse.DepositLimit(amount = "10000", period = "day"),
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
                    fee = BankCredentialsResponse.Fee(type = "flat", amount = "0.00"),
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
                """"deposit_limit":null},"error":null}""",
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
        depositLimit = null,
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
        depositLimit = null,
        exchangeRate = null,
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
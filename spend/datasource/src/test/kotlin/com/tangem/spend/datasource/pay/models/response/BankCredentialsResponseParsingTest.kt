package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.test.core.ProvideTestModels
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
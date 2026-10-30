package com.tangem.spend.datasource.pay.models

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.spend.datasource.pay.models.request.BankOfframpPlaceOrderRequest
import com.tangem.spend.datasource.pay.models.request.BankOfframpRouteRequest
import com.tangem.spend.datasource.pay.models.response.BankOfframpOptionsResponse
import com.tangem.spend.datasource.pay.models.response.BankOfframpRouteResponse
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class BankOfframpRouteSerializationTest {

    private val json = NetworkJsonFactory.create()

    @Nested
    inner class Request {

        @Test
        fun `GIVEN saved recipient WHEN serialized THEN only the product instance and the bank account id sent`() {
            // Arrange
            val request = BankOfframpRouteRequest(
                productInstanceId = PRODUCT_INSTANCE_ID,
                bankAccountId = BANK_ACCOUNT_ID,
            )

            // Act
            val actual = serialize(request)

            // Assert
            assertThat(actual).isEqualTo(
                parse("""{"product_instance_id":"$PRODUCT_INSTANCE_ID","bank_account_id":"$BANK_ACCOUNT_ID"}"""),
            )
        }

        @Test
        fun `GIVEN new recipient WHEN serialized THEN only the product instance and the bank account sent`() {
            // Arrange
            val request = BankOfframpRouteRequest(productInstanceId = PRODUCT_INSTANCE_ID, bankAccount = bankAccount())

            // Act
            val actual = serialize(request)

            // Assert
            assertThat(actual).isEqualTo(
                parse("""{"product_instance_id":"$PRODUCT_INSTANCE_ID","bank_account":$EXPECTED_BANK_ACCOUNT}"""),
            )
        }

        private fun bankAccount() = BankOfframpPlaceOrderRequest.BankAccount(
            details = BankOfframpPlaceOrderRequest.Details(
                currency = "USD",
                rail = "ACH",
                isThirdParty = false,
                beneficiary = BankOfframpPlaceOrderRequest.Beneficiary(
                    type = "INDIVIDUAL",
                    firstName = "John",
                    lastName = "Doe",
                ),
                beneficiaryAddress = address(),
                bankName = "Chase Bank",
                bankAddress = address(),
                accountNumber = "1234567890",
                routingNumber = "021000021",
                bankAccountType = "CHECKING",
            ),
        )

        private fun address() = BankOfframpPlaceOrderRequest.Address(
            line1 = "123 Main St",
            city = "New York",
            region = "NY",
            postalCode = "10001",
            countryCode = "US",
        )

        private fun serialize(request: BankOfframpRouteRequest): JsonObject {
            return json.encodeToJsonElement(BankOfframpRouteRequest.serializer(), request).jsonObject
        }

        private fun parse(raw: String): JsonObject = json.parseToJsonElement(raw).jsonObject
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Response {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN route with or without an attempt id WHEN parsed THEN the route fields read`(raw: String) {
            // Act
            val actual = json.decodeFromString(BankOfframpRouteResponse.serializer(), raw)

            // Assert
            assertThat(actual).isEqualTo(
                BankOfframpRouteResponse(
                    result = BankOfframpRouteResponse.Result(
                        depositAddress = DEPOSIT_ADDRESS,
                        fee = BankOfframpOptionsResponse.Fee(
                            name = "ACH",
                            calculationType = "FIXED",
                            amount = BigDecimal("1.50"),
                            currency = "USD",
                            description = "ACH offramp fee",
                        ),
                        eta = BankOfframpOptionsResponse.Eta(minDays = 1, maxDays = 3),
                        bankAccountId = BANK_ACCOUNT_ID,
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN route with the deposit address only WHEN parsed THEN the other fields are null`() {
            // Arrange
            val raw = """{"result":{"deposit_address":"$DEPOSIT_ADDRESS"},"error":null}"""

            // Act
            val actual = json.decodeFromString(BankOfframpRouteResponse.serializer(), raw)

            // Assert
            assertThat(actual).isEqualTo(
                BankOfframpRouteResponse(
                    result = BankOfframpRouteResponse.Result(
                        depositAddress = DEPOSIT_ADDRESS,
                        fee = null,
                        eta = null,
                        bankAccountId = null,
                    ),
                ),
            )
        }

        private fun provideTestModels() = listOf(
            routeJson(),
            routeJson(attemptId = "5c1e9f0a-2b7d-4e8f-9a6b-3d2c1b0a9f8e"),
        )

        private fun routeJson(attemptId: String? = null): String {
            val attemptIdField = attemptId?.let { ",\"attempt_id\":\"$it\"" }.orEmpty()
            return """{"result":{"deposit_address":"$DEPOSIT_ADDRESS","fee":{"name":"ACH",""" +
                """"calculation_type":"FIXED","amount":1.50,"currency":"USD","description":"ACH offramp fee"},""" +
                """"eta":{"min_days":1,"max_days":3},"bank_account_id":"$BANK_ACCOUNT_ID"$attemptIdField},""" +
                """"error":null}"""
        }
    }

    private companion object {
        const val PRODUCT_INSTANCE_ID = "123e4567-e89b-12d3-a456-426614174000"
        const val BANK_ACCOUNT_ID = "7b0f7d70-ec8f-4d4d-b3f1-41f52b6f4d10"
        const val DEPOSIT_ADDRESS = "0x1234567890abcdef1234567890abcdef12345678"

        val EXPECTED_BANK_ACCOUNT = """{"details":{"currency":"USD","rail":"ACH","third_party":false,""" +
            """"beneficiary":{"type":"INDIVIDUAL","first_name":"John","last_name":"Doe"},""" +
            """"beneficiary_address":{"line1":"123 Main St","city":"New York","region":"NY",""" +
            """"postal_code":"10001","country_code":"US"},"bank_name":"Chase Bank",""" +
            """"bank_address":{"line1":"123 Main St","city":"New York","region":"NY",""" +
            """"postal_code":"10001","country_code":"US"},"account_number":"1234567890",""" +
            """"routing_number":"021000021","bank_account_type":"CHECKING"}}"""
    }
}
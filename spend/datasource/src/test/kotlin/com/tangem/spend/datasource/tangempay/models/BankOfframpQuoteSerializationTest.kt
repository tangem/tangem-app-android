package com.tangem.spend.datasource.tangempay.models

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.spend.datasource.tangempay.models.request.BankOfframpPlaceOrderRequest
import com.tangem.spend.datasource.tangempay.models.request.BankOfframpQuoteRequest
import com.tangem.spend.datasource.tangempay.models.request.BankOfframpTransferRequest
import com.tangem.spend.datasource.tangempay.models.response.BankOfframpOptionsResponse
import com.tangem.spend.datasource.tangempay.models.response.BankOfframpQuoteResponse
import com.tangem.spend.datasource.tangempay.models.response.BankOfframpTransferResponse
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BankOfframpQuoteSerializationTest {

    private val json = NetworkJsonFactory.create()

    @Nested
    inner class Request {

        @Test
        fun `GIVEN saved recipient WHEN quote serialized THEN amount sent as a number with the bank account id`() {
            // Arrange
            val request = BankOfframpQuoteRequest(
                productInstanceId = PRODUCT_INSTANCE_ID,
                amount = BigDecimal("100.50"),
                bankAccountId = BANK_ACCOUNT_ID,
            )

            // Act
            val actual = json.encodeToJsonElement(BankOfframpQuoteRequest.serializer(), request).jsonObject

            // Assert
            assertThat(actual).isEqualTo(
                parse(
                    """{"product_instance_id":"$PRODUCT_INSTANCE_ID","amount":100.50,""" +
                        """"bank_account_id":"$BANK_ACCOUNT_ID"}""",
                ),
            )
        }

        @Test
        fun `GIVEN new SPEI recipient WHEN quote serialized THEN no routing number, bank or addresses sent`() {
            // Arrange
            val request = BankOfframpQuoteRequest(
                productInstanceId = PRODUCT_INSTANCE_ID,
                amount = BigDecimal("100"),
                bankAccount = BankOfframpPlaceOrderRequest.BankAccount(
                    details = BankOfframpPlaceOrderRequest.Details(
                        currency = "MXN",
                        rail = "SPEI",
                        isThirdParty = false,
                        beneficiary = BankOfframpPlaceOrderRequest.Beneficiary(
                            type = "INDIVIDUAL",
                            firstName = "Maria",
                            lastName = "Lopez",
                        ),
                        accountNumber = CLABE,
                    ),
                ),
            )

            // Act
            val actual = json.encodeToJsonElement(BankOfframpQuoteRequest.serializer(), request).jsonObject

            // Assert
            assertThat(actual).isEqualTo(
                parse(
                    """{"product_instance_id":"$PRODUCT_INSTANCE_ID","amount":100,"bank_account":{"details":{""" +
                        """"currency":"MXN","rail":"SPEI","third_party":false,"beneficiary":{"type":"INDIVIDUAL",""" +
                        """"first_name":"Maria","last_name":"Lopez"},"account_number":"$CLABE"}}}""",
                ),
            )
        }

        @Test
        fun `GIVEN quote id WHEN transfer serialized THEN only the quote id sent`() {
            // Act
            val actual = json.encodeToJsonElement(
                BankOfframpTransferRequest.serializer(),
                BankOfframpTransferRequest(quoteId = QUOTE_ID),
            ).jsonObject

            // Assert
            assertThat(actual).isEqualTo(parse("""{"quote_id":"$QUOTE_ID"}"""))
        }

        private fun parse(raw: String): JsonObject = json.parseToJsonElement(raw).jsonObject
    }

    @Nested
    inner class Response {

        @Test
        fun `GIVEN quote WHEN parsed THEN every quote field read`() {
            // Arrange
            val raw = """{"result":{"quote_id":"$QUOTE_ID","bank_account_id":"$BANK_ACCOUNT_ID",""" +
                """"fee_amount":1.00,"source_amount":101.00,"destination_currency":"MXN",""" +
                """"destination_amount":1725.00,"exchange_rate":17.25,"expires_at":"$EXPIRES_AT",""" +
                """"fee":{"name":"SPEI","calculation_type":"PERCENTAGE","amount":1,"currency":"USD",""" +
                """"description":"SPEI offramp fee"},"eta":{"min_days":0,"max_days":0}},"error":null}"""

            // Act
            val actual = json.decodeFromString(BankOfframpQuoteResponse.serializer(), raw)

            // Assert
            assertThat(actual).isEqualTo(
                BankOfframpQuoteResponse(
                    result = BankOfframpQuoteResponse.Result(
                        quoteId = QUOTE_ID,
                        bankAccountId = BANK_ACCOUNT_ID,
                        feeAmount = BigDecimal("1.00"),
                        sourceAmount = BigDecimal("101.00"),
                        destinationCurrency = "MXN",
                        destinationAmount = BigDecimal("1725.00"),
                        exchangeRate = BigDecimal("17.25"),
                        expiresAt = EXPIRES_AT,
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
            )
        }

        @Test
        fun `GIVEN quote without a fee amount WHEN parsed THEN fee amount is null`() {
            // Arrange
            val raw = """{"result":{"quote_id":"$QUOTE_ID","fee_amount":null},"error":null}"""

            // Act
            val actual = json.decodeFromString(BankOfframpQuoteResponse.serializer(), raw)

            // Assert
            assertThat(actual).isEqualTo(
                BankOfframpQuoteResponse(
                    result = BankOfframpQuoteResponse.Result(
                        quoteId = QUOTE_ID,
                        bankAccountId = null,
                        feeAmount = null,
                        sourceAmount = null,
                        destinationCurrency = null,
                        destinationAmount = null,
                        exchangeRate = null,
                        expiresAt = null,
                        fee = null,
                        eta = null,
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN transfer WHEN parsed THEN every transfer field read`() {
            // Arrange
            val raw = """{"result":{"transfer_id":"$TRANSFER_ID","deposit_address":"$DEPOSIT_ADDRESS",""" +
                """"source_amount":101.00,"destination_amount":1725.00,"expires_at":"$EXPIRES_AT"},""" +
                """"error":null}"""

            // Act
            val actual = json.decodeFromString(BankOfframpTransferResponse.serializer(), raw)

            // Assert
            assertThat(actual).isEqualTo(
                BankOfframpTransferResponse(
                    result = BankOfframpTransferResponse.Result(
                        transferId = TRANSFER_ID,
                        depositAddress = DEPOSIT_ADDRESS,
                        sourceAmount = BigDecimal("101.00"),
                        destinationAmount = BigDecimal("1725.00"),
                        expiresAt = EXPIRES_AT,
                    ),
                ),
            )
        }
    }

    private companion object {
        const val PRODUCT_INSTANCE_ID = "123e4567-e89b-12d3-a456-426614174000"
        const val BANK_ACCOUNT_ID = "7b0f7d70-ec8f-4d4d-b3f1-41f52b6f4d10"
        const val QUOTE_ID = "0d1f3b5a-7c9e-4b2d-8f6a-1e3c5b7d9f0a"
        const val TRANSFER_ID = "9e8d7c6b-5a4f-4e3d-2c1b-0a9f8e7d6c5b"
        const val DEPOSIT_ADDRESS = "0x1234567890abcdef1234567890abcdef12345678"
        const val EXPIRES_AT = "2026-11-02T12:15:00Z"
        const val CLABE = "002010077777777771"
    }
}
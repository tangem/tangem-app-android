package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import org.junit.jupiter.api.Test

internal class BankOfframpOrderResponseParsingTest {

    private val json = NetworkJsonFactory.create()

    @Test
    fun `GIVEN order awaiting signature WHEN parsed THEN withdraw data read`() {
        // Arrange
        val raw = orderJson()

        // Act
        val result = json.decodeFromString(OrderResponse.Result.serializer(), raw)

        // Assert
        assertThat(result.data.bankAccountId).isEqualTo(BANK_ACCOUNT_ID)
        assertThat(result.data.withdrawData).isEqualTo(
            OrderResponse.Result.Data.WithdrawData(
                hash = HASH,
                salt = SALT,
                senderAddress = SENDER_ADDRESS,
                structuredData = STRUCTURED_DATA,
            ),
        )
    }

    @Test
    fun `GIVEN withdraw data with the hash only WHEN parsed THEN the order still parses`() {
        // Arrange
        val raw = """{"id":"order_1","status":"PROCESSING","step":"AWAITING_SIGNATURE","data":{""" +
            """"type":"BANK_OFFRAMP_RAIN","withdraw_data":{"hash":"$HASH",""" +
            """"structured_data":$STRUCTURED_DATA}}}"""

        // Act
        val result = json.decodeFromString(OrderResponse.Result.serializer(), raw)

        // Assert
        assertThat(result.data.withdrawData).isEqualTo(
            OrderResponse.Result.Data.WithdrawData(
                hash = HASH,
                salt = null,
                senderAddress = null,
                structuredData = STRUCTURED_DATA,
            ),
        )
    }

    @Test
    fun `GIVEN structured data WHEN parsed THEN kept as the raw json`() {
        // Arrange
        val raw = orderJson()

        // Act
        val structuredData = json.decodeFromString(OrderResponse.Result.serializer(), raw).data.withdrawData
            ?.structuredData

        // Assert
        assertThat(structuredData).isEqualTo(STRUCTURED_DATA)
    }

    @Test
    fun `GIVEN integer chain id in structured data WHEN parsed THEN not turned into a double`() {
        // Act
        val structuredData = json.decodeFromString(OrderResponse.Result.serializer(), orderJson()).data.withdrawData
            ?.structuredData

        // Assert
        assertThat(structuredData).contains(""""chainId":137""")
        assertThat(structuredData).doesNotContain("137.0")
    }

    private fun orderJson(): String =
        """{"id":"order_1","status":"PROCESSING","step":"AWAITING_SIGNATURE","data":{""" +
            """"type":"BANK_OFFRAMP_RAIN","bank_account_id":"$BANK_ACCOUNT_ID","withdraw_data":{""" +
            """"hash":"$HASH","salt":"$SALT","sender_address":"$SENDER_ADDRESS",""" +
            """"structured_data":$STRUCTURED_DATA}}}"""

    private companion object {
        const val BANK_ACCOUNT_ID = "7b0f7d70"
        const val HASH = "0x9d2a1b7c"
        const val SALT = "0x4f8e2c1d"
        const val SENDER_ADDRESS = "0xSENDER"

        val STRUCTURED_DATA = """{"types":{"EIP712Domain":[{"name":"name","type":"string"},""" +
            """{"name":"chainId","type":"uint256"}],"Withdraw":[{"name":"amount","type":"uint256"}]},""" +
            """"primaryType":"Withdraw","domain":{"name":"Rain","chainId":137},""" +
            """"message":{"amount":"10000"}}"""
    }
}
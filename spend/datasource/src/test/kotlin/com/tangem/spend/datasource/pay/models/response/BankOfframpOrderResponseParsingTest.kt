package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.spend.datasource.pay.addTangemPayEnumFallbackAdapters
import org.junit.jupiter.api.Test

internal class BankOfframpOrderResponseParsingTest {

    private val moshi = Moshi.Builder()
        .addTangemPayEnumFallbackAdapters()
        .add(RawJsonAdapter())
        .build()

    private val adapter = moshi.adapter(OrderResponse.Result::class.java)

    private val anyAdapter = moshi.adapter(Any::class.java)

    @Test
    fun `GIVEN order awaiting signature WHEN parsed THEN withdraw data read`() {
        // Arrange
        val json = orderJson()

        // Act
        val result = adapter.fromJson(json)!!

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
        val json = """{"id":"order_1","status":"PROCESSING","step":"AWAITING_SIGNATURE","data":{""" +
            """"type":"BANK_OFFRAMP_RAIN","withdraw_data":{"hash":"$HASH",""" +
            """"structured_data":$STRUCTURED_DATA}}}"""

        // Act
        val result = adapter.fromJson(json)!!

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
        val json = orderJson()

        // Act
        val structuredData = adapter.fromJson(json)!!.data.withdrawData?.structuredData

        // Assert
        assertThat(structuredData).isEqualTo(STRUCTURED_DATA)
    }

    @Test
    fun `GIVEN integer chain id in structured data WHEN parsed THEN not turned into a double`() {
        // Arrange
        val throughKotlinTypes = anyAdapter.toJson(anyAdapter.fromJson(STRUCTURED_DATA))

        // Act
        val structuredData = adapter.fromJson(orderJson())!!.data.withdrawData?.structuredData

        // Assert
        assertThat(throughKotlinTypes).contains("137.0")
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
package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import org.junit.jupiter.api.Test

internal class WithdrawDataResponseParsingTest {

    private val json = NetworkJsonFactory.create()

    @Test
    fun `GIVEN withdraw data with structured data WHEN parsed THEN structured data kept as the raw json`() {
        // Arrange
        val raw = """{"result":{"hash":"$HASH","salt":"$SALT","sender_address":"$SENDER_ADDRESS",""" +
            """"structured_data":$STRUCTURED_DATA}}"""

        // Act
        val actual = json.decodeFromString(WithdrawDataResponse.serializer(), raw)

        // Assert
        assertThat(actual).isEqualTo(
            WithdrawDataResponse(
                result = WithdrawDataResponse.Result(
                    hash = HASH,
                    salt = SALT,
                    senderAddress = SENDER_ADDRESS,
                    structuredData = STRUCTURED_DATA,
                ),
            ),
        )
    }

    @Test
    fun `GIVEN withdraw data without structured data WHEN parsed THEN structured data is null`() {
        // Arrange
        val raw = """{"result":{"hash":"$HASH","salt":"$SALT","sender_address":"$SENDER_ADDRESS"}}"""

        // Act
        val actual = json.decodeFromString(WithdrawDataResponse.serializer(), raw)

        // Assert
        assertThat(actual.result?.structuredData).isNull()
    }

    private companion object {
        const val HASH = "0x9d2a1b7c"
        const val SALT = "0x4f8e2c1d"
        const val SENDER_ADDRESS = "0x059dbd5f24a000d28f83268db1c85c9c3ab6a123"

        val STRUCTURED_DATA = """{"types":{"EIP712Domain":[{"name":"name","type":"string"},""" +
            """{"name":"chainId","type":"uint256"}],"Withdraw":[{"name":"amount","type":"uint256"}]},""" +
            """"primaryType":"Withdraw","domain":{"name":"Collateral","chainId":137},""" +
            """"message":{"amount":1500000}}"""
    }
}
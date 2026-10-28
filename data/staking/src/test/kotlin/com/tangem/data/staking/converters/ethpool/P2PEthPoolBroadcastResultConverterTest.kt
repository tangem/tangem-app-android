package com.tangem.data.staking.converters.ethpool

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.ethpool.P2PEthPoolBroadcastResult
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolBroadcastResponse
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class P2PEthPoolBroadcastResultConverterTest {

    @Test
    fun `GIVEN broadcast response WHEN convert THEN fields mapped and gas parsed`() {
        // Act
        val actual = P2PEthPoolBroadcastResultConverter.convert(broadcastResponse())

        // Assert
        val expected = P2PEthPoolBroadcastResult(
            hash = "0xhash",
            status = "success",
            blockNumber = 100,
            transactionIndex = 2,
            gasUsed = BigDecimal("21000"),
            cumulativeGasUsed = BigDecimal("42000"),
            effectiveGasPrice = BigDecimal("30000000000"),
            from = "0xfrom",
            to = "0xto",
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN pending broadcast without receipt WHEN convert THEN optional fields null`() {
        // Arrange
        val response = broadcastResponse(gasUsed = null).copy(
            blockNumber = null,
            transactionIndex = null,
            cumulativeGasUsed = null,
            effectiveGasPrice = null,
        )

        // Act
        val actual = P2PEthPoolBroadcastResultConverter.convert(response)

        // Assert
        assertThat(actual.blockNumber).isNull()
        assertThat(actual.transactionIndex).isNull()
        assertThat(actual.gasUsed).isNull()
        assertThat(actual.cumulativeGasUsed).isNull()
        assertThat(actual.effectiveGasPrice).isNull()
    }

    // Pins current behaviour: hex gas values are not parsed
    @Test
    fun `GIVEN hex gas used WHEN convert THEN gas used null`() {
        // Act
        val actual = P2PEthPoolBroadcastResultConverter.convert(broadcastResponse(gasUsed = "0x5208"))

        // Assert
        assertThat(actual.gasUsed).isNull()
    }

    private fun broadcastResponse(gasUsed: String? = "21000") = P2PEthPoolBroadcastResponse(
        hash = "0xhash",
        status = "success",
        blockNumber = 100,
        transactionIndex = 2,
        gasUsed = gasUsed,
        cumulativeGasUsed = "42000",
        effectiveGasPrice = "30000000000",
        from = "0xfrom",
        to = "0xto",
    )
}
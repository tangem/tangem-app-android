package com.tangem.data.staking.converters.ethpool

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.ethpool.P2PEthPoolUnsignedTx
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolUnsignedTxDTO
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class P2PEthPoolUnsignedTxConverterTest {

    @Test
    fun `GIVEN unsigned tx dto WHEN convert THEN fields mapped`() {
        // Act
        val actual = P2PEthPoolUnsignedTxConverter.convert(unsignedTxDTO())

        // Assert
        val expected = P2PEthPoolUnsignedTx(
            serializeTx = "0x02f8",
            to = "0xvault1",
            data = "0xdata",
            value = BigDecimal("1000000000000000000"),
            nonce = 7,
            chainId = 1,
            gasLimit = BigDecimal("21000"),
            maxFeePerGas = BigDecimal("30000000000"),
            maxPriorityFeePerGas = BigDecimal("1000000000"),
        )
        assertThat(actual).isEqualTo(expected)
    }

    private fun unsignedTxDTO() = P2PEthPoolUnsignedTxDTO(
        serializeTx = "0x02f8",
        to = "0xvault1",
        data = "0xdata",
        value = "1000000000000000000",
        nonce = 7,
        chainId = 1,
        gasLimit = BigDecimal("21000"),
        maxFeePerGas = BigDecimal("30000000000"),
        maxPriorityFeePerGas = BigDecimal("1000000000"),
    )
}
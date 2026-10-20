package com.tangem.data.staking.converters.ethpool

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.ethpool.P2PEthPoolUnsignedTx
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolUnsignedTxDTO
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Value {

        // Pins current behaviour: hex value silently becomes zero
        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN value string WHEN convert THEN parsed or zero`(model: ValueModel) {
            // Act
            val actual = P2PEthPoolUnsignedTxConverter.convert(unsignedTxDTO(value = model.value))

            // Assert
            assertThat(actual.value).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ValueModel("0x0de0b6b3a7640000", BigDecimal.ZERO),
        )
    }

    private fun unsignedTxDTO(value: String = "1000000000000000000") = P2PEthPoolUnsignedTxDTO(
        serializeTx = "0x02f8",
        to = "0xvault1",
        data = "0xdata",
        value = value,
        nonce = 7,
        chainId = 1,
        gasLimit = BigDecimal("21000"),
        maxFeePerGas = BigDecimal("30000000000"),
        maxPriorityFeePerGas = BigDecimal("1000000000"),
    )

    internal data class ValueModel(val value: String, val expected: BigDecimal)
}
package com.tangem.domain.staking.model.ethpool

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class P2PEthPoolNetworkTest {

    @Test
    fun `GIVEN mainnet chain id WHEN fromChainId THEN MAINNET`() {
        // Act & Assert
        assertThat(P2PEthPoolNetwork.fromChainId(1)).isEqualTo(P2PEthPoolNetwork.MAINNET)
    }

    // Pins current behaviour: value is hoodi but chainId 17000 is Holesky
    @Test
    fun `GIVEN holesky chain id WHEN fromChainId THEN TESTNET`() {
        // Act & Assert
        assertThat(P2PEthPoolNetwork.fromChainId(17000)).isEqualTo(P2PEthPoolNetwork.TESTNET)
    }

    // Pins current behaviour: value is hoodi but chainId 17000 is Holesky
    @Test
    fun `GIVEN real hoodi chain id WHEN fromChainId THEN null`() {
        // Act & Assert
        assertThat(P2PEthPoolNetwork.fromChainId(560048)).isNull()
    }

    @Test
    fun `GIVEN unsupported chain id WHEN isSupported THEN false`() {
        // Act & Assert
        assertThat(P2PEthPoolNetwork.isSupported(137)).isFalse()
    }

    @Test
    fun `GIVEN mainnet chain id WHEN isSupported THEN true`() {
        // Act & Assert
        assertThat(P2PEthPoolNetwork.isSupported(1)).isTrue()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FromValue {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN api value WHEN fromValue THEN matching network`(model: ValueModel) {
            // Act
            val actual = P2PEthPoolNetwork.fromValue(model.value)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            ValueModel("mainnet", P2PEthPoolNetwork.MAINNET),
            ValueModel("hoodi", P2PEthPoolNetwork.TESTNET),
            ValueModel("Mainnet", null),
            ValueModel("holesky", null),
            ValueModel("", null),
        )
    }

    internal data class ValueModel(val value: String, val expected: P2PEthPoolNetwork?)
}
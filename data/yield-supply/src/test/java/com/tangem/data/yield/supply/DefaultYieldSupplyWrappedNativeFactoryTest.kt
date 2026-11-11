package com.tangem.data.yield.supply

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultYieldSupplyWrappedNativeFactoryTest {

    private val currencyFactory = MockCryptoCurrencyFactory()
    private val factory = DefaultYieldSupplyWrappedNativeFactory(excludedBlockchains = ExcludedBlockchains())

    @Test
    fun `GIVEN Ethereum mainnet WHEN create THEN returns WETH9 on that network`() {
        // Arrange
        val network = currencyFactory.createCoin(Blockchain.Ethereum).network

        // Act
        val actual = factory.create(network)

        // Assert
        assertThat(actual).isNotNull()
        assertThat(actual!!.network).isEqualTo(network)
        assertThat(actual.contractAddress).isEqualTo("0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2")
        assertThat(actual.decimals).isEqualTo(18)
        assertThat(actual.symbol).isEqualTo("WETH")
        assertThat(actual.id.rawCurrencyId).isEqualTo(CryptoCurrency.RawID("weth"))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN network without known wrapped native WHEN create THEN returns null`(network: Network) {
        // Act
        val actual = factory.create(network)

        // Assert
        assertThat(actual).isNull()
    }

    private fun provideTestModels() = listOf(
        currencyFactory.createCoin(Blockchain.EthereumTestnet).network,
        currencyFactory.createCoin(Blockchain.BSC).network,
        currencyFactory.createCoin(Blockchain.Polygon).network,
    )
}
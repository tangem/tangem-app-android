package com.tangem.features.earn.lending.impl.provider.aave

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.yield.supply.YieldSupplyWrappedNativeFactory
import com.tangem.features.earn.lending.impl.provider.LendingAsset
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultAaveAssetResolverTest {

    private val currencies = MockCryptoCurrencyFactory()
    private val ethereum = currencies.createCoin(Blockchain.Ethereum)
    private val weth = currencies.createToken(
        blockchain = Blockchain.Ethereum,
        contractAddress = YieldSupplyWrappedNativeFactory.WETH_CONTRACT_ADDRESS,
        symbol = "WETH",
        decimals = 18,
    )

    private val wethFactory: YieldSupplyWrappedNativeFactory = mockk {
        every { create(any()) } answers { if (firstArg<Network>() == ethereum.network) weth else null }
    }

    private val resolver = DefaultAaveAssetResolver(wethFactory = wethFactory)

    @Test
    fun `GIVEN ETH coin on mainnet WHEN resolve THEN asset wraps it into WETH`() {
        // Act
        val actual = resolver.resolve(ethereum)

        // Assert
        assertThat(actual).isEqualTo(LendingAsset(currency = ethereum, wrappedToken = weth))
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN currency Lending does not cover WHEN resolve THEN null`(currency: CryptoCurrency) {
        // Act
        val actual = resolver.resolve(currency)

        // Assert
        assertThat(actual).isNull()
    }

    private fun provideTestModels(): List<CryptoCurrency> = listOf(
        currencies.createCoin(Blockchain.EthereumTestnet),
        currencies.createCoin(Blockchain.BSC),
        weth,
        jointEthereum(),
    )

    private fun jointEthereum(): CryptoCurrency.Coin {
        val jointPath = Network.DerivationPath.Joint("m/44'/60'/0'/0/0")
        val network = ethereum.network.copy(
            id = Network.ID(rawId = ethereum.network.id.rawId, derivationPath = jointPath),
            derivationPath = jointPath,
        )
        return ethereum.copy(network = network)
    }
}
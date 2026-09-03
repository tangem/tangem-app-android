package com.tangem.data.common.currency

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.currency.CryptoCurrency
import org.junit.jupiter.api.Test

internal class CryptoCurrencyFactoryDisplayDecimalsTest {

    private val userWallet = MockUserWalletFactory.create()
    private val excludedBlockchains = ExcludedBlockchains()
    private val factory = CryptoCurrencyFactory(excludedBlockchains = excludedBlockchains)
    private val responseFactory = ResponseCryptoCurrenciesFactory(
        networkFactory = NetworkFactory(excludedBlockchains = excludedBlockchains),
    )

    @Test
    fun `GIVEN arc WHEN create coin THEN chain decimals are 18 and display decimals are 6`() {
        // Act
        val coin = factory.createCoin(blockchain = Blockchain.Arc, extraDerivationPath = null, userWallet = userWallet)

        // Assert
        assertThat(coin?.decimals).isEqualTo(18)
        assertThat(coin?.displayDecimals).isEqualTo(6)
    }

    @Test
    fun `GIVEN ethereum WHEN create coin THEN display decimals equal chain decimals`() {
        // Act
        val coin = factory.createCoin(
            blockchain = Blockchain.Ethereum,
            extraDerivationPath = null,
            userWallet = userWallet,
        )

        // Assert
        assertThat(coin?.decimals).isEqualTo(18)
        assertThat(coin?.displayDecimals).isEqualTo(18)
    }

    @Test
    fun `GIVEN arc response coin WHEN create currency THEN display decimals are 6`() {
        // Arrange
        val arc = MockCryptoCurrencyFactory().createCoin(blockchain = Blockchain.Arc)
        val responseToken = UserTokensResponseFactory().createResponseToken(currency = arc)

        // Act
        val currency = responseFactory.createCurrency(
            responseToken = responseToken,
            userWallet = userWallet,
            network = arc.network,
        )

        // Assert
        assertThat(currency).isInstanceOf(CryptoCurrency.Coin::class.java)
        assertThat(currency?.decimals).isEqualTo(18)
        assertThat(currency?.displayDecimals).isEqualTo(6)
    }

    @Test
    fun `GIVEN arc response coin saved with 6 decimals WHEN create currency THEN chain decimals win`() {
        // Arrange
        val arc = MockCryptoCurrencyFactory().createCoin(blockchain = Blockchain.Arc)
        val responseToken = UserTokensResponseFactory().createResponseToken(currency = arc).copy(decimals = 6)

        // Act
        val currency = responseFactory.createCurrency(
            responseToken = responseToken,
            userWallet = userWallet,
            network = arc.network,
        )

        // Assert
        assertThat(currency?.decimals).isEqualTo(18)
        assertThat(currency?.displayDecimals).isEqualTo(6)
    }
}
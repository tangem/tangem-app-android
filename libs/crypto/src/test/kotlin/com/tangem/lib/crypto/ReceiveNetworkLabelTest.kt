package com.tangem.lib.crypto

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReceiveNetworkLabelTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN currency WHEN receiveNetworkLabel THEN returns the label exchanges show for its network`(
        model: LabelModel,
    ) {
        // Act
        val actual = model.currency.receiveNetworkLabel()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        LabelModel(createToken(Blockchain.Ethereum, Network.StandardType.ERC20), expected = "ERC20"),
        LabelModel(createToken(Blockchain.BSC, Network.StandardType.BEP20), expected = "BEP20"),
        LabelModel(createToken(Blockchain.Tron, Network.StandardType.TRC20), expected = "TRC20"),
        LabelModel(createToken(Blockchain.Binance, Network.StandardType.BEP2), expected = "BEP2"),
        LabelModel(createToken(Blockchain.Chiliz, Network.StandardType.CAP20), expected = "CAP20"),
        LabelModel(createCoin(Blockchain.Ethereum, Network.StandardType.ERC20), expected = null),
        LabelModel(createCoin(Blockchain.BSC, Network.StandardType.BEP20), expected = null),
        LabelModel(createCoin(Blockchain.Tron, Network.StandardType.TRC20), expected = null),
        LabelModel(createCoin(Blockchain.Chiliz, Network.StandardType.CAP20), expected = null),
        LabelModel(createToken(Blockchain.Optimism), expected = "Optimism"),
        LabelModel(createCoin(Blockchain.Optimism), expected = "Optimism"),
        LabelModel(createCoin(Blockchain.OptimismTestnet), expected = "Optimism"),
        LabelModel(createToken(Blockchain.Avalanche), expected = "AVAXC"),
        LabelModel(createCoin(Blockchain.Avalanche), expected = "AVAXC"),
        LabelModel(createCoin(Blockchain.AvalancheTestnet), expected = "AVAXC"),
        LabelModel(createToken(Blockchain.Polygon), expected = null),
        LabelModel(createToken(Blockchain.Arbitrum), expected = null),
        LabelModel(createToken(Blockchain.Solana), expected = null),
        LabelModel(createCoin(Blockchain.Bitcoin), expected = null),
        LabelModel(createCoin(Blockchain.TON), expected = null),
    )

    data class LabelModel(val currency: CryptoCurrency, val expected: String?)

    private fun createCoin(
        blockchain: Blockchain,
        standardType: Network.StandardType = Network.StandardType.Unspecified(blockchain.name),
    ): CryptoCurrency.Coin = CryptoCurrency.Coin(
        id = createId(blockchain, CryptoCurrency.ID.Prefix.COIN_PREFIX),
        network = createNetwork(blockchain, standardType),
        name = blockchain.fullName,
        symbol = blockchain.currency,
        decimals = 8,
        iconUrl = null,
        isCustom = false,
    )

    private fun createToken(
        blockchain: Blockchain,
        standardType: Network.StandardType = Network.StandardType.Unspecified(blockchain.name),
    ): CryptoCurrency.Token = CryptoCurrency.Token(
        id = createId(blockchain, CryptoCurrency.ID.Prefix.TOKEN_PREFIX),
        network = createNetwork(blockchain, standardType),
        name = "Tether",
        symbol = "USDT",
        decimals = 6,
        iconUrl = null,
        isCustom = false,
        contractAddress = "0xdac17f958d2ee523a2206206994597c13d831ec7",
    )

    private fun createId(blockchain: Blockchain, prefix: CryptoCurrency.ID.Prefix) = CryptoCurrency.ID(
        prefix = prefix,
        body = CryptoCurrency.ID.Body.NetworkId(blockchain.toNetworkId()),
        suffix = CryptoCurrency.ID.Suffix.RawID(blockchain.id),
    )

    private fun createNetwork(blockchain: Blockchain, standardType: Network.StandardType) = Network(
        id = Network.ID(value = blockchain.toNetworkId(), derivationPath = Network.DerivationPath.None),
        name = blockchain.fullName,
        currencySymbol = blockchain.currency,
        derivationPath = Network.DerivationPath.None,
        isTestnet = false,
        standardType = standardType,
        hasFiatFeeRate = true,
        canHandleTokens = true,
        transactionExtrasType = Network.TransactionExtrasType.NONE,
        nameResolvingType = Network.NameResolvingType.NONE,
    )
}
package com.tangem.common.test.domain.token

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.FeePaidCurrency
import com.tangem.blockchain.common.Token
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.domain.wallets.derivations.DerivationStyleProvider
import com.tangem.domain.wallets.derivations.derivationStyleProvider
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet

/**
[REDACTED_AUTHOR]
 */
class MockCryptoCurrencyFactory(private val userWallet: UserWallet.Cold = defaultUserWallet) {

    private val factory = CryptoCurrencyFactory(excludedBlockchains = ExcludedBlockchains())

    val cardano by lazy { createCoin(blockchain = Blockchain.Cardano) }
    val chia by lazy { createCoin(Blockchain.Chia) }
    val ethereum by lazy { createCoin(Blockchain.Ethereum) }
    val stellar by lazy { createCoin(Blockchain.Stellar) }

    val chiaAndEthereum by lazy {
        listOf(
            createCoin(blockchain = Blockchain.Chia),
            createCoin(blockchain = Blockchain.Ethereum),
        )
    }

    val ethereumAndStellar by lazy {
        listOf(
            createCoin(blockchain = Blockchain.Ethereum),
            createCoin(blockchain = Blockchain.Stellar),
        )
    }

    val ethereumTokenWithBinanceDerivation by lazy {
        listOf(
            createCustomToken(blockchain = Blockchain.Ethereum, derivationBlockchain = Blockchain.Binance),
        )
    }

    fun createCoin(blockchain: Blockchain): CryptoCurrency.Coin {
        val derivationPath = createDerivationPath(
            blockchain = blockchain,
            extraDerivationPath = null,
            cardDerivationStyleProvider = userWallet.scanResponse.derivationStyleProvider,
        )

        val network = Network(
            id = Network.ID(blockchain.id, derivationPath),
            backendId = blockchain.toNetworkId(),
            name = blockchain.fullName,
            isTestnet = blockchain.isTestnet(),
            derivationPath = derivationPath,
            currencySymbol = blockchain.currency,
            standardType = getNetworkStandardType(blockchain),
            hasFiatFeeRate = blockchain.feePaidCurrency() !is FeePaidCurrency.FeeResource,
            canHandleTokens = false,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = when (blockchain) {
                Blockchain.Ethereum, Blockchain.EthereumTestnet -> Network.NameResolvingType.ENS
                else -> Network.NameResolvingType.NONE
            },
        )

        return factory.createCoin(network = network)
    }

    // Impossible to create custom token by CryptoCurrencyFactory because it works with URI under the hood
    fun createCustomToken(blockchain: Blockchain, derivationBlockchain: Blockchain): CryptoCurrency {
        val derivationPath = Network.DerivationPath.Custom(
            value = derivationBlockchain.derivationPath(
                userWallet.scanResponse.derivationStyleProvider.getDerivationStyle(),
            )!!.rawPath,
        )

        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(blockchain.id),
                suffix = CryptoCurrency.ID.Suffix.RawID(blockchain.id),
            ),
            network = Network(
                id = Network.ID(value = blockchain.id, derivationPath),
                backendId = "NEVER-MIND",
                name = blockchain.fullName,
                currencySymbol = "NEVER-MIND",
                derivationPath = derivationPath,
                isTestnet = false,
                standardType = Network.StandardType.ERC20,
                hasFiatFeeRate = true,
                canHandleTokens = true,
                transactionExtrasType = Network.TransactionExtrasType.NONE,
                nameResolvingType = Network.NameResolvingType.NONE,
            ),
            name = "NEVER-MIND",
            symbol = "NEVER-MIND",
            decimals = 8,
            iconUrl = null,
            isCustom = false,
            contractAddress = "NEVER-MIND",
        )
    }

    fun createToken(blockchain: Blockchain): CryptoCurrency {
        return factory.createToken(
            sdkToken = Token(
                name = "NEVER-MIND",
                symbol = "NEVER-MIND",
                contractAddress = "NEVER-MIND",
                decimals = 8,
                id = "NEVER-MIND",
            ),
            blockchain = blockchain,
            extraDerivationPath = null,
            userWallet = userWallet,
        )!!
    }

    private fun createDerivationPath(
        blockchain: Blockchain,
        extraDerivationPath: String?,
        cardDerivationStyleProvider: DerivationStyleProvider?,
    ): Network.DerivationPath {
        if (cardDerivationStyleProvider == null) return Network.DerivationPath.None

        val defaultDerivationPath = getDefaultDerivationPath(blockchain, cardDerivationStyleProvider)

        return if (extraDerivationPath.isNullOrBlank()) {
            if (defaultDerivationPath.isNullOrBlank()) {
                Network.DerivationPath.None
            } else {
                Network.DerivationPath.Card(defaultDerivationPath)
            }
        } else {
            if (extraDerivationPath == defaultDerivationPath) {
                Network.DerivationPath.Card(defaultDerivationPath)
            } else {
                Network.DerivationPath.Custom(extraDerivationPath)
            }
        }
    }

    private fun getDefaultDerivationPath(
        blockchain: Blockchain,
        derivationStyleProvider: DerivationStyleProvider,
    ): String? {
        return blockchain.derivationPath(derivationStyleProvider.getDerivationStyle())?.rawPath
    }

    private fun getNetworkStandardType(blockchain: Blockchain): Network.StandardType {
        return when (blockchain) {
            Blockchain.Ethereum, Blockchain.EthereumTestnet -> Network.StandardType.ERC20
            Blockchain.BSC, Blockchain.BSCTestnet -> Network.StandardType.BEP20
            Blockchain.Binance, Blockchain.BinanceTestnet -> Network.StandardType.BEP2
            Blockchain.Tron, Blockchain.TronTestnet -> Network.StandardType.TRC20
            else -> Network.StandardType.Unspecified(blockchain.name)
        }
    }

    private companion object {

        val defaultUserWallet = MockUserWalletFactory.create(
            scanResponse = MockScanResponseFactory.create(
                cardConfig = GenericCardConfig(2),
                derivedKeys = emptyMap(),
            ),
        )
    }
}
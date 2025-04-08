package com.tangem.common.test.domain.token

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.FeePaidCurrency
import com.tangem.blockchain.common.Token
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.data.common.currency.CryptoCurrencyFactory
import com.tangem.data.common.currency.getNetworkDerivationPath
import com.tangem.data.common.currency.getNetworkStandardType
import com.tangem.domain.common.configs.GenericCardConfig
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network

/**
[REDACTED_AUTHOR]
 */
class MockCryptoCurrencyFactory(private val scanResponse: ScanResponse = defaultScanResponse) {

    private val factory = CryptoCurrencyFactory(excludedBlockchains = ExcludedBlockchains())

    val cardano by lazy { createCoin(blockchain = Blockchain.Cardano) }
    val chia by lazy { createCoin(Blockchain.Chia) }
    val ethereum by lazy { createCoin(Blockchain.Ethereum) }

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

    fun createCoin(blockchain: Blockchain): CryptoCurrency {
        val network = Network(
            id = Network.ID(blockchain.id),
            backendId = blockchain.toNetworkId(),
            name = blockchain.fullName,
            isTestnet = blockchain.isTestnet(),
            derivationPath = getNetworkDerivationPath(
                blockchain = blockchain,
                extraDerivationPath = null,
                cardDerivationStyleProvider = scanResponse.derivationStyleProvider,
            ),
            currencySymbol = blockchain.currency,
            standardType = getNetworkStandardType(blockchain),
            hasFiatFeeRate = blockchain.feePaidCurrency() !is FeePaidCurrency.FeeResource,
            canHandleTokens = false,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
        )

        return factory.createCoin(network = network)
    }

    // Impossible to create custom token by CryptoCurrencyFactory because it works with URI under the hood
    fun createCustomToken(blockchain: Blockchain, derivationBlockchain: Blockchain): CryptoCurrency {
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(blockchain.id),
                suffix = CryptoCurrency.ID.Suffix.RawID(blockchain.id),
            ),
            network = Network(
                id = Network.ID(value = blockchain.id),
                backendId = "NEVER-MIND",
                name = blockchain.fullName,
                currencySymbol = "NEVER-MIND",
                derivationPath = Network.DerivationPath.Custom(
                    value = derivationBlockchain.derivationPath(
                        scanResponse.derivationStyleProvider.getDerivationStyle(),
                    )!!.rawPath,
                ),
                isTestnet = false,
                standardType = Network.StandardType.ERC20,
                hasFiatFeeRate = true,
                canHandleTokens = true,
                transactionExtrasType = Network.TransactionExtrasType.NONE,
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
            sdkToken = Token(symbol = "NEVER-MIND", contractAddress = "NEVER-MIND", decimals = 8),
            blockchain = blockchain,
            extraDerivationPath = null,
            scanResponse = scanResponse,
        )!!
    }

    private companion object {

        val defaultScanResponse = MockScanResponseFactory.create(
            cardConfig = GenericCardConfig(2),
            derivedKeys = emptyMap(),
        )
    }
}
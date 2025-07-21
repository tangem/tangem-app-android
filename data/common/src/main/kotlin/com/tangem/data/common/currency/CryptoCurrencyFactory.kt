package com.tangem.data.common.currency

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.data.common.network.NetworkFactory
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWallet
import timber.log.Timber
import com.tangem.blockchain.common.Token as SdkToken

// FIXME: Make internal
class CryptoCurrencyFactory(
    private val excludedBlockchains: ExcludedBlockchains,
) {

    private val networkFactory by lazy(LazyThreadSafetyMode.NONE) { NetworkFactory(excludedBlockchains) }

    @Suppress("LongParameterList") // Yep, it's long
    fun createToken(
        network: Network,
        rawId: CryptoCurrency.RawID?,
        name: String,
        symbol: String,
        decimals: Int,
        contractAddress: String,
    ): CryptoCurrency.Token {
        val id = getTokenId(network, rawId, contractAddress)

        return CryptoCurrency.Token(
            id = id,
            network = network,
            name = name,
            symbol = symbol,
            decimals = decimals,
            iconUrl = rawId?.let(::getTokenIconUrlFromDefaultHost),
            isCustom = isCustomToken(id, network),
            contractAddress = contractAddress,
        )
    }

    fun createToken(
        sdkToken: SdkToken,
        blockchain: Blockchain,
        extraDerivationPath: String?,
        userWallet: UserWallet,
    ): CryptoCurrency.Token? {
        if (blockchain == Blockchain.Unknown) {
            Timber.e("Unable to map the SDK token to the domain token with Unknown blockchain")
            return null
        }

        val network = networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = extraDerivationPath,
            userWallet = userWallet,
        ) ?: return null

        val id = getTokenId(network, sdkToken)

        return CryptoCurrency.Token(
            id = id,
            network = network,
            name = sdkToken.name,
            symbol = sdkToken.symbol,
            iconUrl = getTokenIconUrl(blockchain, sdkToken),
            decimals = sdkToken.decimals,
            isCustom = isCustomToken(id, network),
            contractAddress = sdkToken.contractAddress,
        )
    }

    fun createCoin(
        blockchain: Blockchain,
        extraDerivationPath: String?,
        userWallet: UserWallet,
    ): CryptoCurrency.Coin? {
        if (blockchain == Blockchain.Unknown) {
            Timber.e("Unable to map the SDK token to the domain token with Unknown blockchain")
            return null
        }

        val network = networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = extraDerivationPath,
            userWallet = userWallet,
        ) ?: return null

        return createCoin(network)
    }

    fun createCoin(networkId: String, extraDerivationPath: String?, userWallet: UserWallet): CryptoCurrency.Coin? {
        val blockchain = Blockchain.fromNetworkId(networkId) ?: Blockchain.Unknown
        return createCoin(blockchain, extraDerivationPath, userWallet)
    }

    fun createCoin(network: Network): CryptoCurrency.Coin {
        val blockchain = network.toBlockchain()

        return CryptoCurrency.Coin(
            id = getCoinId(network, blockchain.toCoinId()),
            network = network,
            name = blockchain.getCoinName(),
            symbol = blockchain.currency,
            iconUrl = getCoinIconUrl(blockchain),
            decimals = blockchain.decimals(),
            isCustom = isCustomCoin(network),
        )
    }

    fun createToken(
        token: Token,
        networkId: String,
        extraDerivationPath: String?,
        userWallet: UserWallet,
    ): CryptoCurrency.Token? {
        val sdkToken = SdkToken(
            name = token.name,
            symbol = token.symbol,
            contractAddress = token.contractAddress,
            decimals = token.decimals,
            id = token.id,
        )
        val blockchain = Blockchain.fromNetworkId(networkId) ?: Blockchain.Unknown
        return createToken(
            sdkToken = sdkToken,
            blockchain = blockchain,
            extraDerivationPath = extraDerivationPath,
            userWallet = userWallet,
        )
    }

    fun createToken(cryptoCurrency: CryptoCurrency.Token, network: Network): CryptoCurrency.Token {
        val sdkToken = SdkToken(
            name = cryptoCurrency.name,
            symbol = cryptoCurrency.symbol,
            contractAddress = cryptoCurrency.contractAddress,
            decimals = cryptoCurrency.decimals,
            id = cryptoCurrency.id.rawCurrencyId?.value,
        )
        val blockchain =
            Blockchain.fromNetworkId(cryptoCurrency.network.backendId) ?: Blockchain.Unknown
        val id = getTokenId(network, sdkToken)
        return CryptoCurrency.Token(
            id = id,
            network = network,
            name = sdkToken.name,
            symbol = sdkToken.symbol,
            iconUrl = getTokenIconUrl(blockchain, sdkToken),
            decimals = sdkToken.decimals,
            isCustom = isCustomToken(id, network),
            contractAddress = sdkToken.contractAddress,
        )
    }

    data class Token(
        val name: String,
        val symbol: String,
        val contractAddress: String,
        val decimals: Int,
        val id: String? = null,
    )
}
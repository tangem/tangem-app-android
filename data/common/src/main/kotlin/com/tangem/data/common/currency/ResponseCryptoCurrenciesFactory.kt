package com.tangem.data.common.currency

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import timber.log.Timber
import javax.inject.Inject
import com.tangem.blockchain.common.Token as SdkToken

class ResponseCryptoCurrenciesFactory @Inject constructor(
    private val networkFactory: NetworkFactory,
) {

    fun createCurrency(currencyId: String, response: UserTokensResponse, userWallet: UserWallet): CryptoCurrency {
        return response.tokens
            .asSequence()
            .mapNotNull { createCurrency(it, userWallet) }
            .first { it.id.value == currencyId }
    }

    fun createCurrencies(response: UserTokensResponse, userWallet: UserWallet): List<CryptoCurrency> {
        return createCurrencies(tokens = response.tokens, userWallet = userWallet)
    }

    fun createCurrencies(tokens: List<UserTokensResponse.Token>, userWallet: UserWallet): List<CryptoCurrency> {
        return tokens
            .asSequence()
            .mapNotNull { createCurrency(it, userWallet) }
            .distinctBy(CryptoCurrency::id)
            .toList()
    }

    fun createCurrency(responseToken: UserTokensResponse.Token, userWallet: UserWallet): CryptoCurrency? {
        var blockchain = Blockchain.fromNetworkId(responseToken.networkId)
        if (blockchain == null || blockchain == Blockchain.Unknown) {
            Timber.e("Unable to find a blockchain with the network ID: ${responseToken.networkId}")
            return null
        }

        if (userWallet is UserWallet.Cold && userWallet.scanResponse.cardTypesResolver.isTestCard()) {
            blockchain = blockchain.getTestnetVersion() ?: blockchain
        }

        val sdkToken = createSdkToken(responseToken)
        return if (sdkToken == null) {
            createCoin(blockchain, responseToken, userWallet)
        } else {
            createToken(blockchain, sdkToken, responseToken.derivationPath, userWallet)
        }
    }

    private fun createSdkToken(token: UserTokensResponse.Token): SdkToken? {
        return token.contractAddress?.let { contractAddress ->
            SdkToken(
                name = token.name,
                symbol = token.symbol,
                contractAddress = contractAddress,
                decimals = token.decimals,
                id = token.id,
            )
        }
    }

    private fun createCoin(
        blockchain: Blockchain,
        responseToken: UserTokensResponse.Token,
        userWallet: UserWallet,
    ): CryptoCurrency.Coin? {
        val network = networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = responseToken.derivationPath,
            userWallet = userWallet,
        ) ?: return null

        return CryptoCurrency.Coin(
            id = getCoinId(network, blockchain.toCoinId()),
            network = network,
            name = blockchain.getCoinName(),
            symbol = blockchain.getSymbolForCoin(responseToken),
            decimals = responseToken.decimals,
            iconUrl = getCoinIconUrl(blockchain),
            isCustom = isCustomCoin(network),
        )
    }

    private fun Blockchain.getSymbolForCoin(responseToken: UserTokensResponse.Token): String {
        return when (this) {
            // workaround: Dischain was renamed but backend still returns the old name,
            // get name and symbol from enum Blockchain until backend renamed
            // [REDACTED_JIRA]
            Blockchain.Dischain,
            Blockchain.Polygon,
            -> this.currency
            else -> responseToken.symbol
        }
    }

    private fun createToken(
        blockchain: Blockchain,
        sdkToken: Token,
        responseDerivationPath: String?,
        userWallet: UserWallet,
    ): CryptoCurrency.Token? {
        val network = networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = responseDerivationPath,
            userWallet = userWallet,
        ) ?: return null

        val id = getTokenId(network, sdkToken)

        return CryptoCurrency.Token(
            id = id,
            network = network,
            name = sdkToken.name,
            symbol = sdkToken.symbol,
            decimals = sdkToken.decimals,
            iconUrl = getTokenIconUrl(blockchain, sdkToken),
            contractAddress = sdkToken.contractAddress,
            isCustom = isCustomToken(id, network),
        )
    }
}
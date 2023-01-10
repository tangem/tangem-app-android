package com.tangem.tap.features.wallet.models

import com.tangem.blockchain.common.DerivationStyle
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import com.tangem.blockchain.common.Blockchain as SdkBlockchain
import com.tangem.blockchain.common.Token as SdkToken

sealed interface Currency {
    val coinId: String?
        get() = when (this) {
            is Blockchain -> blockchain.toCoinId()
            is Token -> token.id
        }
    val blockchain: SdkBlockchain
    val currencySymbol: CryptoCurrencyName
    val derivationPath: String?
    val currencyName: String
        get() = when (this) {
            is Blockchain -> blockchain.fullName
            is Token -> token.name
        }
    val decimals
        get() = when (this) {
            is Blockchain -> blockchain.decimals()
            is Token -> token.decimals
        }

    data class Token(
        val token: SdkToken,
        override val blockchain: SdkBlockchain,
        override val derivationPath: String?,
    ) : Currency {
        override val currencySymbol = token.symbol
    }

    data class Blockchain(
        override val blockchain: SdkBlockchain,
        override val derivationPath: String?,
    ) : Currency {
        override val currencySymbol: CryptoCurrencyName = blockchain.currency
    }

    fun isCustomCurrency(derivationStyle: DerivationStyle?): Boolean {
        if (this is Token && this.token.id == null) return true

        if (derivationPath == null || derivationStyle == null) return false

        return derivationPath != blockchain.derivationPath(derivationStyle)?.rawPath
    }

    fun isBlockchain(): Boolean = this is Blockchain
    fun isToken(): Boolean = this is Token

    companion object {
        fun fromBlockchainNetwork(
            blockchainNetwork: BlockchainNetwork,
            token: SdkToken? = null,
        ): Currency {
            return if (token != null) {
                Token(
                    token = token,
                    blockchain = blockchainNetwork.blockchain,
                    derivationPath = blockchainNetwork.derivationPath,
                )
            } else {
                Blockchain(
                    blockchain = blockchainNetwork.blockchain,
                    derivationPath = blockchainNetwork.derivationPath,
                )
            }
        }

        fun fromCustomCurrency(customCurrency: CustomCurrency): Currency {
            return when (customCurrency) {
                is CustomCurrency.CustomBlockchain -> Blockchain(
                    blockchain = customCurrency.network,
                    derivationPath = customCurrency.derivationPath?.rawPath,
                )
                is CustomCurrency.CustomToken -> Token(
                    token = customCurrency.token,
                    blockchain = customCurrency.network,
                    derivationPath = customCurrency.derivationPath?.rawPath,
                )
            }
        }

        fun fromTokenWithBlockchain(tokenWithBlockchain: TokenWithBlockchain): Token {
            return Token(
                token = tokenWithBlockchain.token,
                blockchain = tokenWithBlockchain.blockchain,
                derivationPath = null,
            )
        }

        fun fromTokenResponse(tokenBody: UserTokensResponse.Token): Currency? {
            val blockchain = com.tangem.blockchain.common.Blockchain.fromNetworkId(tokenBody.networkId)
                ?: return null
            return when {
                tokenBody.contractAddress != null -> Token(
                    token = SdkToken(
                        name = tokenBody.name,
                        symbol = tokenBody.symbol,
                        contractAddress = tokenBody.contractAddress!!,
                        decimals = tokenBody.decimals,
                        id = tokenBody.id,
                    ),
                    blockchain = blockchain,
                    derivationPath = tokenBody.derivationPath,
                )
                else -> Blockchain(
                    blockchain = blockchain,
                    derivationPath = tokenBody.derivationPath,
                )
            }
        }
    }
}

fun BlockchainNetwork.toCurrencies(): List<Currency> {
    val blockchain = Currency.fromBlockchainNetwork(this)
    val tokens = this.tokens.map { Currency.fromBlockchainNetwork(this, it) }
    return listOf(blockchain) + tokens
}

fun List<BlockchainNetwork>.toCurrencies(): List<Currency> {
    return flatMap { it.toCurrencies() }
}

fun List<Currency>.toBlockchainNetworks(): List<BlockchainNetwork> {
    return this.filter { it.isBlockchain() }.map { BlockchainNetwork(it.blockchain, it.derivationPath, getTokens(it)) }
}

fun List<Currency>.getTokens(currency: Currency): List<SdkToken> {
    return this
        .filter { it.isToken() && it.blockchain == currency.blockchain && it.derivationPath == currency.derivationPath }
        .mapNotNull { if (it is Currency.Token) it.token else null }
}

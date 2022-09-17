package com.tangem.tap.features.wallet.models

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.blockchain.common.Token
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.network.api.tangemTech.TokenResponse
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain

sealed interface Currency {
    val coinId: String?
        get() = when (this) {
            is Blockchain -> blockchain.toCoinId()
            is Token -> token.id
        }
    val blockchain: com.tangem.blockchain.common.Blockchain
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
        val token: com.tangem.blockchain.common.Token,
        override val blockchain: com.tangem.blockchain.common.Blockchain,
        override val derivationPath: String?,
    ) : Currency {
        override val currencySymbol = token.symbol
    }

    data class Blockchain(
        override val blockchain: com.tangem.blockchain.common.Blockchain,
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
    fun toTokenResponse(): TokenResponse {
        return TokenResponse(
            id = coinId ?: "",
            networkId = blockchain.toNetworkId(),
            derivationPath = derivationPath ?: DERIVATION_PATH_RAW_VALUE,
            name = currencyName,
            symbol = currencySymbol,
            decimals = decimals,
            contractAddress = if (this is Token) token.contractAddress else null,
        )
    }

    companion object {
        private const val DERIVATION_PATH_RAW_VALUE = "m/44/0'/0/0"
        fun fromBlockchainNetwork(
            blockchainNetwork: BlockchainNetwork,
            token: com.tangem.blockchain.common.Token? = null,
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

        fun fromTokenResponse(tokenResponse: TokenResponse): Currency {
            val derivationPath = if (tokenResponse.derivationPath == DERIVATION_PATH_RAW_VALUE) {
                null
            } else {
                tokenResponse.derivationPath
            }
            return when {
                tokenResponse.contractAddress != null -> Token(
                    com.tangem.blockchain.common.Token(
                        name = tokenResponse.name,
                        symbol = tokenResponse.symbol,
                        contractAddress = tokenResponse.contractAddress!!,
                        decimals = tokenResponse.decimals,
                        id = tokenResponse.id,
                    ),
                    blockchain = com.tangem.blockchain.common.Blockchain.fromNetworkId(tokenResponse.networkId)!!,
                    derivationPath = derivationPath,
                )
                else -> Blockchain(
                    blockchain = com.tangem.blockchain.common.Blockchain.fromNetworkId(tokenResponse.networkId)!!,
                    derivationPath = derivationPath,
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

private fun List<Currency>.getTokens(currency: Currency): List<Token> {
    return this
        .filter { it.isToken() && it.blockchain == currency.blockchain && it.derivationPath == currency.derivationPath }
        .mapNotNull { if (it is Currency.Token) it.token else null }
}


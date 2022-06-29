package com.tangem.tap.features.wallet.models

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.blockchain.common.Token
import com.tangem.domain.common.extensions.toCoinId
import com.tangem.domain.features.addCustomToken.CustomCurrency
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

    data class Token(
        val token: com.tangem.blockchain.common.Token,
        override val blockchain: com.tangem.blockchain.common.Blockchain,
        override val derivationPath: String?
    ) : Currency {
        override val currencySymbol = token.symbol
    }

    data class Blockchain(
        override val blockchain: com.tangem.blockchain.common.Blockchain,
        override val derivationPath: String?
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
            token: com.tangem.blockchain.common.Token? = null
        ): Currency {
            return if (token != null) {
                Token(
                    token = token,
                    blockchain = blockchainNetwork.blockchain,
                    derivationPath = blockchainNetwork.derivationPath
                )
            } else {
                Blockchain(
                    blockchain = blockchainNetwork.blockchain,
                    derivationPath = blockchainNetwork.derivationPath
                )
            }
        }

        fun fromCustomCurrency(customCurrency: CustomCurrency): Currency {
            return when (customCurrency) {
                is CustomCurrency.CustomBlockchain -> Blockchain(
                    blockchain = customCurrency.network,
                    derivationPath = customCurrency.derivationPath?.rawPath
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
                derivationPath = null
            )
        }
    }
}
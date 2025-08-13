package com.tangem.tap.domain.model

import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.blockchain.common.Blockchain as SdkBlockchain
import com.tangem.blockchain.common.Token as SdkToken

sealed interface Currency {
    val blockchain: SdkBlockchain
    val currencySymbol: CryptoCurrencyName
    val derivationPath: String?
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

    companion object {
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
    }
}
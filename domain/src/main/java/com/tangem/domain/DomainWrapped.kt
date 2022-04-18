package com.tangem.domain

import com.tangem.blockchain.common.DerivationStyle

/**
* [REDACTED_AUTHOR]
 * Provides a temporary copies of the app module classes, data structures, etc.
 */
// [REDACTED_TODO_COMMENT]
// to appropriate parts of module
sealed interface DomainWrapped {

    // Mirror reflection ot the com.tangem.tap.features.wallet.redux.Currency
    sealed interface Currency {
        val blockchain: com.tangem.blockchain.common.Blockchain
        val currencySymbol: String
        val derivationPath: String?

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
            override val currencySymbol: String = blockchain.currency
        }

        fun isCustomCurrency(derivationStyle: DerivationStyle?): Boolean {
            if (derivationPath == null || derivationStyle == null) return false
            return derivationPath != blockchain.derivationPath(derivationStyle)?.rawPath
        }
    }
}
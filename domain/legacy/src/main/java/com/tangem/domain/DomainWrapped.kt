package com.tangem.domain

/**
[REDACTED_AUTHOR]
 * Provides a temporary copies of the app module classes, data structures, etc.
 */
// TODO: refactoring: : after refactoring they should be unwrapped and moved
// to appropriate parts of module
@Deprecated("After refactoring they should be unwrapped and moved to appropriate parts of module")
sealed interface DomainWrapped {

    // Mirror reflection ot the com.tangem.tap.features.wallet.redux.Currency
    sealed interface Currency {
        val blockchain: com.tangem.blockchain.common.Blockchain
        val currencySymbol: String
        val derivationPath: String?

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
            override val currencySymbol: String = blockchain.currency
        }
    }
}
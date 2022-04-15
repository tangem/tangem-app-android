package com.tangem.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token

/**
[REDACTED_AUTHOR]
 * Provides a temporary copies of the app module classes, data structures, etc.
 */
//TODO: refactoring: : after refactoring they should be unwrapped and moved
// to appropriate parts of module
sealed interface DomainWrapped {

    data class TokenWithBlockchain(
        val token: Token,
        val blockchain: Blockchain
    )
}
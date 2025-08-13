package com.tangem.domain.models.account

import com.tangem.domain.models.tokenlist.TokenList
import kotlinx.serialization.Serializable

/**
 * Represents the status of an account
 *
[REDACTED_AUTHOR]
 */
@Serializable
sealed interface AccountStatus {

    /** The account associated with this status */
    val account: Account

    /**
     * Represents the status of a crypto portfolio account
     *
     * @property account   the crypto portfolio account
     * @property tokenList the list of tokens associated with the account
     */
    @Serializable
    data class CryptoPortfolio(
        override val account: Account.CryptoPortfolio,
        val tokenList: TokenList,
    ) : AccountStatus
}
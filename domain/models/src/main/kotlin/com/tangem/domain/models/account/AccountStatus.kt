package com.tangem.domain.models.account

import com.tangem.domain.core.lce.Lce
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.quote.PriceChange
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

    /** Unique identifier of the account */
    val accountId: AccountId
        get() = account.accountId

    @Serializable
    sealed interface Crypto : AccountStatus {

        override val account: Account.Crypto

        /**
         * Represents the status of a crypto portfolio account
         *
         * @property account        the crypto portfolio account
         * @property tokenList      the list of tokens associated with the account
         * @property priceChangeLce the price change information wrapped in a Lce (Loading, Content, Error) state
         */
        @Serializable
        data class Portfolio(
            override val account: Account.Crypto.Portfolio,
            val tokenList: TokenList,
            val priceChangeLce: Lce<Unit, PriceChange>,
        ) : Crypto

        fun flattenCurrencies(): List<CryptoCurrencyStatus> = when (this) {
            is Portfolio -> tokenList.flattenCurrencies()
        }

        fun getCryptoTokenList(): TokenList = when (this) {
            is Portfolio -> tokenList
        }
    }

    @Serializable
    data class Payment(
        override val account: Account.Payment,
        val totalFiatBalance: TotalFiatBalance,
    ) : AccountStatus
}
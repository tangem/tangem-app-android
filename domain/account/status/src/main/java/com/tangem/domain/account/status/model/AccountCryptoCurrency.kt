package com.tangem.domain.account.status.model

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import kotlinx.serialization.Serializable

typealias AccountCryptoCurrencies = Map<Account, List<CryptoCurrency>>

/**
 * Combines an [Account] with its corresponding [CryptoCurrency].
 *
 * This data class is useful for representing the relationship between an account and the cryptocurrency it holds.
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class AccountCryptoCurrency(val account: Account.CryptoPortfolio, val cryptoCurrency: CryptoCurrency) {

    init {
        require(account.cryptoCurrencies.contains(cryptoCurrency))
    }
}
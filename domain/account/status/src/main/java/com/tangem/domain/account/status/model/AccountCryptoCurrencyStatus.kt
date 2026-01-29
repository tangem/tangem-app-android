package com.tangem.domain.account.status.model

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import kotlinx.serialization.Serializable

typealias AccountCryptoCurrencyStatuses = Map<Account.Crypto, List<CryptoCurrencyStatus>>

/**
 * Combines an [Account] with its corresponding [CryptoCurrencyStatus].
 *
 * This data class is useful for representing the relationship between an account and the cryptocurrency it holds.
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class AccountCryptoCurrencyStatus(val account: Account.Crypto, val status: CryptoCurrencyStatus) {

    init {
        require(account.cryptoCurrencies.contains(status.currency))
    }
}
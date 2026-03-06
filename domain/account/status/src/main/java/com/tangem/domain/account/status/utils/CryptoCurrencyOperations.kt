package com.tangem.domain.account.status.utils

import arrow.core.Option
import arrow.core.toOption
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.utils.AccountCryptoCurrencyOperations.getAccountCryptoCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network

/**
 * Extension functions for retrieving [CryptoCurrency] from an [AccountList] or [Account.CryptoPortfolio].
 *
 * This object provides convenient extension functions to search and retrieve [CryptoCurrency] instances
 * from account-related data structures using various parameters such as cryptocurrency ID and network.
 *
 * Unlike [AccountCryptoCurrencyOperations], this object returns only the [CryptoCurrency] without
 * the associated account information.
 *
 * Usage example:
 * ```
 * val accountList: AccountList? = ...
 * val result: Option<CryptoCurrency> = accountList.getCryptoCurrency(currencyId, network)
 * result.fold(
 *     ifEmpty = { /* handle not found */ },
 *     ifSome = { currency -> /* use the found currency */ }
 * )
 * ```
 *
 * @see CryptoCurrency
 * @see AccountList
 * @see Account.CryptoPortfolio
 * @see AccountCryptoCurrencyOperations
[REDACTED_AUTHOR]
 */
object CryptoCurrencyOperations {

    // region AccountList

    /**
     * Retrieves the [CryptoCurrency] matching the specified [cryptoCurrency] from this [AccountList].
     *
     * @receiver the [AccountList] to search within, can be null
     * @param cryptoCurrency the cryptocurrency to match
     * @return [Option] containing the [CryptoCurrency] if found, or [Option.None] otherwise
     */
    fun AccountList?.getCryptoCurrency(cryptoCurrency: CryptoCurrency): Option<CryptoCurrency> {
        return getCryptoCurrency(currencyId = cryptoCurrency.id, network = cryptoCurrency.network)
    }

    /**
     * Retrieves the [CryptoCurrency] for the specified [currencyId] and [network] from this [AccountList].
     *
     * @receiver the [AccountList] to search within, can be null
     * @param currencyId the ID of the cryptocurrency to be retrieved
     * @param network the network associated with the cryptocurrency, can be null
     * @return [Option] containing the [CryptoCurrency] if found, or [Option.None] otherwise
     */
    fun AccountList?.getCryptoCurrency(currencyId: CryptoCurrency.ID, network: Network?): Option<CryptoCurrency> {
        return getAccountCryptoCurrency(currencyId, network)
            .map { it.cryptoCurrency }
    }
    // endregion

    // region Account.CryptoPortfolio

    /**
     * Retrieves the [CryptoCurrency] matching the specified [currencyId] from this [Account.CryptoPortfolio].
     *
     * @receiver the [Account.CryptoPortfolio] to search within
     * @param currencyId the ID of the cryptocurrency to be retrieved
     * @return [Option] containing the [CryptoCurrency] if found, or [Option.None] otherwise
     */
    fun Account.CryptoPortfolio.getCryptoCurrency(currencyId: CryptoCurrency.ID): Option<CryptoCurrency> {
        return cryptoCurrencies.firstOrNull { it.id == currencyId }.toOption()
    }
    // endregion
}
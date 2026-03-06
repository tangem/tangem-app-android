package com.tangem.domain.account.status.utils

import arrow.core.Option
import arrow.core.raise.option
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.model.AccountCryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network

/**
 * Extension functions for retrieving [AccountCryptoCurrency] from an [AccountList].
 *
 * This object provides convenient extension functions to search and retrieve [AccountCryptoCurrency] instances
 * from an [AccountList] using various parameters such as cryptocurrency ID and network.
 *
 * Usage example:
 * ```
 * val accountList: AccountList? = ...
 * val result: Option<AccountCryptoCurrency> = accountList.getAccountCryptoCurrency(currency)
 * result.fold(
 *     ifEmpty = { /* handle not found */ },
 *     ifSome = { accountCryptoCurrency -> /* use the found result */ }
 * )
 * ```
 *
 * @see AccountCryptoCurrency
 * @see AccountList
[REDACTED_AUTHOR]
 */
object AccountCryptoCurrencyOperations {

    // region AccountList

    /**
     * Retrieves the [AccountCryptoCurrency] for the specified [currency] from this [AccountList].
     *
     * @receiver the [AccountList] to search within, can be null
     * @param currency the cryptocurrency whose account association is to be retrieved
     * @return [Option] containing the [AccountCryptoCurrency] if found, or [Option.None] otherwise
     */
    fun AccountList?.getAccountCryptoCurrency(currency: CryptoCurrency): Option<AccountCryptoCurrency> {
        return getAccountCryptoCurrency(currencyId = currency.id, network = currency.network)
    }

    /**
     * Retrieves the [AccountCryptoCurrency] for the specified [currencyId] and [network] from this [AccountList].
     *
     * @receiver the [AccountList] to search within, can be null
     * @param currencyId the ID of the cryptocurrency whose account association is to be retrieved
     * @param network the network associated with the cryptocurrency, can be null
     * @return [Option] containing the [AccountCryptoCurrency] if found, or [Option.None] otherwise
     */
    fun AccountList?.getAccountCryptoCurrency(
        currencyId: CryptoCurrency.ID,
        network: Network?,
    ): Option<AccountCryptoCurrency> = option {
        val accountList = this@getAccountCryptoCurrency

        ensureNotNull(accountList)

        val accountCryptoCurrency = AccountCryptoCurrencyStatusFinder(
            accountList = accountList,
            currencyId = currencyId,
            network = network,
        )

        ensureNotNull(accountCryptoCurrency)
    }
    // endregion
}
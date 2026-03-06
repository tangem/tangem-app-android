package com.tangem.domain.account.status.utils

import arrow.core.Option
import arrow.core.raise.option
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network

/**
 * Extension functions for retrieving [AccountCryptoCurrencyStatus] from an [AccountStatusList].
 *
 * This object provides convenient extension functions to search and retrieve [AccountCryptoCurrencyStatus]
 * instances from an [AccountStatusList] using various parameters such as cryptocurrency ID and network.
 *
 * The primary difference from [AccountCryptoCurrencyOperations] is that this object works with
 * [AccountStatusList] and returns status information along with the account-currency association.
 *
 * Usage example:
 * ```
 * val accountStatusList: AccountStatusList? = ...
 * val result: Option<AccountCryptoCurrencyStatus> = accountStatusList.getAccountCryptoCurrencyStatus(currency)
 * result.fold(
 *     ifEmpty = { /* handle not found */ },
 *     ifSome = { accountCurrencyStatus -> /* use the found result */ }
 * )
 * ```
 *
 * @see AccountCryptoCurrencyStatus
 * @see AccountStatusList
 * @see AccountCryptoCurrencyOperations
[REDACTED_AUTHOR]
 */
object AccountCryptoCurrencyStatusOperations {

    // region AccountStatusList

    /**
     * Retrieves the [AccountCryptoCurrencyStatus] for the specified [currency] from this [AccountStatusList].
     *
     * @receiver the [AccountStatusList] to search within, can be null
     * @param currency the cryptocurrency whose account status association is to be retrieved
     * @return [Option] containing the [AccountCryptoCurrencyStatus] if found, or [Option.None] otherwise
     */
    fun AccountStatusList?.getAccountCryptoCurrencyStatus(
        currency: CryptoCurrency,
    ): Option<AccountCryptoCurrencyStatus> {
        return getAccountCryptoCurrencyStatus(currencyId = currency.id, network = currency.network)
    }

    /**
     * Retrieves the [AccountCryptoCurrencyStatus] for the specified [currencyId] and [network]
     * from this [AccountStatusList].
     *
     * @receiver the [AccountStatusList] to search within, can be null
     * @param currencyId the ID of the cryptocurrency whose account status association is to be retrieved
     * @param network the network associated with the cryptocurrency, can be null
     * @return [Option] containing the [AccountCryptoCurrencyStatus] if found, or [Option.None] otherwise
     */
    fun AccountStatusList?.getAccountCryptoCurrencyStatus(
        currencyId: CryptoCurrency.ID,
        network: Network?,
    ): Option<AccountCryptoCurrencyStatus> = option {
        val accountStatusList = this@getAccountCryptoCurrencyStatus

        ensureNotNull(accountStatusList)

        val accountCryptoCurrencyStatus = AccountCryptoCurrencyStatusFinder(
            accountStatusList = accountStatusList,
            currencyId = currencyId,
            network = network,
        )

        ensureNotNull(accountCryptoCurrencyStatus)
    }
    // endregion
}
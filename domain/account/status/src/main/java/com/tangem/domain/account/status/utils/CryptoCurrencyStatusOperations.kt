package com.tangem.domain.account.status.utils

import arrow.core.Option
import arrow.core.toOption
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.utils.AccountCryptoCurrencyStatusFinder.getExpectedAccountStatuses
import com.tangem.domain.account.status.utils.AccountCryptoCurrencyStatusOperations.getAccountCryptoCurrencyStatus
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network

/**
 * Extension functions for retrieving [CryptoCurrencyStatus] from an [AccountStatusList] or
 * [AccountStatus.CryptoPortfolio].
 *
 * This object provides convenient extension functions to search and retrieve [CryptoCurrencyStatus]
 * instances from account status-related data structures using various parameters such as cryptocurrency ID
 * and network.
 *
 * Unlike [AccountCryptoCurrencyStatusOperations], this object returns only the [CryptoCurrencyStatus] without
 * the associated account information.
 *
 * Usage example:
 * ```
 * val accountStatusList: AccountStatusList? = ...
 * val result: Option<CryptoCurrencyStatus> = accountStatusList.getCryptoCurrencyStatus(currency)
 * result.fold(
 *     ifEmpty = { /* handle not found */ },
 *     ifSome = { status -> /* use the found status */ }
 * )
 * ```
 *
 * @see CryptoCurrencyStatus
 * @see AccountStatusList
 * @see AccountStatus.CryptoPortfolio
 * @see AccountCryptoCurrencyStatusOperations
[REDACTED_AUTHOR]
 */
object CryptoCurrencyStatusOperations {

    // region AccountStatusList

    /**
     * Retrieves the [CryptoCurrencyStatus] for the specified [currency] from this [AccountStatusList].
     *
     * @receiver the [AccountStatusList] to search within, can be null
     * @param currency the cryptocurrency whose status is to be retrieved
     * @return [Option] containing the [CryptoCurrencyStatus] if found, or [Option.None] otherwise
     */
    fun AccountStatusList?.getCryptoCurrencyStatus(currency: CryptoCurrency): Option<CryptoCurrencyStatus> {
        return getCryptoCurrencyStatus(currencyId = currency.id, network = currency.network)
    }

    /**
     * Retrieves the [CryptoCurrencyStatus] for the specified [currencyId] and [network]
     * from this [AccountStatusList].
     *
     * @receiver the [AccountStatusList] to search within, can be null
     * @param currencyId the ID of the cryptocurrency whose status is to be retrieved
     * @param network the network associated with the cryptocurrency, can be null
     * @return [Option] containing the [CryptoCurrencyStatus] if found, or [Option.None] otherwise
     */
    fun AccountStatusList?.getCryptoCurrencyStatus(
        currencyId: CryptoCurrency.ID,
        network: Network?,
    ): Option<CryptoCurrencyStatus> {
        return getAccountCryptoCurrencyStatus(currencyId = currencyId, network = network)
            .map(AccountCryptoCurrencyStatus::status)
    }

    fun AccountStatusList.getCoinStatus(currency: CryptoCurrency): Option<CryptoCurrencyStatus> {
        return getCoinStatus(network = currency.network)
    }

    fun AccountStatusList.getCoinStatus(network: Network): Option<CryptoCurrencyStatus> {
        return getCoinStatus(networkId = network.id)
    }

    fun AccountStatusList.getCoinStatus(networkId: Network.ID): Option<CryptoCurrencyStatus> {
        return getExpectedAccountStatuses(networkId)
            .flatMap { accountStatus ->
                (accountStatus as? AccountStatus.CryptoPortfolio)
                    ?.flattenCurrencies().orEmpty()
                    .filter { it.currency is CryptoCurrency.Coin }
            }
            .firstOrNull { status -> status.currency.network.id == networkId }
            .toOption()
    }
    // endregion

    // region AccountStatus.CryptoPortfolio

    /**
     * Retrieves the [CryptoCurrencyStatus] for the specified [currencyId]
     * from this [AccountStatus.CryptoPortfolio].
     *
     * @receiver the [AccountStatus.CryptoPortfolio] to search within
     * @param currencyId the ID of the cryptocurrency whose status is to be retrieved
     * @return [Option] containing the [CryptoCurrencyStatus] if found, or [Option.None] otherwise
     */
    fun AccountStatus.CryptoPortfolio.getCryptoCurrencyStatus(
        currencyId: CryptoCurrency.ID,
    ): Option<CryptoCurrencyStatus> {
        return flattenCurrencies().firstOrNull { it.currency.id == currencyId }.toOption()
    }
    // endregion
}
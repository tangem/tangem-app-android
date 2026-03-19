package com.tangem.domain.account.status.utils

import arrow.core.None
import arrow.core.Option
import arrow.core.raise.catch
import arrow.core.raise.option
import arrow.core.toOption
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.utils.AccountCryptoCurrencyOperations.getAccountCryptoCurrency
import com.tangem.domain.account.status.utils.AccountCryptoCurrencyStatusFinder.getExpectedAccounts
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import timber.log.Timber

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

    fun AccountList?.getCryptoCurrency(currencyIdValue: String): Option<CryptoCurrency> = option {
        val currencyId = catch(
            block = { CryptoCurrency.ID.fromValue(currencyIdValue) },
            catch = { throwable ->
                Timber.e("Error on converting currencyId: $throwable")
                raise(None)
            },
        )

        return getCryptoCurrency(currencyId = currencyId, network = null)
    }

    fun AccountList.getCoin(currency: CryptoCurrency): Option<CryptoCurrency.Coin> {
        return getCoin(network = currency.network)
    }

    /**
     * Retrieves all tokens that belong to the same network as the specified [coin].
     *
     * @receiver the [AccountList] to search within
     * @param coin the coin whose network will be used to find tokens
     * @return list of [CryptoCurrency.Token] in the same network, or empty list if none found
     */
    fun AccountList.getTokens(coin: CryptoCurrency.Coin): List<CryptoCurrency.Token> {
        return getExpectedAccounts(network = coin.network)
            .flatMap { account ->
                (account as? Account.CryptoPortfolio)
                    ?.cryptoCurrencies.orEmpty()
                    .filterIsInstance<CryptoCurrency.Token>()
            }
            .filter { token -> token.network.id == coin.network.id }
    }

    fun AccountList.getCoin(network: Network): Option<CryptoCurrency.Coin> {
        return getExpectedAccounts(network = network)
            .flatMap { account ->
                (account as? Account.CryptoPortfolio)
                    ?.cryptoCurrencies.orEmpty()
                    .filterIsInstance<CryptoCurrency.Coin>()
            }
            .firstOrNull { currency -> currency.network.id == network.id }
            .toOption()
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
package com.tangem.domain.account.status.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrency
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatuses
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.lib.crypto.derivation.AccountNodeRecognizer

/**
 * Finds a specific cryptocurrency or its status associated with an account from the provided account list.
 *
 * The base implementation uses [AccountList] for searching. For [AccountStatusList], it first converts
 * to [AccountList], finds the [AccountCryptoCurrency], then retrieves the status using accountId and currencyId.
 *
[REDACTED_AUTHOR]
 */
@Suppress("MethodOverloading")
internal object AccountCryptoCurrencyStatusFinder {

    // region AccountCryptoCurrencyStatus methods

    /**
     * Retrieves the [AccountCryptoCurrencyStatus] for the specified [currency] from the given [accountStatusList].
     *
     * @param accountStatusList the list of account statuses to search within.
     * @param currency the cryptocurrency whose status is to be retrieved.
     * @return the [AccountCryptoCurrencyStatus] if found, otherwise null.
     */
    operator fun invoke(accountStatusList: AccountStatusList, currency: CryptoCurrency): AccountCryptoCurrencyStatus? {
        return invoke(
            accountStatusList = accountStatusList,
            currencyId = currency.id,
            network = currency.network,
        )
    }

    /**
     * Retrieves the [AccountCryptoCurrencyStatus] for the specified [currencyId] and [network]
     * from the given [accountStatusList].
     *
     * @param accountStatusList the list of account statuses to search within.
     * @param currencyId the ID of the cryptocurrency whose status is to be retrieved.
     * @param network the network associated with the cryptocurrency.
     * @return the [AccountCryptoCurrencyStatus] if found, otherwise null.
     */
    operator fun invoke(
        accountStatusList: AccountStatusList,
        currencyId: CryptoCurrency.ID,
        network: Network?,
    ): AccountCryptoCurrencyStatus? {
        val accountList = accountStatusList.toAccountList().getOrNull() ?: return null
        val accountCurrency = invoke(accountList, currencyId, network) ?: return null

        return accountStatusList.findStatus(accountCurrency)
    }

    /**
     * Retrieves a map of accounts to their corresponding list of [CryptoCurrencyStatus] for the specified
     * list of [currencies] from the given [accountStatusList].
     *
     * @param accountStatusList the list of account statuses to search within.
     * @param currencies the list of cryptocurrencies whose statuses are to be retrieved.
     * @return a map where the key is the account and the value is a list of [CryptoCurrencyStatus].
     */
    operator fun invoke(
        accountStatusList: AccountStatusList,
        currencies: List<CryptoCurrency>,
    ): AccountCryptoCurrencyStatuses {
        if (currencies.isEmpty()) return emptyMap()

        val currencyIds = currencies.map(CryptoCurrency::id).toSet()
        val networks = currencies.map(CryptoCurrency::network).distinct()

        return accountStatusList.getExpectedAccountStatuses(networks)
            .asSequence()
            .filterCryptoPortfolio()
            .associate { accountStatus ->
                val statuses = accountStatus.flattenCurrencies()
                    .filter { it.currency.id in currencyIds }

                accountStatus.account to statuses
            }
    }

    /**
     * Retrieves the [AccountCryptoCurrencyStatus] for the specified [networkId], [derivationPath],
     * and optional [contractAddress] from the given [accountStatusList].
     *
     * @param accountStatusList the list of account statuses to search within.
     * @param networkId the ID of the network associated with the cryptocurrency.
     * @param derivationPath the derivation path of the account.
     * @param contractAddress the optional contract address of the token (if applicable).
     * @return the [AccountCryptoCurrencyStatus] if found, otherwise null.
     */
    operator fun invoke(
        accountStatusList: AccountStatusList,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        contractAddress: String?,
    ): AccountCryptoCurrencyStatus? {
        val accountList = accountStatusList.toAccountList().getOrNull() ?: return null
        val accountCurrency = invoke(
            accountList = accountList,
            networkId = networkId,
            derivationPath = derivationPath,
            contractAddress = contractAddress,
        ) ?: return null

        return accountStatusList.findStatus(accountCurrency)
    }

    // endregion

    // region AccountCryptoCurrency methods

    /**
     * Retrieves the [AccountCryptoCurrency] for the specified [currency] from the given [accountList].
     *
     * @param accountList the list of accounts to search within.
     * @param currency the cryptocurrency to be retrieved.
     * @return the [AccountCryptoCurrency] if found, otherwise null.
     */
    operator fun invoke(accountList: AccountList, currency: CryptoCurrency): AccountCryptoCurrency? {
        return invoke(
            accountList = accountList,
            currencyId = currency.id,
            network = currency.network,
        )
    }

    /**
     * Retrieves the [AccountCryptoCurrency] for the specified [currencyId] and [network]
     * from the given [accountList].
     *
     * @param accountList the list of accounts to search within.
     * @param currencyId the ID of the cryptocurrency to be retrieved.
     * @param network the network associated with the cryptocurrency.
     * @return the [AccountCryptoCurrency] if found, otherwise null.
     */
    operator fun invoke(
        accountList: AccountList,
        currencyId: CryptoCurrency.ID,
        network: Network?,
    ): AccountCryptoCurrency? {
        return accountList.getExpectedAccounts(network)
            .asSequence()
            .filterIsInstance<Account.CryptoPortfolio>()
            .mapNotNull { account ->
                val currency = account.cryptoCurrencies.firstOrNull { it.id == currencyId }
                    ?: return@mapNotNull null

                AccountCryptoCurrency(account = account, cryptoCurrency = currency)
            }
            .firstOrNull()
    }

    /**
     * Retrieves the [AccountCryptoCurrency] for the specified [networkId], [derivationPath],
     * and optional [contractAddress] from the given [accountList].
     *
     * @param accountList the list of accounts to search within.
     * @param networkId the ID of the network associated with the cryptocurrency.
     * @param derivationPath the derivation path of the account.
     * @param contractAddress the optional contract address of the token (if applicable).
     * @return the [AccountCryptoCurrency] if found, otherwise null.
     */
    operator fun invoke(
        accountList: AccountList,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        contractAddress: String?,
    ): AccountCryptoCurrency? {
        return accountList.getExpectedAccounts(
            rawNetworkId = networkId.rawId.value,
            derivationPath = derivationPath,
        )
            .asSequence()
            .filterIsInstance<Account.CryptoPortfolio>()
            .mapNotNull { account ->
                val currency = account.cryptoCurrencies.firstOrNull { currency ->
                    val isContractAddressMatch = contractAddress == null ||
                        currency.id.contractAddress.equals(contractAddress, ignoreCase = true)

                    currency.network.rawId == networkId.rawId.value &&
                        currency.network.derivationPath.value == derivationPath.value &&
                        isContractAddressMatch
                }
                    ?: return@mapNotNull null

                AccountCryptoCurrency(account = account, cryptoCurrency = currency)
            }
            .firstOrNull()
    }

    // endregion

    // region AccountStatusList helpers

    private fun AccountStatusList.findStatus(accountCurrency: AccountCryptoCurrency): AccountCryptoCurrencyStatus? {
        val accountStatus = accountStatuses
            .filterCryptoPortfolio()
            .firstOrNull { it.account.accountId == accountCurrency.account.accountId }
            ?: return null

        val currencyStatus = accountStatus.flattenCurrencies()
            .firstOrNull { it.currency.id == accountCurrency.cryptoCurrency.id }
            ?: return null

        return AccountCryptoCurrencyStatus(account = accountCurrency.account, status = currencyStatus)
    }

    internal fun AccountStatusList.getExpectedAccountStatuses(networkId: Network.ID): List<AccountStatus> {
        val possibleAccountIndex = getAccountIndexOrNull(
            rawNetworkId = networkId.rawId.value,
            derivationPath = networkId.derivationPath,
        )

        return when (possibleAccountIndex) {
            null -> accountStatuses
            DerivationIndex.Main.value -> listOf(mainAccount)
            // currency only in the account with specific derivation index or in the main account
            else -> {
                val account = accountStatuses.firstOrNull { account ->
                    val cryptoPortfolio = account as? AccountStatus.CryptoPortfolio ?: return@firstOrNull false
                    cryptoPortfolio.account.derivationIndex.value == possibleAccountIndex
                }
                listOfNotNull(account, mainAccount)
            }
        }
    }

    internal fun AccountStatusList.getExpectedAccountStatuses(networks: List<Network>): List<AccountStatus> {
        val possibleAccountIndexes = networks.mapNotNull { getAccountIndexOrNull(it.rawId, it.derivationPath) }

        if (possibleAccountIndexes.isEmpty()) return accountStatuses

        val filteredStatuses = accountStatuses.filter { accountStatus ->
            val cryptoPortfolio = accountStatus.account as? Account.CryptoPortfolio ?: return@filter false
            cryptoPortfolio.derivationIndex.value in possibleAccountIndexes
        }

        return filteredStatuses + listOf(mainAccount)
    }

    // endregion

    // region AccountList helpers

    internal fun AccountList.getExpectedAccounts(network: Network?): List<Account> {
        return getExpectedAccounts(rawNetworkId = network?.rawId, derivationPath = network?.derivationPath)
    }

    private fun AccountList.getExpectedAccounts(
        rawNetworkId: String?,
        derivationPath: Network.DerivationPath?,
    ): List<Account> {
        val possibleAccountIndex = getAccountIndexOrNull(rawNetworkId, derivationPath)

        return when (possibleAccountIndex) {
            null -> accounts
            DerivationIndex.Main.value -> listOf(mainAccount)
            // currency only in the account with specific derivation index or in the main account
            else -> {
                val account = accounts.firstOrNull { account ->
                    val cryptoPortfolio = account as? Account.CryptoPortfolio ?: return@firstOrNull false
                    cryptoPortfolio.derivationIndex.value == possibleAccountIndex
                }
                listOfNotNull(account, mainAccount)
            }
        }
    }

    // endregion

    // region Common helpers

    private fun getAccountIndexOrNull(rawNetworkId: String?, derivationPath: Network.DerivationPath?): Int? {
        if (rawNetworkId == null || derivationPath == null) return null

        val blockchain = Blockchain.fromId(id = rawNetworkId)
        val recognizer = AccountNodeRecognizer(blockchain)

        return recognizer.recognize(derivationPath)?.toInt()
    }

    // endregion
}
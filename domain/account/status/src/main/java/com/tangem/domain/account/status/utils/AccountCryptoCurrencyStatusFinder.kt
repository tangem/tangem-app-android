package com.tangem.domain.account.status.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatuses
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.lib.crypto.derivation.AccountNodeRecognizer

/**
 * Finds the status of a specific cryptocurrency associated with an account from the provided account status list.
 *
[REDACTED_AUTHOR]
 */
internal object AccountCryptoCurrencyStatusFinder {

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
        return accountStatusList.getExpectedAccountStatuses(network)
            .asSequence()
            .filterIsInstance<AccountStatus.Crypto.Portfolio>()
            .mapNotNull { accountStatus ->
                val status = accountStatus.flattenCurrencies().firstOrNull { it.currency.id == currencyId }
                    ?: return@mapNotNull null

                AccountCryptoCurrencyStatus(account = accountStatus.account, status = status)
            }
            .firstOrNull()
    }

    /**
     * Retrieves a map of accounts to their corresponding list of [AccountCryptoCurrencyStatus] for the specified
     * list of [currencies] from the given [accountStatusList].
     *
     * @param accountStatusList the list of account statuses to search within.
     * @param currencies the list of cryptocurrencies whose statuses are to be retrieved.
     * @return a map where the key is the account and the value is a list of [AccountCryptoCurrencyStatus].
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
            .filterIsInstance<AccountStatus.Crypto.Portfolio>()
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
        return accountStatusList.getExpectedAccountStatuses(
            rawNetworkId = networkId.rawId.value,
            derivationPath = derivationPath,
        )
            .asSequence()
            .filterIsInstance<AccountStatus.Crypto.Portfolio>()
            .mapNotNull { accountStatus ->
                val status = accountStatus.flattenCurrencies().firstOrNull {
                    val currency = it.currency
                    val isContractAddressMatch = contractAddress == null ||
                        currency.id.contractAddress.equals(contractAddress, ignoreCase = true)

                    currency.network.rawId == networkId.rawId.value &&
                        currency.network.derivationPath.value == derivationPath.value &&
                        isContractAddressMatch
                }
                    ?: return@mapNotNull null

                AccountCryptoCurrencyStatus(account = accountStatus.account, status = status)
            }
            .firstOrNull()
    }

    private fun AccountStatusList.getExpectedAccountStatuses(network: Network?): List<AccountStatus> {
        return getExpectedAccountStatuses(rawNetworkId = network?.rawId, derivationPath = network?.derivationPath)
    }

    /**
     * Retrieves the expected account statuses based on the provided [rawNetworkId] and [derivationPath].
     * If either parameter is null, all account statuses are returned.
     * If both parameters are provided, it filters the accounts based on the derivation index.
     *
     * @param rawNetworkId the raw ID of the network to filter accounts by, can be null.
     * @param derivationPath the derivation path of the network to filter accounts by, can be null.
     * @return a list of [AccountStatus] that match the expected criteria.
     */
    private fun AccountStatusList.getExpectedAccountStatuses(
        rawNetworkId: String?,
        derivationPath: Network.DerivationPath?,
    ): List<AccountStatus> {
        val possibleAccountIndex = if (rawNetworkId != null && derivationPath != null) {
            getAccountIndexOrNull(rawNetworkId, derivationPath)
        } else {
            null
        }

        return when (possibleAccountIndex) {
            // currency can be in any account
            null -> accountStatuses
            // currency only in the main account
            DerivationIndex.Main.value -> listOf(mainAccount)
            // currency only in the account with specific derivation index or in the main account
            else -> {
                val accountStatus = accountStatuses.firstOrNull { accountStatus ->
                    val cryptoPortfolio = accountStatus.account as? Account.Crypto.Portfolio ?: return@firstOrNull false

                    cryptoPortfolio.derivationIndex.value == possibleAccountIndex
                }

                listOfNotNull(accountStatus, mainAccount)
            }
        }
    }

    private fun AccountStatusList.getExpectedAccountStatuses(networks: List<Network>): List<AccountStatus> {
        val possibleAccountIndexes = networks.mapNotNull { it.getAccountIndexOrNull() }

        if (possibleAccountIndexes.isEmpty()) return this@getExpectedAccountStatuses.accountStatuses

        val accountStatuses = this@getExpectedAccountStatuses.accountStatuses.filter { accountStatus ->
            val cryptoPortfolio = accountStatus.account as? Account.Crypto.Portfolio ?: return@filter false

            cryptoPortfolio.derivationIndex.value in possibleAccountIndexes
        }

        return accountStatuses + listOf(mainAccount)
    }

    private fun Network.getAccountIndexOrNull(): Int? {
        return getAccountIndexOrNull(rawNetworkId = rawId, derivationPath = derivationPath)
    }

    private fun getAccountIndexOrNull(rawNetworkId: String, derivationPath: Network.DerivationPath): Int? {
        val blockchain = Blockchain.fromId(id = rawNetworkId)
        val recognizer = AccountNodeRecognizer(blockchain)

        return recognizer.recognize(derivationPath)?.toInt()
    }
}
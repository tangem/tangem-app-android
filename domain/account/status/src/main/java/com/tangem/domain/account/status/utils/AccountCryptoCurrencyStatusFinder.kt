package com.tangem.domain.account.status.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
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

    operator fun invoke(
        accountStatusList: AccountStatusList,
        currencyId: CryptoCurrency.ID,
        network: Network?,
    ): AccountCryptoCurrencyStatus? {
        return accountStatusList.getExpectedAccountStatuses(network)
            .asSequence()
            .filterIsInstance<AccountStatus.CryptoPortfolio>()
            .mapNotNull { accountStatus ->
                val status = accountStatus.flattenCurrencies().firstOrNull { it.currency.id == currencyId }
                    ?: return@mapNotNull null

                AccountCryptoCurrencyStatus(account = accountStatus.account, status = status)
            }
            .firstOrNull()
    }

    operator fun invoke(
        accountStatusList: AccountStatusList,
        currencies: List<CryptoCurrency>,
    ): AccountCryptoCurrencyStatuses {
        if (currencies.isEmpty()) return emptyMap()

        val currencyIds = currencies.map(CryptoCurrency::id).toSet()
        val networks = currencies.map(CryptoCurrency::network).distinct()

        return accountStatusList.getExpectedAccountStatuses(networks)
            .asSequence()
            .filterIsInstance<AccountStatus.CryptoPortfolio>()
            .associate { accountStatus ->
                val statuses = accountStatus.flattenCurrencies()
                    .filter { it.currency.id in currencyIds }

                accountStatus.account to statuses
            }
    }

    /**
     * Retrieves the expected account statuses based on the provided [network].
     * If the [network] is null, all account statuses are returned.
     * If the network has a specific derivation index, it filters the accounts accordingly.
     *
     * @param network the network to filter accounts by, can be null.
     * @return a set of [AccountStatus] that match the expected criteria.
     */
    private fun AccountStatusList.getExpectedAccountStatuses(network: Network?): List<AccountStatus> {
        val possibleAccountIndex = network?.getAccountIndexOrNull()

        return when (possibleAccountIndex) {
            // currency can be in any account
            null -> accountStatuses
            // currency only in the main account
            DerivationIndex.Main.value -> listOf(mainAccount)
            // currency only in the account with specific derivation index or in the main account
            else -> {
                val accountStatus = accountStatuses.firstOrNull {
                    val cryptoPortfolio = it.account as? Account.CryptoPortfolio ?: return@firstOrNull false

                    cryptoPortfolio.derivationIndex.value == possibleAccountIndex
                }

                listOfNotNull(accountStatus, mainAccount)
            }
        }
    }

    private fun AccountStatusList.getExpectedAccountStatuses(networks: List<Network>): List<AccountStatus> {
        val possibleAccountIndexes = networks.mapNotNull { it.getAccountIndexOrNull() }

        if (possibleAccountIndexes.isEmpty()) return this@getExpectedAccountStatuses.accountStatuses

        val accountStatuses = this@getExpectedAccountStatuses.accountStatuses.filter {
            val cryptoPortfolio = it.account as? Account.CryptoPortfolio ?: return@filter false

            cryptoPortfolio.derivationIndex.value in possibleAccountIndexes
        }

        return accountStatuses + listOf(mainAccount)
    }

    private fun Network.getAccountIndexOrNull(): Int? {
        val blockchain = Blockchain.fromNetworkId(networkId = rawId) ?: return null
        val recognizer = AccountNodeRecognizer(blockchain)

        return recognizer.recognize(derivationPath)?.toInt()
    }
}
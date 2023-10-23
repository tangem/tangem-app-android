package com.tangem.data.tokens.utils

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.CryptoCurrencyAmount
import com.tangem.domain.walletmanager.model.CryptoCurrencyTransaction
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import timber.log.Timber

internal class NetworkStatusFactory {

    fun createNetworkStatus(
        network: Network,
        result: UpdateWalletManagerResult,
        currencies: Set<CryptoCurrency>,
    ): NetworkStatus {
        return NetworkStatus(
            network = network,
            value = when (result) {
                is UpdateWalletManagerResult.MissedDerivation -> NetworkStatus.MissedDerivation
                is UpdateWalletManagerResult.Unreachable -> NetworkStatus.Unreachable
                is UpdateWalletManagerResult.NoAccount -> NetworkStatus.NoAccount(
                    address = getNetworkAddress(result.defaultAddress, result.addresses),
                    amountToCreateAccount = result.amountToCreateAccount,
                    errorMessage = result.errorMessage,
                )
                is UpdateWalletManagerResult.Verified -> NetworkStatus.Verified(
                    address = getNetworkAddress(result.defaultAddress, result.addresses),
                    amounts = formatAmounts(result.currenciesAmounts, currencies),
                    pendingTransactions = formatTransactions(
                        transactions = result.currentTransactions,
                        currencies = currencies,
                    ),
                )
            },
        )
    }

    private fun formatAmounts(
        amounts: Set<CryptoCurrencyAmount>,
        currencies: Set<CryptoCurrency>,
    ): Map<CryptoCurrency.ID, NetworkStatus.AmountStatus> {
        return currencies.associate { currency ->
            val amount = when (currency) {
                is CryptoCurrency.Coin -> {
                    amounts.singleOrNull { it is CryptoCurrencyAmount.Coin }
                }
                is CryptoCurrency.Token -> {
                    amounts.firstOrNull { amount ->
                        amount is CryptoCurrencyAmount.Token &&
                            currency.id.rawCurrencyId == amount.tokenId &&
                            currency.contractAddress == amount.tokenContractAddress
                    }
                }
            }
            if (amount == null) {
                Timber.e("Unable to find amount for cryptoCurrency: $currency")
                currency.id to NetworkStatus.UnreachableAmount
            } else {
                currency.id to NetworkStatus.LoadedAmount(amount.value)
            }
        }
    }

    private fun formatTransactions(
        transactions: Set<CryptoCurrencyTransaction>,
        currencies: Set<CryptoCurrency>,
    ): Map<CryptoCurrency.ID, Set<TxHistoryItem>> {
        if (transactions.isEmpty()) return emptyMap()

        return currencies
            .asSequence()
            .map { currency ->
                val currencyTransactions = when (currency) {
                    is CryptoCurrency.Coin -> transactions.filterTo(hashSetOf()) { transaction ->
                        transaction is CryptoCurrencyTransaction.Coin
                    }
                    is CryptoCurrency.Token -> transactions.filterTo(hashSetOf()) { transaction ->
                        transaction is CryptoCurrencyTransaction.Token &&
                            transaction.tokenId == currency.id.rawCurrencyId &&
                            transaction.tokenContractAddress == currency.contractAddress
                    }
                }

                currency.id to createCurrentTransactions(currencyTransactions)
            }
            .toMap()
    }

    private fun createCurrentTransactions(transactions: Set<CryptoCurrencyTransaction>): Set<TxHistoryItem> {
        return transactions.mapTo(hashSetOf()) { it.txHistoryItem }
    }

    private fun getNetworkAddress(defaultAddress: String, availableAddresses: Set<String>): NetworkAddress {
        return if (availableAddresses.size != 1) {
            NetworkAddress.Selectable(defaultAddress, availableAddresses)
        } else {
            NetworkAddress.Single(defaultAddress)
        }
    }
}
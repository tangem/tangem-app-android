package com.tangem.data.tokens.utils

import com.tangem.domain.tokens.model.CurrentTransaction
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.walletmanager.model.CryptoCurrencyAmount
import com.tangem.domain.walletmanager.model.CryptoCurrencyTransaction
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import timber.log.Timber
import java.math.BigDecimal

internal class NetworkStatusFactory {

    fun createNetworkStatus(
        networkId: Network.ID,
        result: UpdateWalletManagerResult,
        currencies: Set<CryptoCurrency>,
    ): NetworkStatus {
        return NetworkStatus(
            networkId = networkId,
            value = when (result) {
                is UpdateWalletManagerResult.MissedDerivation -> NetworkStatus.MissedDerivation
                is UpdateWalletManagerResult.Unreachable -> NetworkStatus.Unreachable
                is UpdateWalletManagerResult.NoAccount -> NetworkStatus.NoAccount(
                    address = getNetworkAddress(result.defaultAddress, result.addresses),
                    amountToCreateAccount = result.amountToCreateAccount,
                )
                is UpdateWalletManagerResult.Verified -> NetworkStatus.Verified(
                    address = getNetworkAddress(result.defaultAddress, result.addresses),
                    amounts = formatAmounts(result.currenciesAmounts, currencies),
                    currentTransactions = formatTransactions(
                        networksAddresses = result.addresses ?: setOf(result.defaultAddress),
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
    ): Map<CryptoCurrency.ID, BigDecimal> {
        return amounts
            .asSequence()
            .mapNotNull { amount ->
                val currency = when (amount) {
                    is CryptoCurrencyAmount.Coin -> currencies.singleOrNull { it is CryptoCurrency.Coin }
                    is CryptoCurrencyAmount.Token -> currencies.firstOrNull {
                        it is CryptoCurrency.Token &&
                            it.id.rawCurrencyId == amount.tokenId &&
                            it.contractAddress == amount.tokenContractAddress
                    }
                }

                if (currency == null) {
                    Timber.e("Unable to find cryptocurrency for amount: $amount")
                    null
                } else {
                    currency.id to amount.value
                }
            }
            .toMap()
    }

    private fun formatTransactions(
        networksAddresses: Set<String>,
        transactions: Set<CryptoCurrencyTransaction>,
        currencies: Set<CryptoCurrency>,
    ): Map<CryptoCurrency.ID, Set<CurrentTransaction>> {
        if (transactions.isEmpty()) return emptyMap()

        return currencies
            .asSequence()
            .map { currency ->
                val currencyTransactions = when (currency) {
                    is CryptoCurrency.Coin -> transactions.filterTo(hashSetOf()) {
                        it is CryptoCurrencyTransaction.Coin
                    }
                    is CryptoCurrency.Token -> transactions.filterTo(hashSetOf()) {
                        it is CryptoCurrencyTransaction.Token &&
                            it.tokenId == currency.id.rawCurrencyId &&
                            it.tokenContractAddress == currency.contractAddress
                    }
                }

                currency.id to createCurrentTransactions(networksAddresses, currencyTransactions)
            }
            .toMap()
    }

    private fun createCurrentTransactions(
        networksAddresses: Set<String>,
        transactions: Set<CryptoCurrencyTransaction>,
    ): Set<CurrentTransaction> {
        return transactions.mapNotNullTo(hashSetOf()) { createCurrentTransaction(networksAddresses, it) }
    }

    private fun createCurrentTransaction(
        networksAddresses: Set<String>,
        transaction: CryptoCurrencyTransaction,
    ): CurrentTransaction? {
        val direction = when {
            transaction.toAddress in networksAddresses -> CurrentTransaction.Direction.Incoming(
                fromAddress = transaction.fromAddress,
            )
            transaction.fromAddress in networksAddresses -> CurrentTransaction.Direction.Outgoing(
                toAddress = transaction.toAddress,
            )
            else -> {
                Timber.e(
                    """
                    Unable to find transaction direction
                    |- To address: ${transaction.toAddress}
                    |- From address: ${transaction.fromAddress}
                    |- Network addresses: $networksAddresses
                    """.trimIndent(),
                )

                return null
            }
        }

        return CurrentTransaction(
            amount = transaction.amount,
            direction = direction,
            sentAt = transaction.sentAt,
        )
    }

    private fun getNetworkAddress(defaultAddress: String, availableAddresses: Set<String>?): NetworkAddress {
        return if (availableAddresses != null) {
            NetworkAddress.Selectable(defaultAddress, availableAddresses)
        } else {
            NetworkAddress.Single(defaultAddress)
        }
    }
}

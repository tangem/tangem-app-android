package com.tangem.data.tokens.utils

import com.tangem.domain.tokens.model.*
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.walletmanager.model.Address
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
                is UpdateWalletManagerResult.Unreachable -> NetworkStatus.Unreachable(
                    address = getNetworkAddressOrNull(result.selectedAddress, result.addresses),
                )
                is UpdateWalletManagerResult.NoAccount -> NetworkStatus.NoAccount(
                    address = getNetworkAddress(result.selectedAddress, result.addresses),
                    amountToCreateAccount = result.amountToCreateAccount,
                    errorMessage = result.errorMessage,
                )
                is UpdateWalletManagerResult.Verified -> NetworkStatus.Verified(
                    address = getNetworkAddress(result.selectedAddress, result.addresses),
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
    ): Map<CryptoCurrency.ID, CryptoCurrencyAmountStatus> {
        return currencies.associate { currency ->
            val amount = when (currency) {
                is CryptoCurrency.Coin -> {
                    amounts.singleOrNull { it is CryptoCurrencyAmount.Coin }
                }
                is CryptoCurrency.Token -> {
                    amounts.firstOrNull { amount ->
                        amount is CryptoCurrencyAmount.Token &&
                            currency.id.rawCurrencyId == amount.tokenId &&
                            currency.contractAddress.equals(amount.tokenContractAddress, ignoreCase = true)
                    }
                }
            }

            if (amount == null) {
                Timber.w("Unable to find amount for cryptocurrency: $currency")
                currency.id to CryptoCurrencyAmountStatus.NotFound
            } else {
                currency.id to CryptoCurrencyAmountStatus.Loaded(amount.value)
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
                            transaction.tokenContractAddress.equals(currency.contractAddress, ignoreCase = true)
                    }
                }

                currency.id to createCurrentTransactions(currencyTransactions)
            }
            .toMap()
    }

    private fun createCurrentTransactions(transactions: Set<CryptoCurrencyTransaction>): Set<TxHistoryItem> {
        return transactions.mapTo(hashSetOf()) { it.txHistoryItem }
    }

    private fun getNetworkAddressOrNull(selectedAddress: String?, availableAddresses: Set<Address>?): NetworkAddress? {
        if (selectedAddress == null || availableAddresses == null) {
            return null
        }

        return getNetworkAddress(selectedAddress, availableAddresses)
    }

    private fun getNetworkAddress(selectedAddress: String, availableAddresses: Set<Address>): NetworkAddress {
        val defaultAddress = availableAddresses
            .firstOrNull { it.value == selectedAddress }
            ?.let(::mapToDomainAddress)

        requireNotNull(defaultAddress) { "Selected address must not be null" }

        return if (availableAddresses.size != 1) {
            NetworkAddress.Selectable(defaultAddress, availableAddresses.mapTo(hashSetOf(), ::mapToDomainAddress))
        } else {
            NetworkAddress.Single(defaultAddress)
        }
    }

    private fun mapToDomainAddress(address: Address): NetworkAddress.Address {
        val type = when (address.type) {
            Address.Type.Primary -> NetworkAddress.Address.Type.Primary
            Address.Type.Secondary -> NetworkAddress.Address.Type.Secondary
        }

        if (address.value.isBlank()) {
            Timber.w("Address value is blank")
        }

        return NetworkAddress.Address(address.value, type)
    }
}
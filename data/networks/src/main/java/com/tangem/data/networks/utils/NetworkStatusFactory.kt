package com.tangem.data.networks.utils

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyAmountStatus
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.walletmanager.model.Address
import com.tangem.domain.walletmanager.model.CryptoCurrencyAmount
import com.tangem.domain.walletmanager.model.CryptoCurrencyTransaction
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import timber.log.Timber

/** Factory for creating [NetworkStatus] */
object NetworkStatusFactory {

    /**
     * Create [NetworkStatus]
     *
     * @param network         network
     * @param updatingResult  result of updating wallet manager
     * @param addedCurrencies added currencies
     */
    fun create(
        network: Network,
        updatingResult: UpdateWalletManagerResult,
        addedCurrencies: Set<CryptoCurrency>,
    ): NetworkStatus {
        return NetworkStatus(
            network = network,
            value = when (updatingResult) {
                is UpdateWalletManagerResult.MissedDerivation -> NetworkStatus.MissedDerivation
                is UpdateWalletManagerResult.Unreachable -> createUnreachableStatus(result = updatingResult)
                is UpdateWalletManagerResult.NoAccount -> createNoAccount(result = updatingResult)
                is UpdateWalletManagerResult.Verified -> {
                    createVerifiedStatus(result = updatingResult, addedCurrencies = addedCurrencies)
                }
            },
        )
    }

    private fun createUnreachableStatus(result: UpdateWalletManagerResult.Unreachable): NetworkStatus.Unreachable {
        return NetworkStatus.Unreachable(
            address = getNetworkAddressOrNull(
                selectedAddress = result.selectedAddress,
                availableAddresses = result.addresses,
            ),
        )
    }

    private fun createNoAccount(result: UpdateWalletManagerResult.NoAccount): NetworkStatus.NoAccount {
        return NetworkStatus.NoAccount(
            address = getNetworkAddress(
                selectedAddress = result.selectedAddress,
                availableAddresses = result.addresses,
            ),
            amountToCreateAccount = result.amountToCreateAccount,
            errorMessage = result.errorMessage,
            source = StatusSource.ACTUAL,
        )
    }

    private fun createVerifiedStatus(
        result: UpdateWalletManagerResult.Verified,
        addedCurrencies: Set<CryptoCurrency>,
    ): NetworkStatus.Verified {
        return NetworkStatus.Verified(
            address = getNetworkAddress(
                selectedAddress = result.selectedAddress,
                availableAddresses = result.addresses,
            ),
            amounts = formatAmounts(amounts = result.currenciesAmounts, currencies = addedCurrencies),
            pendingTransactions = formatTransactions(
                transactions = result.currentTransactions,
                currencies = addedCurrencies,
            ),
            source = StatusSource.ACTUAL,
        )
    }

    private fun getNetworkAddressOrNull(selectedAddress: String?, availableAddresses: Set<Address>?): NetworkAddress? {
        if (selectedAddress.isNullOrBlank() || availableAddresses == null) return null

        return getNetworkAddress(selectedAddress, availableAddresses)
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
    ): Map<CryptoCurrency.ID, Set<TxInfo>> {
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

    private fun createCurrentTransactions(transactions: Set<CryptoCurrencyTransaction>): Set<TxInfo> {
        return transactions.mapTo(hashSetOf()) { it.txInfo }
    }

    private fun getNetworkAddress(selectedAddress: String, availableAddresses: Set<Address>): NetworkAddress {
        val defaultAddress = availableAddresses
            .firstOrNull { it.value == selectedAddress }
            ?.let(::mapToDomainAddress)

        require(defaultAddress != null && defaultAddress.value.isNotBlank()) {
            "Selected address must not be null"
        }

        return if (availableAddresses.size != 1) {
            NetworkAddress.Selectable(
                defaultAddress = defaultAddress,
                availableAddresses = availableAddresses.mapTo(hashSetOf(), ::mapToDomainAddress),
            )
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
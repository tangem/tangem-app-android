package com.tangem.data.tokens.utils

import com.tangem.common.extensions.mapNotNullValues
import com.tangem.data.tokens.entity.NetworkStatusDM
import com.tangem.domain.tokens.model.*
import timber.log.Timber
import java.math.BigDecimal

internal object NetworkStatusMapper {

    fun toDataModel(network: Network, networkStatus: NetworkStatus.Verified): NetworkStatusDM {
        return NetworkStatusDM(
            networkId = network.id,
            selectedAddress = networkStatus.address.defaultAddress.value,
            availableAddresses = networkStatus.address.availableAddresses
                .map { address ->
                    NetworkStatusDM.Address(
                        value = address.value,
                        type = when (address.type) {
                            NetworkAddress.Address.Type.Primary -> NetworkStatusDM.Address.Type.Primary
                            NetworkAddress.Address.Type.Secondary -> NetworkStatusDM.Address.Type.Secondary
                        },
                    )
                }
                .toSet(),
            amounts = networkStatus.amounts
                .mapKeys { (id, _) -> id.value }
                .mapNotNullValues { (_, amount) ->
                    when (amount) {
                        is CryptoCurrencyAmountStatus.Loaded -> amount.value
                        is CryptoCurrencyAmountStatus.NotFound -> null
                    }
                },
        )
    }

    fun toDomainModel(network: Network, currencies: Sequence<CryptoCurrency>, status: NetworkStatusDM): NetworkStatus {
        return NetworkStatus(
            network = network,
            value = NetworkStatus.Verified(
                address = mapToDomainAddress(status.selectedAddress, status.availableAddresses),
                amounts = mapToDomainAmounts(status.amounts, currencies.filter { it.network == network }),
                pendingTransactions = mapOf(),
            ),
        )
    }

    private fun mapToDomainAmounts(
        amounts: Map<String, BigDecimal>,
        currencies: Sequence<CryptoCurrency>,
    ): Map<CryptoCurrency.ID, CryptoCurrencyAmountStatus> {
        val currenciesIds = currencies.map { it.id }.toSet()

        return amounts
            .mapKeys { CryptoCurrency.ID.fromValue(it.key) }
            .mapValues { (currencyId, amount) ->
                if (currencyId in currenciesIds) {
                    CryptoCurrencyAmountStatus.Loaded(amount)
                } else {
                    CryptoCurrencyAmountStatus.NotFound
                }
            }
    }

    private fun mapToDomainAddress(
        selectedAddress: String,
        availableAddresses: Set<NetworkStatusDM.Address>,
    ): NetworkAddress {
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

    private fun mapToDomainAddress(address: NetworkStatusDM.Address): NetworkAddress.Address {
        val type = when (address.type) {
            NetworkStatusDM.Address.Type.Primary -> NetworkAddress.Address.Type.Primary
            NetworkStatusDM.Address.Type.Secondary -> NetworkAddress.Address.Type.Secondary
        }

        if (address.value.isBlank()) {
            Timber.w("Address value is blank")
        }

        return NetworkAddress.Address(address.value, type)
    }
}
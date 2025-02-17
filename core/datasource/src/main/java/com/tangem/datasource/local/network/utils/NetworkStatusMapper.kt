package com.tangem.datasource.local.network.utils

import com.tangem.common.extensions.mapNotNullValues
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.tokens.model.*
import timber.log.Timber
import java.math.BigDecimal

internal fun NetworkStatus.Verified.toDataModel(network: Network): NetworkStatusDM {
    return NetworkStatusDM(
        networkId = network.id,
        selectedAddress = address.defaultAddress.value,
        availableAddresses = address.availableAddresses
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
        amounts = amounts
            .mapKeys { (id, _) -> id.value }
            .mapNotNullValues { (_, amount) ->
                when (amount) {
                    is CryptoCurrencyAmountStatus.Loaded -> amount.value
                    is CryptoCurrencyAmountStatus.NotFound -> null
                }
            },
    )
}

internal fun NetworkStatusDM.toDomainModel(network: Network, isCached: Boolean): NetworkStatus {
    return NetworkStatus(
        network = network,
        value = NetworkStatus.Verified(
            address = mapToDomainAddress(selectedAddress, availableAddresses),
            amounts = mapToDomainAmounts(amounts),
            pendingTransactions = mapOf(),
        ),
        isCached = isCached,
    )
}

private fun mapToDomainAmounts(amounts: Map<String, BigDecimal>): Map<CryptoCurrency.ID, CryptoCurrencyAmountStatus> {
    return amounts
        .mapKeys { CryptoCurrency.ID.fromValue(it.key) }
        .mapValues { (_, amount) -> CryptoCurrencyAmountStatus.Loaded(amount) }
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
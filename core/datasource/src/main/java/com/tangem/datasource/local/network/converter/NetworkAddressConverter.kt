package com.tangem.datasource.local.network.converter

import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.utils.converter.TwoWayConverter
import timber.log.Timber

/**
 * Converter from [Set<NetworkStatusDM.Address>] to [NetworkAddress] and vice versa
 *
[REDACTED_AUTHOR]
 */
internal class NetworkAddressConverter(
    private val selectedAddress: String,
) : TwoWayConverter<Set<NetworkStatusDM.Address>, NetworkAddress> {

    override fun convert(value: Set<NetworkStatusDM.Address>): NetworkAddress {
        val defaultAddress = value
            .firstOrNull { it.value == selectedAddress }
            ?.let(::toNetworkAddress)

        requireNotNull(defaultAddress) { "Selected address must not be null" }

        return if (value.size != 1) {
            NetworkAddress.Selectable(
                defaultAddress = defaultAddress,
                availableAddresses = value.mapTo(destination = hashSetOf(), transform = ::toNetworkAddress),
            )
        } else {
            NetworkAddress.Single(defaultAddress = defaultAddress)
        }
    }

    override fun convertBack(value: NetworkAddress): Set<NetworkStatusDM.Address> {
        return value.availableAddresses
            .map { address ->
                NetworkStatusDM.Address(
                    value = address.value,
                    type = when (address.type) {
                        NetworkAddress.Address.Type.Primary -> NetworkStatusDM.Address.Type.Primary
                        NetworkAddress.Address.Type.Secondary -> NetworkStatusDM.Address.Type.Secondary
                    },
                )
            }
            .toSet()
    }

    private fun toNetworkAddress(address: NetworkStatusDM.Address): NetworkAddress.Address {
        val type = when (address.type) {
            NetworkStatusDM.Address.Type.Primary -> NetworkAddress.Address.Type.Primary
            NetworkStatusDM.Address.Type.Secondary -> NetworkAddress.Address.Type.Secondary
        }

        if (address.value.isBlank()) {
            Timber.w("Address value is blank")
        }

        return NetworkAddress.Address(value = address.value, type = type)
    }
}
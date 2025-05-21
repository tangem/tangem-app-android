package com.tangem.data.networks.converters

import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.utils.converter.TwoWayConverter
import timber.log.Timber

/**
 * Converter from [NetworkAddressConverter.Value] to [NetworkAddress] and vice versa
 *
[REDACTED_AUTHOR]
 */
internal object NetworkAddressConverter : TwoWayConverter<NetworkAddressConverter.Value, NetworkAddress> {

    data class Value(
        val selectedAddress: String,
        val addresses: Set<NetworkStatusDM.Address>,
    )

    override fun convert(value: Value): NetworkAddress {
        val defaultAddress = value.addresses
            .firstOrNull { it.value == value.selectedAddress }
            ?.let(::toNetworkAddress)

        requireNotNull(defaultAddress) { "Selected address must not be null" }

        return if (value.addresses.size != 1) {
            NetworkAddress.Selectable(
                defaultAddress = defaultAddress,
                availableAddresses = value.addresses.mapTo(destination = hashSetOf(), transform = ::toNetworkAddress),
            )
        } else {
            NetworkAddress.Single(defaultAddress = defaultAddress)
        }
    }

    override fun convertBack(value: NetworkAddress): Value {
        return Value(
            selectedAddress = value.defaultAddress.value,
            addresses = value.availableAddresses
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
        )
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
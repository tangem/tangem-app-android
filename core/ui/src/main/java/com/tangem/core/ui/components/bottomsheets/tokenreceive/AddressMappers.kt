package com.tangem.core.ui.components.bottomsheets.tokenreceive

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkAddress

private const val DSC_ADDRESS_NAME = "DSC"
private const val DEL_ADDRESS_NAME = "Main"

private const val LEGACY_ADDRESS_NAME = "Legacy"
private const val DEFAULT_ADDRESS_NAME = "Default"

fun Set<NetworkAddress.Address>.mapToAddressModels(currency: CryptoCurrency): List<AddressModel> = this
    .sortedBy { it.type.ordinal }
    .map { address ->
        AddressModel(
            displayName = currency.network.getAddressDisplayName(address.type),
            value = address.value,
            type = when (address.type) {
                NetworkAddress.Address.Type.Primary -> AddressModel.Type.Default
                NetworkAddress.Address.Type.Secondary -> AddressModel.Type.Legacy
            },
        )
    }

private fun Network.getAddressDisplayName(addressType: NetworkAddress.Address.Type): TextReference {
    return when (id.value) {
        "decimal", "decimal/test" -> {
            when (addressType) {
                NetworkAddress.Address.Type.Primary -> stringReference(value = DEL_ADDRESS_NAME)
                NetworkAddress.Address.Type.Secondary -> stringReference(value = DSC_ADDRESS_NAME)
            }
        }
        else -> {
            when (addressType) {
                NetworkAddress.Address.Type.Primary -> stringReference(value = DEFAULT_ADDRESS_NAME)
                NetworkAddress.Address.Type.Secondary -> stringReference(value = LEGACY_ADDRESS_NAME)
            }
        }
    }
}
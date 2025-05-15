package com.tangem.data.networks.converters

import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.utils.converter.Converter

/**
 * Converter from [NetworkStatus] to [NetworkStatusDM]
 *
[REDACTED_AUTHOR]
 */
internal object NetworkStatusDataModelConverter : Converter<NetworkStatus, NetworkStatusDM?> {

    override fun convert(value: NetworkStatus): NetworkStatusDM? {
        return when (val status = value.value) {
            is NetworkStatus.Verified -> {
                NetworkStatusDM.Verified(
                    networkId = value.network.id,
                    derivationPath = NetworkDerivationPathConverter.convertBack(value = value.network.derivationPath),
                    selectedAddress = status.address.defaultAddress.value,
                    availableAddresses = NetworkAddressConverter(selectedAddress = status.address.defaultAddress.value)
                        .convertBack(value = status.address),
                    amounts = NetworkAmountsConverter.convertBack(value = status.amounts),
                )
            }
            is NetworkStatus.NoAccount -> {
                NetworkStatusDM.NoAccount(
                    networkId = value.network.id,
                    derivationPath = NetworkDerivationPathConverter.convertBack(value = value.network.derivationPath),
                    selectedAddress = status.address.defaultAddress.value,
                    availableAddresses = NetworkAddressConverter(selectedAddress = status.address.defaultAddress.value)
                        .convertBack(value = status.address),
                    amountToCreateAccount = status.amountToCreateAccount,
                    errorMessage = status.errorMessage,
                )
            }
            else -> null
        }
    }
}
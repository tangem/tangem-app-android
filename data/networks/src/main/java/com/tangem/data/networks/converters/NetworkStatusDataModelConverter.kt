package com.tangem.data.networks.converters

import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.network.NetworkStatus
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
                val address = NetworkAddressConverter.convertBack(value = status.address)

                NetworkStatusDM.Verified(
                    networkId = NetworkStatusDM.ID(value = value.network.rawId),
                    derivationPath = NetworkDerivationPathConverter.convertBack(value = value.network.derivationPath),
                    selectedAddress = address.selectedAddress,
                    availableAddresses = address.addresses,
                    amounts = NetworkAmountsConverter.convertBack(value = status.amounts),
                    yieldSupplyStatuses = NetworkYieldSupplyStatusConverter.convertBack(status.yieldSupplyStatuses),
                )
            }
            is NetworkStatus.NoAccount -> {
                val address = NetworkAddressConverter.convertBack(value = status.address)

                NetworkStatusDM.NoAccount(
                    networkId = NetworkStatusDM.ID(value = value.network.rawId),
                    derivationPath = NetworkDerivationPathConverter.convertBack(value = value.network.derivationPath),
                    selectedAddress = address.selectedAddress,
                    availableAddresses = address.addresses,
                    amountToCreateAccount = status.amountToCreateAccount,
                    errorMessage = status.errorMessage,
                )
            }
            else -> null
        }
    }
}
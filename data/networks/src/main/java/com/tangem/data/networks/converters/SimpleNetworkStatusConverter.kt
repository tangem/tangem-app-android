package com.tangem.data.networks.converters

import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.utils.converter.Converter

/**
 * Converter from [NetworkStatusDM] to [SimpleNetworkStatus]
 *
[REDACTED_AUTHOR]
 */
internal object SimpleNetworkStatusConverter : Converter<NetworkStatusDM, SimpleNetworkStatus> {

    override fun convert(value: NetworkStatusDM): SimpleNetworkStatus {
        val address = NetworkAddressConverter.convert(
            value = NetworkAddressConverter.Value(
                selectedAddress = value.selectedAddress,
                addresses = value.availableAddresses,
            ),
        )

        val derivationPath = NetworkDerivationPathConverter.convert(value = value.derivationPath)

        val amountsConverter = NetworkAmountsConverter(
            rawNetworkId = value.networkId.value,
            derivationPath = derivationPath,
        )

        val yieldSupplyStatusConverter = NetworkYieldSupplyStatusConverter(
            rawNetworkId = value.networkId.value,
            derivationPath = derivationPath,
        )

        val status = when (value) {
            is NetworkStatusDM.Verified -> {
                NetworkStatus.Verified(
                    address = address,
                    amounts = amountsConverter.convert(value = value.amounts),
                    pendingTransactions = emptyMap(),
                    source = StatusSource.CACHE,
                    yieldSupplyStatuses = yieldSupplyStatusConverter.convert(value = value.yieldSupplyStatuses),
                )
            }
            is NetworkStatusDM.NoAccount -> {
                NetworkStatus.NoAccount(
                    address = address,
                    amountToCreateAccount = value.amountToCreateAccount,
                    errorMessage = value.errorMessage,
                    source = StatusSource.CACHE,
                )
            }
        }

        return SimpleNetworkStatus(
            id = Network.ID(
                value = value.networkId.value,
                derivationPath = NetworkDerivationPathConverter.convert(value = value.derivationPath),
            ),
            value = status,
        )
    }
}
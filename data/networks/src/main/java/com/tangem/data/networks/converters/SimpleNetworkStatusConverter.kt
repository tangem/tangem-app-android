package com.tangem.data.networks.converters

import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.NetworkStatus
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

        val status = when (value) {
            is NetworkStatusDM.Verified -> {
                NetworkStatus.Verified(
                    address = address,
                    amounts = NetworkAmountsConverter.convert(value = value.amounts),
                    pendingTransactions = emptyMap(),
                    source = StatusSource.CACHE,
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
            id = SimpleNetworkStatus.Id(
                networkId = value.networkId,
                derivationPath = NetworkDerivationPathConverter.convert(value = value.derivationPath),
            ),
            value = status,
        )
    }
}
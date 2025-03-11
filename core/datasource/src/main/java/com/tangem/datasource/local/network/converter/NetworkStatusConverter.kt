package com.tangem.datasource.local.network.converter

import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.utils.converter.Converter

/**
 * Converter from [NetworkStatusDM] to [NetworkStatus]
 *
 * @property network  network
 * @property isCached flag that determines whether the status is a cache
 *
[REDACTED_AUTHOR]
 */
internal class NetworkStatusConverter(
    private val network: Network,
    private val isCached: Boolean,
) : Converter<NetworkStatusDM, NetworkStatus> {

    override fun convert(value: NetworkStatusDM): NetworkStatus {
        val address = NetworkAddressConverter(selectedAddress = value.selectedAddress)
            .convert(value = value.availableAddresses)

        val status = when (value) {
            is NetworkStatusDM.Verified -> {
                NetworkStatus.Verified(
                    address = address,
                    amounts = NetworkAmountsConverter.convert(value = value.amounts),
                    pendingTransactions = mapOf(),
                    source = if (isCached) StatusSource.CACHE else StatusSource.ACTUAL,
                )
            }
            is NetworkStatusDM.NoAccount -> {
                NetworkStatus.NoAccount(
                    address = address,
                    amountToCreateAccount = value.amountToCreateAccount,
                    errorMessage = value.errorMessage,
                    source = if (isCached) StatusSource.CACHE else StatusSource.ACTUAL,
                )
            }
        }

        return NetworkStatus(network = network, value = status)
    }
}
package com.tangem.data.notifications.converters

import com.tangem.datasource.api.tangemTech.models.CryptoNetworkResponse
import com.tangem.domain.notifications.models.NotificationsEligibleNetwork
import com.tangem.utils.converter.Converter

internal object NotificationsEligibleNetworkConverter : Converter<CryptoNetworkResponse, NotificationsEligibleNetwork> {
    override fun convert(value: CryptoNetworkResponse): NotificationsEligibleNetwork {
        return NotificationsEligibleNetwork(
            id = value.id.toString(),
            networkId = value.networkId,
            name = value.name,
        )
    }
}
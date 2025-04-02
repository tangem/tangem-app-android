package com.tangem.data.networks

import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.datasource.local.network.converter.NetworkStatusDataModelConverter
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.tokens.model.NetworkStatus

internal fun NetworkStatus.toSimple(): SimpleNetworkStatus {
    return SimpleNetworkStatus(
        id = SimpleNetworkStatus.Id(network = network),
        value = value,
    )
}

internal fun NetworkStatus.toDataModel(): NetworkStatusDM? {
    return NetworkStatusDataModelConverter.convert(value = this)
}
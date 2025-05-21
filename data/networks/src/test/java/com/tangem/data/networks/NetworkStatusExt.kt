package com.tangem.data.networks

import com.tangem.data.networks.converters.NetworkStatusDataModelConverter
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.network.NetworkStatus

internal fun NetworkStatus.toSimple() = SimpleNetworkStatus(status = this)

internal fun NetworkStatus.toDataModel(): NetworkStatusDM? {
    return NetworkStatusDataModelConverter.convert(value = this)
}
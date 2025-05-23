package com.tangem.data.networks.models

import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus

/**
 * Simplified model of network status. Like [NetworkStatus] but without metadata.
 *
 * @property id    identifier
 * @property value value
 */
internal data class SimpleNetworkStatus(val id: Network.ID, val value: NetworkStatus.Value) {

    /** Constructs [SimpleNetworkStatus] from [NetworkStatus] */
    constructor(status: NetworkStatus) : this(id = status.network.id, value = status.value)
}
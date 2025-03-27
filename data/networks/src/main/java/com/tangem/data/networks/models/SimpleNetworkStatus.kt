package com.tangem.data.networks.models

import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus

/**
 * Simplified model of network status. Like [NetworkStatus] but without metadata.
 *
 * @property id    identifier
 * @property value value
 */
internal data class SimpleNetworkStatus(val id: Id, val value: NetworkStatus.Value) {

    /**
     * Identifier of [SimpleNetworkStatus]
     *
     * @property networkId      network id
     * @property derivationPath derivation path
     */
    data class Id(val networkId: Network.ID, val derivationPath: Network.DerivationPath) {

        constructor(network: Network) : this(
            networkId = network.id,
            derivationPath = network.derivationPath,
        )
    }
}
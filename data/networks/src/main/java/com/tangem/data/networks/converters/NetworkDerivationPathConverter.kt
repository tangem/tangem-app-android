package com.tangem.data.networks.converters

import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.network.Network
import com.tangem.utils.converter.TwoWayConverter

/**
 * Converter from [NetworkStatusDM.DerivationPath] to [Network.DerivationPath] and vice versa
 *
[REDACTED_AUTHOR]
 */
internal object NetworkDerivationPathConverter :
    TwoWayConverter<NetworkStatusDM.DerivationPath, Network.DerivationPath> {

    override fun convert(value: NetworkStatusDM.DerivationPath): Network.DerivationPath {
        return when (value.type) {
            NetworkStatusDM.DerivationPath.Type.CARD -> Network.DerivationPath.Card(value.value)
            NetworkStatusDM.DerivationPath.Type.CUSTOM -> Network.DerivationPath.Custom(value.value)
            NetworkStatusDM.DerivationPath.Type.NONE -> Network.DerivationPath.None
        }
    }

    override fun convertBack(value: Network.DerivationPath): NetworkStatusDM.DerivationPath {
        return NetworkStatusDM.DerivationPath(
            value = value.value.orEmpty(),
            type = when (value) {
                is Network.DerivationPath.Card -> NetworkStatusDM.DerivationPath.Type.CARD
                is Network.DerivationPath.Custom -> NetworkStatusDM.DerivationPath.Type.CUSTOM
                Network.DerivationPath.None -> NetworkStatusDM.DerivationPath.Type.NONE
            },
        )
    }
}
package com.tangem.datasource.local.network.converter

import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.datasource.local.network.entity.NetworkStatusDM.DerivationPath.Type
import com.tangem.domain.tokens.model.Network
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
            Type.CARD -> Network.DerivationPath.Card(value.value)
            Type.CUSTOM -> Network.DerivationPath.Custom(value.value)
            Type.NONE -> Network.DerivationPath.None
        }
    }

    override fun convertBack(value: Network.DerivationPath): NetworkStatusDM.DerivationPath {
        return NetworkStatusDM.DerivationPath(
            value = value.value.orEmpty(),
            type = when (value) {
                is Network.DerivationPath.Card -> Type.CARD
                is Network.DerivationPath.Custom -> Type.CUSTOM
                Network.DerivationPath.None -> Type.NONE
            },
        )
    }
}
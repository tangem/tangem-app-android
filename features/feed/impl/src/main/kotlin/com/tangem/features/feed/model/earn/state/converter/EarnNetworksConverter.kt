package com.tangem.features.feed.model.earn.state.converter

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.domain.models.earn.EarnNetwork
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.utils.converter.Converter

internal class EarnNetworksConverter : Converter<EarnNetwork, EarnFilterNetworkUM.Network> {

    override fun convert(value: EarnNetwork): EarnFilterNetworkUM.Network {
        return EarnFilterNetworkUM.Network(
            id = value.networkId,
            text = TextReference.Str(value.fullName),
            symbol = TextReference.Str(value.symbol),
            iconRes = getActiveIconRes(Blockchain.fromNetworkId(value.networkId)?.id.orEmpty()),
            isSelected = false,
        )
    }
}
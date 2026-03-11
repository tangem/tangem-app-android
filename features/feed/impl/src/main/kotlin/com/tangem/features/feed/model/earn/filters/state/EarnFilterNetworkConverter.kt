package com.tangem.features.feed.model.earn.filters.state

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.utils.converter.Converter

internal class EarnFilterNetworkConverter : Converter<EarnFilterNetwork, EarnFilterNetworkUM> {

    override fun convert(value: EarnFilterNetwork): EarnFilterNetworkUM {
        return when (value) {
            is EarnFilterNetwork.AllNetworks -> {
                EarnFilterNetworkUM.AllNetworks(isSelected = value.isSelected)
            }
            is EarnFilterNetwork.MyNetworks -> {
                EarnFilterNetworkUM.MyNetworks(isSelected = value.isSelected)
            }
            is EarnFilterNetwork.Specific -> {
                EarnFilterNetworkUM.Network(
                    id = value.id,
                    text = value.fullName,
                    symbol = value.symbol,
                    iconRes = getActiveIconRes(Blockchain.fromNetworkId(value.id)?.id.orEmpty()),
                    isSelected = value.isSelected,
                )
            }
        }
    }
}
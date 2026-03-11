package com.tangem.features.feed.model.earn.filters.state

import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.utils.converter.Converter

internal class EarnFilterNetworkUMConverter : Converter<EarnFilterNetworkUM, EarnFilterNetwork> {

    override fun convert(value: EarnFilterNetworkUM): EarnFilterNetwork {
        return when (value) {
            is EarnFilterNetworkUM.AllNetworks -> EarnFilterNetwork.AllNetworks(isSelected = value.isSelected)
            is EarnFilterNetworkUM.MyNetworks -> EarnFilterNetwork.MyNetworks(isSelected = value.isSelected)
            is EarnFilterNetworkUM.Network -> EarnFilterNetwork.Specific(
                isSelected = value.isSelected,
                id = value.id,
                symbol = value.symbol,
                fullName = value.text,
            )
        }
    }
}
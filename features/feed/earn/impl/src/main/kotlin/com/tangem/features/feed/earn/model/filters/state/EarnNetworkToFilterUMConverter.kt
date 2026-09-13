package com.tangem.features.feed.earn.model.filters.state

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.earn.EarnNetwork
import com.tangem.features.feed.earn.ui.state.EarnFilterNetworkUM
import com.tangem.utils.converter.Converter

internal class EarnNetworkToFilterUMConverter(
    private val selectedNetworkId: String?,
    private val onClick: (EarnNetwork) -> Unit,
) : Converter<EarnNetwork, EarnFilterNetworkUM.Network> {

    override fun convert(value: EarnNetwork): EarnFilterNetworkUM.Network {
        return EarnFilterNetworkUM.Network(
            id = value.networkId,
            name = stringReference(value.fullName),
            symbol = value.symbol,
            iconRes = getActiveIconRes(Blockchain.fromNetworkId(value.networkId) ?: Blockchain.Unknown),
            isSelected = value.networkId == selectedNetworkId,
            onClick = { onClick(value) },
        )
    }
}
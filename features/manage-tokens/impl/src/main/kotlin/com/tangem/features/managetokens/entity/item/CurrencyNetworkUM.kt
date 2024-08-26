package com.tangem.features.managetokens.entity.item

import com.tangem.domain.tokens.model.Network

internal data class CurrencyNetworkUM(
    val networkId: Network.ID,
    val name: String,
    val type: String,
    val iconResId: Int,
    val isMainNetwork: Boolean,
    override val isSelected: Boolean,
    override val onSelectedStateChange: (Boolean) -> Unit,
) : SelectableItemUM {

    override val id: String = networkId.value
}

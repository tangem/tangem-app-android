package com.tangem.features.managetokens.entity.item

import com.tangem.domain.tokens.model.Network

internal data class CurrencyNetworkUM(
    val network: Network,
    val name: String,
    val type: String,
    val iconResId: Int,
    val isMainNetwork: Boolean,
    val longTapConfig: LongTapConfig?,
    override val isSelected: Boolean,
    override val onSelectedStateChange: (Boolean) -> Unit,
) : SelectableItemUM {

    override val id: String = network.id.value

    data class LongTapConfig(val contractAddress: String, val onLongTap: () -> Unit)
}

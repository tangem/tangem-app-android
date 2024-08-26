package com.tangem.features.managetokens.entity.item

import com.tangem.core.ui.extensions.TextReference

internal data class DerivationPathUM(
    val value: String,
    val blockchainName: TextReference,
    override val isSelected: Boolean,
    override val onSelectedStateChange: (Boolean) -> Unit,
) : SelectableItemUM {

    override val id: String = value
}

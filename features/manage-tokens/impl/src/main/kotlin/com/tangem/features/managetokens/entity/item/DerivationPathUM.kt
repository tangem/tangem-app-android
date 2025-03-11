package com.tangem.features.managetokens.entity.item

import com.tangem.core.ui.extensions.TextReference

internal data class DerivationPathUM(
    override val id: String,
    val value: String,
    val networkName: TextReference,
    override val isSelected: Boolean,
    override val onSelectedStateChange: (Boolean) -> Unit,
) : SelectableItemUM
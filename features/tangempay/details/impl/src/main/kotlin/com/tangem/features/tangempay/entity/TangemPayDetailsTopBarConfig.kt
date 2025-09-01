package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayDetailsTopBarConfig(
    val onBackClick: () -> Unit,
    val items: ImmutableList<TangemDropdownMenuItem>?,
)
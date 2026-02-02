package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayDetailsTopBarConfig(
    val onBackClick: () -> Unit,
    val onOpenMenu: () -> Unit,
    val items: ImmutableList<TangemPayDetailsTopBarMenuItem>?,
)

internal data class TangemPayDetailsTopBarMenuItem(
    val type: TangemPayDetailsTopBarMenuItemType,
    val dropdownItem: TangemDropdownMenuItem,
)

internal enum class TangemPayDetailsTopBarMenuItemType {
    ChangePin,
    TermsAndLimits,
    FreezeCard,
    UnfreezeCard,
}
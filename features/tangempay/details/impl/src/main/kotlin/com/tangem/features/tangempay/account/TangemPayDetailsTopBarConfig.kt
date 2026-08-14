package com.tangem.features.tangempay.account

import com.tangem.features.tangempay.common.TangemPayDropDownItemUM
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayDetailsTopBarConfig(
    val onBackClick: () -> Unit,
    val onOpenMenu: () -> Unit,
    val items: ImmutableList<TangemPayDropDownItemUM>,
)
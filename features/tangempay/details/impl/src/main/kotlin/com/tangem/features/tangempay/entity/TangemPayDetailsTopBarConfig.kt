package com.tangem.features.tangempay.entity

import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayDetailsTopBarConfig(
    val onBackClick: () -> Unit,
    val onOpenMenu: () -> Unit,
    val items: ImmutableList<TangemPayDropDownItemUM>,
)
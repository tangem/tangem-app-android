package com.tangem.features.details.entity

import kotlinx.collections.immutable.ImmutableList

internal data class DetailsUM(
    val items: ImmutableList<DetailsItemUM>,
    val footer: DetailsFooterUM,
    val popBack: () -> Unit,
)
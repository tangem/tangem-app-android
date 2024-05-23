package com.tangem.features.details.entity

import kotlinx.collections.immutable.ImmutableList

internal data class DetailsState(
    val blocks: ImmutableList<DetailsBlock>,
    val footer: DetailsFooter,
    val popBack: () -> Unit,
)

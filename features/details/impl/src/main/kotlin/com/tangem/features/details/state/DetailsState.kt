package com.tangem.features.details.state

import kotlinx.collections.immutable.ImmutableList

data class DetailsState(
    val blocks: ImmutableList<DetailsBlock>,
    val footer: DetailsFooter,
    val popBack: () -> Unit,
)

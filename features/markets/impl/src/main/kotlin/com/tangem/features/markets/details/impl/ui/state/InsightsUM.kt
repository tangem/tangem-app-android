package com.tangem.features.markets.details.impl.ui.state

import kotlinx.collections.immutable.ImmutableList

internal data class InsightsUM(
    val h24Info: ImmutableList<InfoPointUM>,
    val weekInfo: ImmutableList<InfoPointUM>,
    val monthInfo: ImmutableList<InfoPointUM>,
)

package com.tangem.features.markets.details.impl.ui.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList

@Immutable
internal data class InsightsUM(
    val h24Info: PersistentList<InfoPointUM>,
    val weekInfo: PersistentList<InfoPointUM>,
    val monthInfo: PersistentList<InfoPointUM>,
)

package com.tangem.features.markets.details.impl.ui.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList

@Immutable
internal data class MetricsUM(
    val metrics: PersistentList<InfoPointUM>,
)

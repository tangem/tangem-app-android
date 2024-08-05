package com.tangem.features.markets.details.impl.ui.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
data class InfoPointUM(
    val title: TextReference,
    val value: String,
    val onInfoClick: (() -> Unit)? = null,
)

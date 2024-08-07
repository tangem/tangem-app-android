package com.tangem.features.markets.details.impl.ui.state

import com.tangem.core.ui.extensions.TextReference

internal data class InfoPointUM(
    val title: TextReference,
    val value: String,
    val onInfoClick: (() -> Unit)? = null,
)

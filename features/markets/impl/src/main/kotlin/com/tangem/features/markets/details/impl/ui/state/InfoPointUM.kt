package com.tangem.features.markets.details.impl.ui.state

import com.tangem.core.ui.extensions.TextReference

internal data class InfoPointUM(
    val title: TextReference,
    val value: String,
    val change: ChangeType? = null,
    val onInfoClick: (() -> Unit)? = null,
) {
    enum class ChangeType {
        UP, DOWN
    }
}
package com.tangem.features.feed.ui.market.detailed.state

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
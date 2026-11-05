package com.tangem.features.polymarket.impl.main.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface PolymarketPositionsPreviewUM {

    val hasFinished: Boolean

    val isVisible: Boolean
        get() = this !is Content || positions.isNotEmpty() || onRetry != null

    data object Loading : PolymarketPositionsPreviewUM {
        override val hasFinished: Boolean = false
    }

    data class Content(
        val positions: ImmutableList<PolymarketPositionUM>,
        val hasMoreActive: Boolean = false,
        override val hasFinished: Boolean = false,
        val onRetry: (() -> Unit)? = null,
    ) : PolymarketPositionsPreviewUM

    data class Error(
        val onRetry: () -> Unit,
        override val hasFinished: Boolean = false,
    ) : PolymarketPositionsPreviewUM
}
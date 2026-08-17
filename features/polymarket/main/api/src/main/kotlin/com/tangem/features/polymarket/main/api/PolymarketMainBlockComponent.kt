package com.tangem.features.polymarket.main.api

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.features.polymarket.main.api.entity.PolymarketMainUM

/**
 * The Prediction account row of the wallet screen.
 *
 * Renders into the caller's list rather than into a container of its own, because the row sits among the wallet's
 * accounts and shares their shape. The state is passed in: the wallet screen owns it, as it does for the other
 * special accounts.
 */
@Stable
interface PolymarketMainBlockComponent {

    fun LazyListScope.polymarketMainContent(
        state: PolymarketMainUM,
        isBalanceHidden: Boolean,
        modifier: Modifier = Modifier,
    )

    interface Factory : ComponentFactory<Unit, PolymarketMainBlockComponent>
}
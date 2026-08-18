package com.tangem.features.polymarket.main.api

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.features.polymarket.main.api.entity.PolymarketMainUM

@Stable
interface PolymarketMainBlockComponent {

    fun LazyListScope.polymarketMainContent(
        state: PolymarketMainUM,
        isBalanceHidden: Boolean,
        modifier: Modifier = Modifier,
    )

    interface Factory : ComponentFactory<Unit, PolymarketMainBlockComponent>
}
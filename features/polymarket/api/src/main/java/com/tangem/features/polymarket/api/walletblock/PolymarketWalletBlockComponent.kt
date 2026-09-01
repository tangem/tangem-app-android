package com.tangem.features.polymarket.api.walletblock

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory

@Stable
interface PolymarketWalletBlockComponent {

    fun LazyListScope.polymarketWalletBlockContent(
        state: PolymarketWalletBlockUM,
        isBalanceHidden: Boolean,
        modifier: Modifier = Modifier,
    )

    interface Factory : ComponentFactory<Unit, PolymarketWalletBlockComponent>
}
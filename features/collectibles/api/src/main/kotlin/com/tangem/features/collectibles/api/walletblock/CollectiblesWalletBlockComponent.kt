package com.tangem.features.collectibles.api.walletblock

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory

@Stable
interface CollectiblesWalletBlockComponent {

    fun LazyListScope.collectiblesWalletBlockContent(
        state: CollectiblesWalletBlockUM,
        isBalanceHidden: Boolean,
        modifier: Modifier = Modifier,
    )

    interface Factory : ComponentFactory<Unit, CollectiblesWalletBlockComponent>
}
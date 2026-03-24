package com.tangem.features.tangempay.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.features.tangempay.entity.TangemPayMainUM

@Stable
interface TangemPayMainBlockComponent {

    fun LazyListScope.tangemPayMainContent(
        state: TangemPayMainUM,
        isBalanceHidden: Boolean,
        modifier: Modifier = Modifier,
    )

    interface Factory : ComponentFactory<Unit, TangemPayMainBlockComponent>
}
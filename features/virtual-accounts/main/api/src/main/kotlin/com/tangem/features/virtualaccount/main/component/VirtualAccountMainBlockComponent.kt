package com.tangem.features.virtualaccount.main.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.features.virtualaccount.main.entity.VirtualAccountMainUM

@Stable
interface VirtualAccountMainBlockComponent {

    fun LazyListScope.virtualAccountMainContent(
        state: VirtualAccountMainUM,
        isBalanceHidden: Boolean,
        modifier: Modifier = Modifier,
    )

    interface Factory : ComponentFactory<Unit, VirtualAccountMainBlockComponent>
}
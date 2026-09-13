package com.tangem.features.jointaccount.main

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory

@Stable
interface JointAccountMainBlockComponent {

    fun LazyListScope.jointAccountMainContent(
        key: String,
        state: JointAccountMainUM,
        isBalanceHidden: Boolean,
        modifier: Modifier = Modifier,
    )

    interface Factory : ComponentFactory<Unit, JointAccountMainBlockComponent>
}
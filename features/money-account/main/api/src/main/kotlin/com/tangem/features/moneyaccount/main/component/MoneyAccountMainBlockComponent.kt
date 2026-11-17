package com.tangem.features.moneyaccount.main.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.features.moneyaccount.main.entity.MoneyAccountMainUM

@Stable
interface MoneyAccountMainBlockComponent {

    fun LazyListScope.moneyAccountMainContent(
        state: MoneyAccountMainUM,
        isBalanceHidden: Boolean,
        modifier: Modifier = Modifier,
    )

    @Composable
    fun GroupedRow(state: MoneyAccountMainUM, isBalanceHidden: Boolean, modifier: Modifier)

    interface Factory : ComponentFactory<Unit, MoneyAccountMainBlockComponent>
}
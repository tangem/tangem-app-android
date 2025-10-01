package com.tangem.features.yield.supply.impl.main.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class YieldSupplyUM {

    data class Initial(
        val title: TextReference,
        val onClick: () -> Unit,
    ) : YieldSupplyUM()

    data object Loading : YieldSupplyUM()

    data object Unavailable : YieldSupplyUM()

    data class Content(
        val rewardsBalance: TextReference,
        val rewardsApy: TextReference,
        val onClick: () -> Unit,
        val isAllowedToSpend: Boolean,
    ) : YieldSupplyUM()

    @Immutable
    sealed class Processing : YieldSupplyUM() {
        data object Enter : Processing()
        data object Exit : Processing()
    }
}
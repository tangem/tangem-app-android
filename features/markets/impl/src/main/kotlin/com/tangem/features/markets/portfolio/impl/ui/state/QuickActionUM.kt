package com.tangem.features.markets.portfolio.impl.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.markets.impl.R

@Immutable
internal sealed class QuickActionUM(
    val title: TextReference,
    val description: TextReference,
    @DrawableRes val icon: Int,
    val longClickAvailable: Boolean = false,
) {
    data object Buy : QuickActionUM(
        title = resourceReference(R.string.common_buy),
        description = resourceReference(R.string.buy_token_description),
        icon = R.drawable.ic_plus_24,
    )

    data class Exchange(
        val showBadge: Boolean,
    ) : QuickActionUM(
        title = resourceReference(R.string.common_exchange),
        description = resourceReference(R.string.exсhange_token_description),
        icon = R.drawable.ic_exchange_vertical_24,
    )

    data object Receive : QuickActionUM(
        title = resourceReference(R.string.common_receive),
        description = resourceReference(R.string.receive_token_description),
        icon = R.drawable.ic_arrow_down_24,
        longClickAvailable = true,
    )
}
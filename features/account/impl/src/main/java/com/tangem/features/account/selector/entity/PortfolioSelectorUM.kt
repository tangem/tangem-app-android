package com.tangem.features.account.selector.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

data class PortfolioSelectorUM(
    val title: TextReference,
    val items: ImmutableList<PortfolioSelectorItemUM>,
)

@Immutable
sealed interface PortfolioSelectorItemUM {
    val id: String

    data class GroupTitle(
        override val id: String,
        val name: TextReference,
    ) : PortfolioSelectorItemUM

    data class Portfolio(
        val item: UserWalletItemUM,
        val isSelected: Boolean,
    ) : PortfolioSelectorItemUM {
        override val id: String = item.id
    }
}
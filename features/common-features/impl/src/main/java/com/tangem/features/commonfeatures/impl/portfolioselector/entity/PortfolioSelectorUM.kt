package com.tangem.features.commonfeatures.impl.portfolioselector.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

data class PortfolioSelectorUM(
    val title: TextReference,
    val items: ImmutableList<PortfolioSelectorItemUM>,
    val button: PortfolioSelectorButtonUM?,
)

data class PortfolioSelectorButtonUM(
    val text: TextReference,
    val onClick: () -> Unit,
)

@Immutable
sealed interface PortfolioSelectorItemUM {
    val id: String

    data class GroupTitle(
        override val id: String,
        val name: TextReference,
        val deviceIcon: DeviceIconUM,
        val isSelected: Boolean,
        val onClick: () -> Unit,
    ) : PortfolioSelectorItemUM

    data class Portfolio(
        val item: UserWalletItemUM,
        val isSelected: Boolean,
    ) : PortfolioSelectorItemUM {
        override val id: String = item.id
    }
}
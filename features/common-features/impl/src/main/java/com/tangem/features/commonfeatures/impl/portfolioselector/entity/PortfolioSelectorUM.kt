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
    val isMultiChoiceEnabled: Boolean,
    val isSelectorV3Enabled: Boolean,
)

/**
 * Confirmation button of the multi-choice selector.
 *
 * @property isEnabled `false` dims the button and ignores clicks. Applying an empty selection would
 * leave the consumer with no portfolio at all, so the button stays disabled until at least one
 * account is picked.
 */
data class PortfolioSelectorButtonUM(
    val text: TextReference,
    val onClick: () -> Unit,
    val isEnabled: Boolean = true,
)

@Immutable
sealed interface PortfolioSelectorItemUM {
    val id: String
    val groupPosition: GroupPosition

    data class GroupTitle(
        override val id: String,
        val name: TextReference,
        val deviceIcon: DeviceIconUM,
        val isSelected: Boolean,
        val onClick: (() -> Unit)?,
        override val groupPosition: GroupPosition = GroupPosition.Default,
    ) : PortfolioSelectorItemUM

    data class Portfolio(
        val item: UserWalletItemUM,
        val isSelected: Boolean,
        override val groupPosition: GroupPosition = GroupPosition.Default,
    ) : PortfolioSelectorItemUM {
        override val id: String = item.id
    }
}

/**
 * Position of an item within its wallet group. Used to draw rounded-corner decorations and group
 * spacing without recomputing group boundaries in Compose.
 */
data class GroupPosition(
    val indexInGroup: Int,
    val lastIndexInGroup: Int,
    val isGroupStart: Boolean,
) {
    companion object {
        val Default = GroupPosition(indexInGroup = 0, lastIndexInGroup = 0, isGroupStart = true)
    }
}
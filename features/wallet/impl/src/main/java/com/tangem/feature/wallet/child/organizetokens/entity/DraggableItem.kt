package com.tangem.feature.wallet.child.organizetokens.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.feature.wallet.impl.R

/**
 * Helper class for the DND list items
 *
 * @property id ID of the item
 * @property roundingModeUM item [RoundingModeUM]
 * @property showShadow if true then item should be elevated
 * */
@Immutable
internal sealed class DraggableItem {
    abstract val id: Any
    abstract val roundingModeUM: RoundingModeUM
    abstract val showShadow: Boolean

    /**
     * Item for network group header.
     *
     * @property id ID of the network group
     * @property networkName network group name
     * @property accountId account id
     * @property roundingModeUM item [RoundingModeUM]
     * @property showShadow if true then item should be elevated
     * */
    data class GroupHeader(
        override val id: Int,
        val networkName: String,
        val accountId: String = "",
        override val roundingModeUM: RoundingModeUM = RoundingModeUM.None,
        override val showShadow: Boolean = false,
    ) : DraggableItem() {

        val groupTitle = TokensListItemUM.GroupTitle(
            id = id,
            text = resourceReference(
                id = R.string.wallet_network_group_title,
                formatArgs = wrappedList(networkName),
            ),
        )
    }

    /**
     * Item for token.
     *
     * @property tokenItemState state of the token item
     * @property groupId ID of the network group which contains this token
     * @property accountId account id
     * @property id ID of the token
     * @property roundingModeUM item [RoundingModeUM]
     * @property showShadow if true then item should be elevated
     * */
    data class Token(
        val tokenItemState: TokenItemState.Draggable,
        val groupId: Int,
        val accountId: String = "",
        override val showShadow: Boolean = false,
        override val roundingModeUM: RoundingModeUM = RoundingModeUM.None,
    ) : DraggableItem() {
        override val id: String = tokenItemState.id
    }

    /**
     * Helper item used to detect possible positions where a draggable item can be placed.
     *
     * @property id ID of the placeholder
     * @property accountId account id
     * */
    data class Placeholder(
        override val id: String,
        val accountId: String = "",
    ) : DraggableItem() {
        override val showShadow: Boolean = false
        override val roundingModeUM: RoundingModeUM = RoundingModeUM.None
    }

    /**
     * Item for portfolio.
     *
     * @property tokenItemState state of the portfolio item
     * @property id ID of the portfolio
     * @property roundingModeUM item [RoundingModeUM]
     * @property showShadow if true then item should be elevated
     * */
    data class Portfolio(
        override val roundingModeUM: RoundingModeUM = RoundingModeUM.None,
        val tokenItemState: TokenItemState,
    ) : DraggableItem() {
        override val id: String = tokenItemState.id
        override val showShadow: Boolean = false
    }

    /**
     * Update item [RoundingModeUM]
     *
     * @param mode new [RoundingModeUM]
     *
     * @return updated [DraggableItem]
     * */
    fun updateRoundingMode(mode: RoundingModeUM): DraggableItem = when (this) {
        is Placeholder -> this
        is Portfolio -> this.copy(roundingModeUM = mode)
        is GroupHeader -> this.copy(roundingModeUM = mode)
        is Token -> this.copy(roundingModeUM = mode)
    }

    /**
     * Update item shadow visibility
     *
     * @param show if true then item should be elevated
     *
     * @return updated [DraggableItem]
     * */
    fun updateShadowVisibility(show: Boolean): DraggableItem = when (this) {
        is Portfolio,
        is Placeholder,
        -> this
        is GroupHeader -> this.copy(showShadow = show)
        is Token -> this.copy(showShadow = show)
    }
}
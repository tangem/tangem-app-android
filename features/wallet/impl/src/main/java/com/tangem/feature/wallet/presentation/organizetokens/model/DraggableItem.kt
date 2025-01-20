package com.tangem.feature.wallet.presentation.organizetokens.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem.RoundingMode

/**
 * Helper class for the DND list items
 *
 * @property id ID of the item
 * @property roundingMode item [RoundingMode]
 * @property showShadow if true then item should be elevated
 * */
@Immutable
internal sealed class DraggableItem {
    abstract val id: Any
    abstract val roundingMode: RoundingMode
    abstract val showShadow: Boolean

    /**
     * Item for network group header.
     *
     * @property id ID of the network group
     * @property networkName network group name
     * @property roundingMode item [RoundingMode]
     * @property showShadow if true then item should be elevated
     * */
    data class GroupHeader(
        override val id: Int,
        val networkName: String,
        override val roundingMode: RoundingMode = RoundingMode.None,
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
     * @property id ID of the token
     * @property roundingMode item [RoundingMode]
     * @property showShadow if true then item should be elevated
     * */
    data class Token(
        val tokenItemState: TokenItemState.Draggable,
        val groupId: Int,
        override val showShadow: Boolean = false,
        override val roundingMode: RoundingMode = RoundingMode.None,
    ) : DraggableItem() {
        override val id: String = tokenItemState.id
    }

    /**
     * Helper item used to detect possible positions where a draggable item can be placed.
     *
     * @property id ID of the placeholder
     * */
    data class Placeholder(
        override val id: String,
    ) : DraggableItem() {
        override val showShadow: Boolean = false
        override val roundingMode: RoundingMode = RoundingMode.None
    }

    /**
     * Rounding mode of the [DraggableItem]
     *
     * @property showGap if true then item should have padding on rounded side
     * */
    @Immutable
    sealed class RoundingMode {
        abstract val showGap: Boolean

        /**
         * In this mode, item is not rounded
         * */
        object None : RoundingMode() {
            override val showGap: Boolean = false
        }

        /**
         * In this mode, item should have a rounded top side
         *
         * @property showGap if true then item should have top padding
         * */
        data class Top(override val showGap: Boolean = false) : RoundingMode()

        /**
         * In this mode, item should have a rounded bottom side
         *
         * @property showGap if true then item should have bottom padding
         * */
        data class Bottom(override val showGap: Boolean = false) : RoundingMode()

        /**
         * In this mode, item should have a rounded all sides
         *
         * @property showGap if true then item should have top and bottom padding
         * */
        data class All(override val showGap: Boolean = false) : RoundingMode()
    }

    /**
     * Update item [RoundingMode]
     *
     * @param mode new [RoundingMode]
     *
     * @return updated [DraggableItem]
     * */
    fun updateRoundingMode(mode: RoundingMode): DraggableItem = when (this) {
        is Placeholder -> this
        is GroupHeader -> this.copy(roundingMode = mode)
        is Token -> this.copy(roundingMode = mode)
    }

    /**
     * Update item shadow visibility
     *
     * @param show if true then item should be elevated
     *
     * @return updated [DraggableItem]
     * */
    fun updateShadowVisibility(show: Boolean): DraggableItem = when (this) {
        is Placeholder -> this
        is GroupHeader -> this.copy(showShadow = show)
        is Token -> this.copy(showShadow = show)
    }
}
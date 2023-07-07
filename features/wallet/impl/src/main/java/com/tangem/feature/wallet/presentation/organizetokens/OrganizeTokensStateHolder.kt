package com.tangem.feature.wallet.presentation.organizetokens

import androidx.compose.runtime.Immutable
import com.tangem.feature.wallet.presentation.common.state.TokenItemState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import org.burnoutcrew.reorderable.ItemPosition

internal data class OrganizeTokensStateHolder(
    val header: HeaderConfig,
    val itemsState: OrganizeTokensListState,
    val dragConfig: DragConfig,
    val actions: ActionsConfig,
) {

    data class HeaderConfig(
        val onSortByBalanceClick: () -> Unit,
        val onGroupByNetworkClick: () -> Unit,
    )

    data class ActionsConfig(
        val onApplyClick: () -> Unit,
        val onCancelClick: () -> Unit,
    )

    data class DragConfig(
        val onItemDragged: (from: ItemPosition, to: ItemPosition) -> Unit,
        val canDragItemOver: (dragOver: ItemPosition, dragging: ItemPosition) -> Boolean,
        val onItemDragEnd: () -> Unit,
        val onDragStart: (item: DraggableItem) -> Unit,
    )
}

@Immutable
internal sealed interface OrganizeTokensListState {
    val items: PersistentList<DraggableItem>

    data class GroupedByNetwork(
        override val items: PersistentList<DraggableItem>,
    ) : OrganizeTokensListState

    data class Ungrouped(
        override val items: PersistentList<DraggableItem.Token>,
    ) : OrganizeTokensListState

    @Suppress("UNCHECKED_CAST")
    fun updateItems(update: (PersistentList<DraggableItem>) -> List<DraggableItem>): OrganizeTokensListState {
        val updatedItems = update(this.items).toPersistentList()

        return when (this) {
            is GroupedByNetwork -> this.copy(items = updatedItems)
            is Ungrouped -> this.copy(items = updatedItems as PersistentList<DraggableItem.Token>)
        }
    }
}

/**
 * Helper class for the DND list items
 *
 * @property id ID of the item
 * @property roundingMode item [RoundingMode]
 * @property showShadow if true then item should be elevated
 * */
@Immutable
internal sealed interface DraggableItem {
    val id: String
    val roundingMode: RoundingMode
    val showShadow: Boolean

    /**
     * Item for network group header.
     *
     * @property id ID of the network group
     * @property networkName network group name
     * @property roundingMode item [RoundingMode]
     * @property showShadow if true then item should be elevated
     * */
    data class GroupHeader(
        override val id: String,
        val networkName: String,
        override val roundingMode: RoundingMode = RoundingMode.None,
        override val showShadow: Boolean = false,
    ) : DraggableItem

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
        val groupId: String,
        override val showShadow: Boolean = false,
        override val roundingMode: RoundingMode = RoundingMode.None,
    ) : DraggableItem {
        override val id: String = tokenItemState.id
    }

    /**
     * Helper item used to detect possible positions where a network group can be placed.
     * Used only on [OrganizeTokensListState.GroupedByNetwork] and placed between network groups.
     *
     * @property id ID of the placeholder
     * */
    data class GroupPlaceholder(
        override val id: String,
    ) : DraggableItem {
        override val showShadow: Boolean = false
        override val roundingMode: RoundingMode = RoundingMode.None
    }

    /**
     * Update item [RoundingMode]
     *
     * @param mode new [RoundingMode]
     *
     * @return updated [DraggableItem]
     * */
    fun roundingMode(mode: RoundingMode): DraggableItem = when (this) {
        is GroupPlaceholder -> this
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
    fun showShadow(show: Boolean): DraggableItem = when (this) {
        is GroupPlaceholder -> this
        is GroupHeader -> this.copy(showShadow = show)
        is Token -> this.copy(showShadow = show)
    }

    /**
     * Rounding mode of the [DraggableItem]
     *
     * @property showGap if true then item should have padding on rounded side
     * */
    @Immutable
    sealed interface RoundingMode {
        val showGap: Boolean

        /**
         * In this mode, item is not rounded
         * */
        object None : RoundingMode {
            override val showGap: Boolean = false
        }

        /**
         * In this mode, item should have a rounded top side
         *
         * @property showGap if true then item should have top padding
         * */
        data class Top(override val showGap: Boolean = false) : RoundingMode

        /**
         * In this mode, item should have a rounded bottom side
         *
         * @property showGap if true then item should have bottom padding
         * */
        data class Bottom(override val showGap: Boolean = false) : RoundingMode

        /**
         * In this mode, item should have a rounded all sides
         *
         * @property showGap if true then item should have top and bottom padding
         * */
        data class All(override val showGap: Boolean = false) : RoundingMode
    }
}

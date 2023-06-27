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
 * */
@Immutable
internal sealed interface DraggableItem {
    val id: String

    /**
     * Item for network group header.
     *
     * @property id ID of the network group
     * @property networkName network group name
     * */
    data class GroupHeader(
        override val id: String,
        val networkName: String,
    ) : DraggableItem

    /**
     * Item for token.
     *
     * @property tokenItemState state of the token item
     * @property groupId ID of the network group which contains this token
     * @property id ID of the token
     * */
    data class Token(
        val tokenItemState: TokenItemState.Draggable,
        val groupId: String,
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
    ) : DraggableItem
}
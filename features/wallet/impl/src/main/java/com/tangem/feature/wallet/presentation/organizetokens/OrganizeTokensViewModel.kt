package com.tangem.feature.wallet.presentation.organizetokens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tangem.feature.wallet.presentation.common.WalletPreviewData
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensStateHolder.DragConfig
import com.tangem.feature.wallet.presentation.organizetokens.OrganizeTokensStateHolder.HeaderConfig
import com.tangem.feature.wallet.presentation.organizetokens.utils.checkCanMoveHeaderOver
import com.tangem.feature.wallet.presentation.organizetokens.utils.checkCanMoveTokenOver
import com.tangem.feature.wallet.presentation.organizetokens.utils.findItemsToMove
import com.tangem.feature.wallet.presentation.organizetokens.utils.moveItem
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import org.burnoutcrew.reorderable.ItemPosition
import javax.inject.Inject
import kotlin.properties.Delegates

// FIXME: Implemented with preview data
@HiltViewModel
internal class OrganizeTokensViewModel @Inject constructor() : ViewModel() {

    // TODO: Move to domain
    @Volatile
    private var groupIdToTokens: Map<String, List<DraggableItem.Token>>? = null

    var router: InnerWalletRouter by Delegates.notNull()

    var uiState: OrganizeTokensStateHolder by mutableStateOf(getInitialState())
        private set

    private fun getInitialState(): OrganizeTokensStateHolder = WalletPreviewData.organizeTokensState.copy(
        itemsState = OrganizeTokensListState.Ungrouped(
            items = WalletPreviewData.draggableTokens,
        ),
        dragConfig = DragConfig(
            onItemDragged = this::moveItem,
            canDragItemOver = this::checkCanMoveItemOver,
            onItemDragEnd = this::expandGroups,
            onDragStart = this::collapseGroup,
        ),
        header = HeaderConfig(
            onSortByBalanceClick = { /* no-op */ },
            onGroupByNetworkClick = this::toggleTokensByNetworkGrouping,
        ),
    )

    private fun toggleTokensByNetworkGrouping() {
        val newListState = when (val itemsState = uiState.itemsState) {
            is OrganizeTokensListState.GroupedByNetwork -> OrganizeTokensListState.Ungrouped(
                items = itemsState.items.filterIsInstance<DraggableItem.Token>().toPersistentList(),
            )
            is OrganizeTokensListState.Ungrouped -> OrganizeTokensListState.GroupedByNetwork(
                items = WalletPreviewData.draggableItems,
            )
        }

        uiState = uiState.copy(itemsState = newListState)
    }

    private fun checkCanMoveItemOver(moveOverItemPosition: ItemPosition, movedItemPosition: ItemPosition): Boolean {
        val items = (uiState.itemsState as? OrganizeTokensListState.GroupedByNetwork)
            ?.items
            ?: return true // If ungrouped then item can be moved anywhere

        val (moveOverItem, movedItem) = items.findItemsToMove(moveOverItemPosition.key, movedItemPosition.key)

        if (moveOverItem == null || movedItem == null) {
            return false
        }

        return when (movedItem) {
            is DraggableItem.GroupHeader -> checkCanMoveHeaderOver(moveOverItemPosition, moveOverItem, items.lastIndex)
            is DraggableItem.Token -> checkCanMoveTokenOver(movedItem, moveOverItem)
            is DraggableItem.GroupPlaceholder -> false
        }
    }

    private fun collapseGroup(item: DraggableItem) {
        if (!groupIdToTokens.isNullOrEmpty() || item is DraggableItem.Token) return

        val itemsState = uiState.itemsState as? OrganizeTokensListState.GroupedByNetwork ?: return
        groupIdToTokens = itemsState.items
            .asSequence()
            .filterIsInstance<DraggableItem.Token>()
            .groupBy(DraggableItem.Token::groupId)

        uiState = uiState.copy(
            itemsState = itemsState.updateItems { items ->
                items.filterNot { it is DraggableItem.Token && it.groupId == item.id }
            },
        )
    }

    private fun expandGroups() {
        if (groupIdToTokens.isNullOrEmpty()) return

        val itemsState = uiState.itemsState as? OrganizeTokensListState.GroupedByNetwork ?: return
        val currentGroups = itemsState.items.filterIsInstance<DraggableItem.GroupHeader>()
        val newItems = currentGroups
            .flatMapIndexed { index, group ->
                mutableListOf<DraggableItem>(group)
                    .also { it.addAll(groupIdToTokens?.get(group.id).orEmpty()) }
                    .also {
                        if (index != currentGroups.lastIndex) {
                            it.add(DraggableItem.GroupPlaceholder(id = "group_divider_$index"))
                        }
                    }
            }

        uiState = uiState.copy(
            itemsState = itemsState.updateItems { newItems },
        )

        groupIdToTokens = null
    }

    private fun moveItem(from: ItemPosition, to: ItemPosition) {
        uiState = uiState.copy(
            itemsState = uiState.itemsState.updateItems {
                it.moveItem(from.index, to.index)
            },
        )
    }
}
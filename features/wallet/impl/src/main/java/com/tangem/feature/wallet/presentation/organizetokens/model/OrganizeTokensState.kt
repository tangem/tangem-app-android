package com.tangem.feature.wallet.presentation.organizetokens.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.event.StateEvent
import org.burnoutcrew.reorderable.ItemPosition

@Immutable
internal data class OrganizeTokensState(
    val onBackClick: () -> Unit,
    val itemsState: OrganizeTokensListState,
    val header: HeaderConfig,
    val actions: ActionsConfig,
    val dndConfig: DragAndDropConfig,
    val scrollListToTop: StateEvent<Unit>,
    val isBalanceHidden: Boolean,
) {

    data class HeaderConfig(
        val isEnabled: Boolean = false,
        val isSortedByBalance: Boolean = false,
        val isGrouped: Boolean = false,
        val onSortClick: () -> Unit,
        val onGroupClick: () -> Unit,
    )

    data class ActionsConfig(
        val canApply: Boolean = false,
        val showApplyProgress: Boolean = false,
        val onApplyClick: () -> Unit,
        val onCancelClick: () -> Unit,
    )

    data class DragAndDropConfig(
        val onItemDragged: (ItemPosition, ItemPosition) -> Unit,
        val canDragItemOver: (ItemPosition, ItemPosition) -> Boolean,
        val onItemDragEnd: () -> Unit,
        val onItemDragStart: (DraggableItem) -> Unit,
    )
}
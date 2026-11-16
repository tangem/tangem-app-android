package com.tangem.feature.wallet.child.organizetokens.model.dnd

import com.google.common.truth.Truth.assertThat
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeRowItemUM
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensUM
import com.tangem.feature.wallet.child.organizetokens.ui.preview.OrganizeTokensPreview
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Test

internal class DragAndDropAdapterTest {

    @Test
    fun `GIVEN token list WHEN item moved down THEN dragged operation carries the moved list`() {
        // Arrange
        val items = OrganizeTokensPreview.defaultState.tokenList.take(4)
        val adapter = createAdapter(items)

        // Act
        adapter.onItemMoved(from = 1, to = 3)

        // Assert
        val expected = DragAndDropAdapter.DragOperation(
            type = DragAndDropAdapter.DragOperation.Type.Dragged,
            tokenList = listOf(items[0], items[2], items[3], items[1]).toPersistentList(),
        )
        assertThat(adapter.dragAndDropUpdates.value).isEqualTo(expected)
    }

    @Test
    fun `GIVEN token list WHEN item moved up THEN dragged operation carries the moved list`() {
        // Arrange
        val items = OrganizeTokensPreview.defaultState.tokenList.take(4)
        val adapter = createAdapter(items)

        // Act
        adapter.onItemMoved(from = 3, to = 1)

        // Assert
        val expected = DragAndDropAdapter.DragOperation(
            type = DragAndDropAdapter.DragOperation.Type.Dragged,
            tokenList = listOf(items[0], items[3], items[1], items[2]).toPersistentList(),
        )
        assertThat(adapter.dragAndDropUpdates.value).isEqualTo(expected)
    }

    private fun createAdapter(items: List<OrganizeRowItemUM>): DragAndDropAdapter {
        val state: OrganizeTokensUM = OrganizeTokensPreview.defaultState.copy(tokenList = items.toPersistentList())
        return DragAndDropAdapter(MutableStateFlow(state))
    }
}
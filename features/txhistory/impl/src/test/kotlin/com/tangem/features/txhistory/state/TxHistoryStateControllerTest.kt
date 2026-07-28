package com.tangem.features.txhistory.state

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TxHistoryStateControllerTest {

    private val controller = TxHistoryStateController()

    @Test
    fun `GIVEN empty items snapshot WHEN setContent THEN Empty state with explorer action`() {
        val onExploreClick = {}

        controller.setContent(
            snapshot = TxHistoryItemsSnapshot.Items(persistentListOf()),
            loadMore = { true },
            onExploreClick = onExploreClick,
        )

        val state = controller.uiState.value
        assertThat(state).isInstanceOf(TxHistoryItemsUM.Empty::class.java)
        assertThat((state as TxHistoryItemsUM.Empty).onExploreClick).isEqualTo(onExploreClick)
    }

    @Test
    fun `GIVEN snapshot with only a group title WHEN setContent THEN Empty state`() {
        controller.setContent(
            snapshot = TxHistoryItemsSnapshot.Items(
                persistentListOf(
                    TxHistoryItemsUM.TxHistoryItemUM.GroupTitle(title = "Today", itemKey = "0-Today"),
                ),
            ),
            loadMore = { true },
            onExploreClick = {},
        )

        assertThat(controller.uiState.value).isInstanceOf(TxHistoryItemsUM.Empty::class.java)
    }

    @Test
    fun `GIVEN snapshot with transactions WHEN setContent THEN Content state`() {
        controller.setContent(
            snapshot = TxHistoryItemsSnapshot.Items(
                persistentListOf(
                    TxHistoryItemsUM.TxHistoryItemUM.GroupTitle(title = "Today", itemKey = "0-Today"),
                    TxHistoryItemsUM.TxHistoryItemUM.Transaction(TransactionItemUM.Loading("hash")),
                ),
            ),
            loadMore = { true },
            onExploreClick = {},
        )

        assertThat(controller.uiState.value).isInstanceOf(TxHistoryItemsUM.Content::class.java)
    }

    @Test
    fun `GIVEN Empty state WHEN empty snapshot arrives THEN Empty is not overridden by Content`() {
        controller.setEmpty(onExploreClick = {})

        controller.setContent(
            snapshot = TxHistoryItemsSnapshot.Items(persistentListOf()),
            loadMore = { true },
            onExploreClick = {},
        )

        assertThat(controller.uiState.value).isInstanceOf(TxHistoryItemsUM.Empty::class.java)
    }
}
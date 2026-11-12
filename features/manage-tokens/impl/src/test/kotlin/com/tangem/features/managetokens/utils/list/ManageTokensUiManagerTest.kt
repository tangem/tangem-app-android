package com.tangem.features.managetokens.utils.list

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.features.managetokens.component.ManageTokensMode
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.pagination.Batch
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class ManageTokensUiManagerTest {

    @Test
    fun `GIVEN a coin returned in two batches WHEN items are built THEN it is listed once`() = runTest {
        // Arrange
        val state = MutableStateFlow(
            ManageTokensListState(
                mode = ManageTokensMode.None,
                uiBatches = listOf(
                    Batch(key = 0, data = listOf(item("bitcoin"), item("zenith-protocol"))),
                    Batch(key = 1, data = listOf(item("zenith-protocol"), item("ethereum"))),
                ),
            ),
        )
        val manager = ManageTokensUiManager(
            state = state,
            dispatchers = TestingCoroutineDispatcherProvider(),
            scope = backgroundScope,
            actions = mockk(relaxed = true),
            manageTokensWarningDelegate = mockk(relaxed = true),
        )

        // Act
        val items = manager.items.first()

        // Assert
        assertThat(items.map { it.id.value }).containsExactly("bitcoin", "zenith-protocol", "ethereum").inOrder()
    }

    private fun item(id: String) = CurrencyItemUM.Custom(
        id = ManagedCryptoCurrency.ID(value = id),
        name = id,
        symbol = id.uppercase(),
        icon = CurrencyIconState.Loading,
        onRemoveClick = {},
    )
}
package com.tangem.features.tangempay.txhistory

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayTxHistoryItem
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchFlow
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class TangemPayTxHistoryModelTest {

    private val repository: TangemPayTxHistoryRepository = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()

    @BeforeEach
    fun setup() {
        clearMocks(repository, getBalanceHidingSettingsUseCase)
        every { getBalanceHidingSettingsUseCase() } returns flowOf(
            BalanceHidingSettings(
                isHidingEnabledInSettings = false,
                isBalanceHidden = false,
                isBalanceHidingNotificationEnabled = false,
            ),
        )
        every { repository.getTxHistoryBatchFlow(any(), any(), any()) } returns emptyHistoryBatchFlow()
    }

    @Test
    fun `GIVEN empty history WHEN model created THEN empty state shown`() = runTest {
        // Arrange
        val model = createModel(testScope = this)

        // Act
        advanceUntilIdle()

        // Assert
        val expected = TangemPayTxHistoryUM.Empty(isBalanceHidden = false)
        assertThat(model.uiState.value).isEqualTo(expected)
        model.onDestroy()
    }

    private fun emptyHistoryBatchFlow(): TangemPayTxHistoryListBatchFlow = mockk {
        every { state } returns MutableStateFlow(
            BatchListState<Int, List<TangemPayTxHistoryItem>>(
                data = emptyList(),
                status = PaginationStatus.EndOfPagination,
            ),
        )
    }

    private fun createModel(testScope: TestScope): TangemPayTxHistoryModel {
        val params = DefaultTangemPayTxHistoryComponent.Params(
            userWalletId = UserWalletId("123"),
            uiActions = mockk(relaxed = true),
        )
        return TangemPayTxHistoryModel(
            paramsContainer = MutableParamsContainer(params),
            tangemPayTxHistoryRepository = repository,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            txHistoryUpdateListener = TangemPayTxHistoryUpdateListener(),
        )
    }
}
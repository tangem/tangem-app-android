package com.tangem.features.polymarket.impl.history.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivityFilter
import com.tangem.domain.polymarket.model.PolymarketActivityState
import com.tangem.domain.polymarket.session.PolymarketActivitySession
import com.tangem.domain.polymarket.usecase.CreatePolymarketActivitySessionUseCase
import com.tangem.features.polymarket.impl.history.PolymarketHistoryComponent
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketHistoryUM.Content
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketHistoryUM.NextPage
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketHistoryModelTest {

    private val router: Router = mockk(relaxed = true)
    private val createSession: CreatePolymarketActivitySessionUseCase = mockk()
    private val session: PolymarketActivitySession = mockk(relaxed = true)
    private val state = MutableStateFlow(PolymarketActivityState())
    private val wallet = UserWalletId("011")
    private var model: PolymarketHistoryModel? = null

    @AfterEach
    fun destroyModel() {
        model?.onDestroy()
    }

    @Test
    fun `GIVEN the selected wallet WHEN opening and leaving THEN controls its history session`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.onResume()
        model.onPause()
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) { createSession(wallet) }
        verify(exactly = 1) { session.resume(any()) }
        verify(exactly = 1) { session.pause() }
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN an empty filtered history WHEN shown THEN keeps filters available and displays no results`() = runTest {
        // Arrange
        val model = createModel()
        val filter = PolymarketActivityFilter(PolymarketActivity.Type.BUY)
        state.value = PolymarketActivityState(filter = filter, status = PaginationStatus.EndOfPagination)

        // Act
        runCurrent()
        val tabs = model.uiState.value.tabs.filterIsInstance<TangemTabItemUM.Content>()
        tabs.first { it.id == "ALL" }.onClick()

        // Assert
        assertThat(model.uiState.value.content).isEqualTo(Content.NoResults)
        assertThat(tabs.single { it.isSelected }.id).isEqualTo("BUY")
        verify(exactly = 1) { session.selectFilter(PolymarketActivityFilter()) }
    }

    @Test
    fun `GIVEN a refresh failure with cached rows WHEN shown THEN preserves rows and wires retry and refresh`() = runTest {
        // Arrange
        val model = createModel()
        state.value = PolymarketActivityState(
            items = listOf(createHistoryActivity()),
            status = PaginationStatus.InitialLoadingError(IllegalStateException("offline")),
        )

        // Act
        runCurrent()
        val content = model.uiState.value.content as Content.Data
        (content.nextPage as NextPage.Error).onRetry()
        model.uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())

        // Assert
        assertThat(content.groups.flatMap { it.activities }.map { it.id }).containsExactly("activity")
        verify(exactly = 1) { session.retry() }
        verify(exactly = 1) { session.refresh() }
    }

    @Test
    fun `GIVEN loaded history WHEN search and CSV are tapped THEN placeholders do not change state or load data`() = runTest {
        // Arrange
        val model = createModel()
        state.value = PolymarketActivityState(listOf(createHistoryActivity()), status = PaginationStatus.EndOfPagination)
        runCurrent()
        val before = model.uiState.value
        val content = before.content as Content.Data

        // Act
        content.search.onQueryChange("event")
        content.search.onActiveChange(true)
        content.onDownloadCsvClick()
        runCurrent()

        // Assert
        assertThat(model.uiState.value).isSameInstanceAs(before)
        verify(exactly = 0) { session.loadMore() }
        verify(exactly = 0) { session.refresh() }
    }

    private fun TestScope.createModel(): PolymarketHistoryModel {
        every { createSession(wallet) } returns session
        every { session.state } returns state
        return PolymarketHistoryModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            router = router,
            paramsContainer = MutableParamsContainer(PolymarketHistoryComponent.Params(wallet)),
            createSession = createSession,
        ).also { model = it }
    }

}
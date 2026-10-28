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
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.pagination.PaginationStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketHistoryModelTest {

    private val exporter: PolymarketActivityExport = mockk()
    private val messageSender: com.tangem.core.decompose.ui.UiMessageSender = mockk(relaxed = true)
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
    fun `GIVEN loaded history WHEN search is tapped THEN the placeholder does not change state or load data`() = runTest {
        // Arrange
        val model = createModel()
        state.value = PolymarketActivityState(listOf(createHistoryActivity()), status = PaginationStatus.EndOfPagination)
        runCurrent()
        val before = model.uiState.value
        val content = before.content as Content.Data

        // Act
        content.search.onQueryChange("event")
        content.search.onActiveChange(true)
        runCurrent()

        // Assert
        assertThat(model.uiState.value).isSameInstanceAs(before)
        verify(exactly = 0) { session.loadMore() }
        verify(exactly = 0) { session.refresh() }
    }

    @Test
    fun `GIVEN open details WHEN history refreshes THEN retains details and navigates to the same outcome`() = runTest {
        // Arrange
        val model = createModel()
        state.value = PolymarketActivityState(listOf(historyTrade()), status = PaginationStatus.EndOfPagination)
        runCurrent()

        // Act
        val content = model.uiState.value.content as Content.Data
        requireNotNull(content.groups.single().activities.single().onClick).invoke()
        val details = requireNotNull(model.uiState.value.details)
        state.value = state.value.copy(isRefreshing = true)
        runCurrent()

        // Assert
        assertThat(model.uiState.value.details).isSameInstanceAs(details)
        requireNotNull(details.onGoToPrediction).invoke()
        assertThat(model.uiState.value.details).isNull()
        verify(exactly = 1) {
            router.push(PolymarketRoute.EventDetails("event", wallet, "market", "asset"))
        }
    }

    @Test
    fun `GIVEN a loss without event metadata WHEN opening details THEN hides amount and navigation`() = runTest {
        // Arrange
        val model = createModel()
        val activity = createHistoryActivity(type = PolymarketActivity.Type.REDEEM)
            .copy(redeemResult = PolymarketActivity.RedeemResult.LOST)
        state.value = PolymarketActivityState(listOf(activity), status = PaginationStatus.EndOfPagination)
        runCurrent()

        // Act
        val content = model.uiState.value.content as Content.Data
        requireNotNull(content.groups.single().activities.single().onClick).invoke()

        val details = requireNotNull(model.uiState.value.details)
        // Assert
        assertThat(details.amount).isNull()
        assertThat(details.onGoToPrediction).isNull()
        model.uiState.value.onDismissDetails()
        assertThat(model.uiState.value.details).isNull()
    }

    @Test
    fun `GIVEN an export in flight WHEN tapping twice and switching tabs THEN exports the original filter once`() = runTest {
        // Arrange
        val pendingFile = CompletableDeferred<File>()
        coEvery { exporter(wallet, PolymarketActivityFilter()) } coAnswers { pendingFile.await() }
        val model = createModel()
        state.value = PolymarketActivityState(listOf(historyTrade()), status = PaginationStatus.EndOfPagination)
        runCurrent()
        val content = model.uiState.value.content as Content.Data
        val received = async { model.exports.first() }

        // Act
        content.onDownloadCsvClick()
        content.onDownloadCsvClick()
        state.value = state.value.copy(filter = PolymarketActivityFilter(PolymarketActivity.Type.SELL))
        runCurrent()
        assertThat(model.uiState.value.isExporting).isTrue()
        val file = File("predictions-history.csv")
        pendingFile.complete(file)
        runCurrent()

        // Assert
        assertThat(received.await()).isEqualTo(file)
        assertThat(model.uiState.value.isExporting).isFalse()
        coVerify(exactly = 1) { exporter(wallet, PolymarketActivityFilter()) }
        coVerify(exactly = 1) { exporter(any(), any()) }
    }

    @Test
    fun `GIVEN an export failure WHEN downloading THEN clears loading and offers an error message`() = runTest {
        // Arrange
        coEvery { exporter(any(), any()) } throws IllegalStateException("Failed page")
        val model = createModel()
        state.value = PolymarketActivityState(listOf(historyTrade()), status = PaginationStatus.EndOfPagination)
        runCurrent()

        // Act
        (model.uiState.value.content as Content.Data).onDownloadCsvClick()
        runCurrent()

        // Assert
        assertThat(model.uiState.value.isExporting).isFalse()
        verify(exactly = 1) { messageSender.send(any()) }
    }

    private fun TestScope.createModel(): PolymarketHistoryModel {
        every { createSession(wallet) } returns session
        every { session.state } returns state
        return PolymarketHistoryModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            router = router,
            paramsContainer = MutableParamsContainer(PolymarketHistoryComponent.Params(wallet)),
            createSession = createSession,
            exportActivity = exporter,
            messageSender = messageSender,
        ).also { model = it }
    }

}
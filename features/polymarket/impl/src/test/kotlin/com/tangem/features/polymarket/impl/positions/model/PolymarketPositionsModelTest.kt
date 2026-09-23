package com.tangem.features.polymarket.impl.positions.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.model.PolymarketPositionsFilter
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionResult
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.positions.PolymarketPositionsComponent
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.Content
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.FilteredContent
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.Tab
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketPositionsModelTest {

    private val router: Router = mockk(relaxed = true)
    private val loader: PolymarketPositionsLoader = mockk()
    private val active: PolymarketPositionsLoader.Feed = mockk(relaxed = true)
    private val finished: PolymarketPositionsLoader.Feed = mockk(relaxed = true)
    private val activeContent = Content.Data(persistentListOf(PolymarketPositionUMConverter().convert(createPolymarketPosition())))
    private val activeState = MutableStateFlow(FilteredContent(content = activeContent))
    private val finishedState = MutableStateFlow(FilteredContent(content = Content.Empty))
    private val wallet = UserWalletId("022")
    private var model: PolymarketPositionsModel? = null
    private val positionClick = slot<(PolymarketPosition.Open) -> Unit>()
    private val finishedClick = slot<(PolymarketPosition.Closed) -> Unit>()

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
    }

    @Test
    fun `GIVEN Finished route WHEN resumed THEN uses the selected wallet and loads only Finished`() = runTest {
        // Arrange
        val model = createModel(PolymarketPositionsTab.FINISHED)

        // Act
        model.onResume()
        runCurrent()

        // Assert
        assertThat(model.uiState.value.selectedTab).isEqualTo(Tab.Finished)
        assertThat(model.uiState.value.content).isEqualTo(Content.Empty)
        verify(exactly = 1) { loader.createFeed(wallet, PolymarketPositionsTab.FINISHED, null, any()) }
        verify(exactly = 1) { finished.resume(any()) }
        verify(exactly = 0) { active.resume(any()) }
    }

    @Test
    fun `GIVEN active list WHEN switching tabs and returning THEN creates a fresh feed`() = runTest {
        // Arrange
        val model = createModel()
        model.onResume()
        runCurrent()
        val newActive: PolymarketPositionsLoader.Feed = mockk(relaxed = true)
        every { newActive.state } returns MutableStateFlow(FilteredContent())
        every { loader.createFeed(wallet, PolymarketPositionsTab.ACTIVE, any()) } returns newActive

        // Act
        model.uiState.value.onFilterSelect(PolymarketPositionsFilter.WON)
        model.uiState.value.onTabSelect(Tab.Finished)
        runCurrent()
        val history = model.uiState.value.content
        val finishedFilter = model.uiState.value.selectedFilter
        model.uiState.value.onFilterSelect(PolymarketPositionsFilter.LOST)
        model.uiState.value.onTabSelect(Tab.Active)
        runCurrent()

        // Assert
        assertThat(history).isEqualTo(Content.Empty)
        assertThat(model.uiState.value.content).isEqualTo(Content.Loading)
        assertThat(finishedFilter).isEqualTo(PolymarketPositionsFilter.ALL)
        assertThat(model.uiState.value.selectedFilter).isEqualTo(PolymarketPositionsFilter.ALL)
        verify(exactly = 0) { finished.selectFilter(any()) }
        verify(exactly = 1) { active.dispose() }
        verify(exactly = 1) { finished.dispose() }
        verify(exactly = 1) { active.resume(any()) }
        verify(exactly = 1) { newActive.resume(any()) }
        verify(exactly = 2) { loader.createFeed(wallet, PolymarketPositionsTab.ACTIVE, any()) }
    }

    @Test
    fun `GIVEN active list WHEN returning from a child screen THEN retains the same feed and content`() = runTest {
        // Arrange
        val model = createModel()
        model.onResume()
        runCurrent()
        model.uiState.value.onFilterSelect(PolymarketPositionsFilter.WON)
        activeState.value = FilteredContent(PolymarketPositionsFilter.WON, activeContent)
        runCurrent()
        val initial = model.uiState.value.content

        // Act
        model.onPause()
        model.onResume()
        runCurrent()

        // Assert
        assertThat(model.uiState.value.content).isSameInstanceAs(initial)
        assertThat(model.uiState.value.selectedFilter).isEqualTo(PolymarketPositionsFilter.WON)
        verify(exactly = 1) { loader.createFeed(wallet, PolymarketPositionsTab.ACTIVE, any()) }
        verify(exactly = 0) { active.dispose() }
    }

    @Test
    fun `GIVEN Active list WHEN filter is selected and cleared THEN reuses the feed and ignores duplicate selections`() = runTest {
        // Arrange
        val model = createModel()
        model.onResume()
        runCurrent()
        every { active.selectFilter(any()) } answers { activeState.value = FilteredContent(firstArg(), Content.Loading) }

        // Act
        model.uiState.value.onFilterSelect(PolymarketPositionsFilter.WON)
        val selected = model.uiState.value
        model.uiState.value.onFilterSelect(PolymarketPositionsFilter.WON)
        model.uiState.value.onFilterSelect(PolymarketPositionsFilter.ALL)
        runCurrent()

        // Assert
        assertThat(selected.selectedFilter).isEqualTo(PolymarketPositionsFilter.WON)
        assertThat(selected.content).isEqualTo(Content.Loading)
        assertThat(model.uiState.value.selectedFilter).isEqualTo(PolymarketPositionsFilter.ALL)
        verify(exactly = 1) { active.selectFilter(PolymarketPositionsFilter.WON) }
        verify(exactly = 1) { active.selectFilter(PolymarketPositionsFilter.ALL) }
        verify(exactly = 1) { loader.createFeed(wallet, PolymarketPositionsTab.ACTIVE, any()) }
        verify(exactly = 0) { active.dispose() }
    }

    @Test
    fun `GIVEN cached All content WHEN selection resets before feed emits THEN restores the same cards`() = runTest {
        // Arrange
        val model = createModel()
        model.onResume()
        runCurrent()

        // Act
        model.uiState.value.onFilterSelect(PolymarketPositionsFilter.WON)
        val pending = model.uiState.value.content
        model.uiState.value.onFilterSelect(PolymarketPositionsFilter.ALL)
        runCurrent()

        // Assert
        assertThat(pending).isEqualTo(Content.Loading)
        assertThat(model.uiState.value.content).isSameInstanceAs(activeContent)
        assertThat(model.uiState.value.selectedFilter).isEqualTo(PolymarketPositionsFilter.ALL)
    }

    @Test
    fun `GIVEN Finished selected WHEN old Active flow emits THEN does not replace Finished content`() = runTest {
        // Arrange
        val model = createModel()
        model.onResume()
        runCurrent()
        model.uiState.value.onTabSelect(Tab.Finished)
        runCurrent()

        // Act
        activeState.value = FilteredContent(content = Content.Error(onRetry = {}))
        runCurrent()

        // Assert
        assertThat(model.uiState.value.content).isEqualTo(Content.Empty)
    }

    @Test
    fun `GIVEN paused screen WHEN tab changes THEN does not load until resumed`() = runTest {
        // Arrange
        val model = createModel()
        model.onResume()
        model.onPause()

        // Act
        model.uiState.value.onTabSelect(Tab.Finished)
        runCurrent()

        // Assert
        verify(exactly = 0) { finished.resume(any()) }

        // Act
        model.onResume()
        model.onResume()
        model.uiState.value.onTabSelect(Tab.Finished)
        runCurrent()

        // Assert
        verify(exactly = 1) { finished.resume(any()) }
    }

    @Test
    fun `GIVEN positions screen WHEN back tapped THEN pops the feature stack`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        model.uiState.value.onBackClick()

        // Assert
        verify(exactly = 1) { router.pop() }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN Finished details WHEN dismissed or back pressed THEN closes only the sheet and retains the list`(
        useBack: Boolean,
    ) = runTest {
        // Arrange
        val position = createClosedPolymarketPosition()
        val content = Content.Data(persistentListOf(PolymarketPositionUMConverter().convert(position)))
        finishedState.value = FilteredContent(content = content)
        val model = createModel(PolymarketPositionsTab.FINISHED)
        model.onResume()
        runCurrent()

        // Act
        finishedClick.captured(position)
        val opened = model.uiState.value
        if (useBack) opened.onBackClick() else opened.onFinishedPositionDismiss()
        runCurrent()

        // Assert
        assertThat(opened.finishedPosition?.summary?.title).isEqualTo(stringReference(position.title))
        assertThat(opened.content).isSameInstanceAs(content)
        assertThat(model.uiState.value.finishedPosition).isNull()
        assertThat(model.uiState.value.content).isSameInstanceAs(content)
        verify(exactly = 1) { loader.createFeed(wallet, PolymarketPositionsTab.FINISHED, null, any()) }
        verify(exactly = 1) { finished.resume(any()) }
        verify(exactly = 0) { finished.dispose() }
        verify(exactly = 0) { router.pop() }
        model.uiState.value.onBackClick()
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN Finished details WHEN tab changes and old card callback fires THEN sheet stays closed`() = runTest {
        // Arrange
        val model = createModel(PolymarketPositionsTab.FINISHED)
        val oldClick = finishedClick.captured
        val position = createClosedPolymarketPosition()
        oldClick(position)

        // Act
        model.uiState.value.onTabSelect(Tab.Active)
        oldClick(position)
        runCurrent()

        // Assert
        assertThat(model.uiState.value.finishedPosition).isNull()
        assertThat(model.uiState.value.selectedTab).isEqualTo(Tab.Active)
        assertThat(model.uiState.value.content).isSameInstanceAs(activeContent)
    }

    @Test
    fun `GIVEN Finished details WHEN app resumes THEN retains the selected position and feed`() = runTest {
        // Arrange
        val model = createModel(PolymarketPositionsTab.FINISHED)
        model.onResume()
        runCurrent()
        finishedClick.captured(createClosedPolymarketPosition())
        val opened = model.uiState.value.finishedPosition

        // Act
        model.onPause()
        model.onResume()
        runCurrent()

        // Assert
        assertThat(model.uiState.value.finishedPosition).isSameInstanceAs(opened)
        verify(exactly = 1) { loader.createFeed(wallet, PolymarketPositionsTab.FINISHED, null, any()) }
        verify(exactly = 0) { finished.dispose() }
    }

    private fun TestScope.createModel(initialTab: PolymarketPositionsTab = PolymarketPositionsTab.ACTIVE): PolymarketPositionsModel {
        every { loader.createFeed(wallet, PolymarketPositionsTab.ACTIVE, capture(positionClick)) } returns active
        every { loader.createFeed(wallet, PolymarketPositionsTab.FINISHED, null, capture(finishedClick)) } returns finished
        every { active.state } returns activeState
        every { finished.state } returns finishedState
        val dispatcher = StandardTestDispatcher(testScheduler)
        return PolymarketPositionsModel(
            paramsContainer = MutableParamsContainer(PolymarketPositionsComponent.Params(wallet, initialTab)),
            router = router,
            dispatchers = TestingCoroutineDispatcherProvider(dispatcher, dispatcher, dispatcher, dispatcher, dispatcher),
            loader = loader,
        ).also { model = it }
    }

    @Test
    fun `GIVEN active position WHEN tapped THEN passes wallet and outcome to details`() = runTest {
        // Arrange
        createModel()
        val position = createPolymarketPosition()

        // Act
        positionClick.captured(position)

        // Assert
        verify(exactly = 1) { router.push(PolymarketRoute.PositionDetails(wallet, position.id), any()) }
    }

    @Test
    fun `GIVEN claimable position WHEN tapped THEN passes wallet and snapshot to result screen`() = runTest {
        // Arrange
        createModel()
        val position = createPolymarketPosition(
            result = PolymarketPositionResult.WON,
            resolutionStatus = PolymarketResolutionStatus.RESOLVED,
        ).copy(isRedeemable = true)

        // Act
        positionClick.captured(position)

        // Assert
        verify(exactly = 1) { router.push(PolymarketRoute.PositionResult(wallet, position), any()) }
        verify(exactly = 0) { router.push(ofType<PolymarketRoute.PositionDetails>(), any()) }
    }
}
package com.tangem.features.polymarket.impl.positions.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.model.PolymarketPositionsFilter
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.features.polymarket.impl.positionresult.model.canOpenPositionResult
import com.tangem.features.polymarket.impl.positiondetails.model.canOpenPositionDetails
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.positiondetails.model.PolymarketFinishedPositionConverter
import com.tangem.features.polymarket.impl.positions.PolymarketPositionsComponent
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM
import com.tangem.features.polymarket.impl.positions.ui.state.PolymarketPositionsUM.Tab
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class PolymarketPositionsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val loader: PolymarketPositionsLoader,
) : Model() {

    private val params = paramsContainer.require<PolymarketPositionsComponent.Params>()
    private val observation = JobHolder()
    private val finishedPositionConverter = PolymarketFinishedPositionConverter()
    private var isResumed = false

    val uiState: StateFlow<PolymarketPositionsUM>
        field = MutableStateFlow(
            PolymarketPositionsUM(
                selectedTab = when (params.initialTab) {
                    PolymarketPositionsTab.ACTIVE -> Tab.Active
                    PolymarketPositionsTab.FINISHED -> Tab.Finished
                },
                content = PolymarketPositionsUM.Content.Loading,
                onTabSelect = ::selectTab,
                onBackClick = ::onBack,
                onFinishedPositionDismiss = ::dismissFinishedPosition,
                onFilterSelect = ::selectFilter,
            ),
        )

    private var feed = createFeed(uiState.value.selectedTab)

    init {
        observeTab()
    }

    fun onResume() {
        if (isResumed) return
        isResumed = true
        feed.resume(modelScope)
    }

    fun onPause() {
        isResumed = false
        feed.pause()
    }

    private fun selectTab(tab: Tab) {
        if (tab == uiState.value.selectedTab) return
        feed.dispose()
        feed = createFeed(tab)
        uiState.update { state ->
            state.copy(
                selectedTab = tab,
                selectedFilter = PolymarketPositionsFilter.ALL,
                content = feed.state.value.content,
                finishedPosition = null,
            )
        }
        observeTab()
        if (isResumed) feed.resume(modelScope)
    }

    private fun selectFilter(filter: PolymarketPositionsFilter) {
        if (uiState.value.selectedTab != Tab.Active || filter == uiState.value.selectedFilter) return
        feed.selectFilter(filter)
        val content = feed.state.value.takeIf { it.filter == filter }?.content ?: PolymarketPositionsUM.Content.Loading
        uiState.update { it.copy(selectedFilter = filter, content = content) }
    }

    private fun createFeed(tab: Tab): PolymarketPositionsLoader.Feed = loader.createFeed(
        userWalletId = params.userWalletId,
        tab = when (tab) {
            Tab.Active -> PolymarketPositionsTab.ACTIVE
            Tab.Finished -> PolymarketPositionsTab.FINISHED
        },
        onPositionClick = if (tab == Tab.Active) ::openPosition else null,
        onClosedPositionClick = if (tab == Tab.Finished) ::openFinishedPosition else null,
    )

    private fun observeTab() {
        observation.cancel()
        val tab = uiState.value.selectedTab
        feed.state
            .onEach { value ->
                uiState.update { state ->
                    if (state.selectedTab == tab && state.selectedFilter == value.filter) {
                        state.copy(
                            content = value.content,
                        )
                    } else {
                        state
                    }
                }
            }
            .launchIn(modelScope)
            .saveIn(observation)
    }

    private fun openPosition(position: PolymarketPosition.Open) {
        when {
            position.canOpenPositionResult -> router.push(PolymarketRoute.PositionResult(params.userWalletId, position))
            position.canOpenPositionDetails -> router.push(
                PolymarketRoute.PositionDetails(params.userWalletId, position.id),
            )
        }
    }

    private fun openFinishedPosition(position: PolymarketPosition.Closed) {
        if (uiState.value.selectedTab != Tab.Finished) return
        uiState.update { it.copy(finishedPosition = finishedPositionConverter.convert(position)) }
    }

    private fun dismissFinishedPosition() {
        uiState.update { it.copy(finishedPosition = null) }
    }

    private fun onBack() {
        if (uiState.value.finishedPosition != null) dismissFinishedPosition() else router.pop()
    }
}
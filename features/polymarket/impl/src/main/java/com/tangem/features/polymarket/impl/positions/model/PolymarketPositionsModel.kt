package com.tangem.features.polymarket.impl.positions.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
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
                onBackClick = { router.pop() },
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
        uiState.update { it.copy(selectedTab = tab, content = feed.state.value) }
        observeTab()
        if (isResumed) feed.resume(modelScope)
    }

    private fun createFeed(tab: Tab): PolymarketPositionsLoader.Feed = loader.createFeed(
        userWalletId = params.userWalletId,
        tab = when (tab) {
            Tab.Active -> PolymarketPositionsTab.ACTIVE
            Tab.Finished -> PolymarketPositionsTab.FINISHED
        },
        onPositionClick = if (tab == Tab.Active) ::openPosition else null,
    )

    private fun observeTab() {
        observation.cancel()
        val tab = uiState.value.selectedTab
        feed.state
            .onEach { content -> uiState.update { if (it.selectedTab == tab) it.copy(content = content) else it } }
            .launchIn(modelScope)
            .saveIn(observation)
    }

    private fun openPosition(id: PolymarketPositionId) {
        router.push(PolymarketRoute.PositionDetails(params.userWalletId, id))
    }
}
package com.tangem.features.feed.components.market.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableModularContentComponent
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.feed.model.market.list.MarketsListModel
import com.tangem.features.feed.ui.market.list.MarketsList
import com.tangem.features.feed.ui.market.list.TopBarWithSearch
import com.tangem.features.feed.ui.market.list.state.SortByTypeUM
import kotlinx.serialization.Serializable

internal class DefaultMarketsTokenListComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableModularContentComponent, AppComponentContext by appComponentContext {

    private val model: MarketsListModel = getOrCreateModel<MarketsListModel, Params>(params = params)

    @Composable
    override fun Title() {
        val state by model.state.collectAsStateWithLifecycle()
        TopBarWithSearch(
            onBackClick = params.onBackClicked,
            onSearchClick = state.onSearchClicked,
            marketsSearchBar = state.marketsSearchBar,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        LifecycleStartEffect(Unit) {
            model.isVisibleOnScreen.value = true
            onStopOrDispose {
                model.isVisibleOnScreen.value = false
            }
        }
        val state by model.state.collectAsStateWithLifecycle()
        MarketsList(
            modifier = modifier,
            state = state,
        )
    }

    @Composable
    override fun Footer() = Unit

    @Serializable
    data class Params(
        val onBackClicked: () -> Unit,
        val onTokenClick: ((TokenMarketParams, AppCurrency) -> Unit),
        val preselectedSortType: SortByTypeUM,
        val shouldAlwaysShowSearchBar: Boolean,
    )
}
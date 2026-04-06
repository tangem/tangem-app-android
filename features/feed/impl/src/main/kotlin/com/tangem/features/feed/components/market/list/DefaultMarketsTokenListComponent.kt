package com.tangem.features.feed.components.market.list

import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.decompose.ComposableModularBottomSheetContentComponent
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.features.feed.model.market.list.MarketsListModel
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.ui.components.FeedSearchBar
import com.tangem.features.feed.ui.market.list.MarketsList
import com.tangem.features.feed.ui.market.list.TopBarWithSearch
import dev.chrisbanes.haze.HazeProgressive
import kotlinx.serialization.Serializable

internal class DefaultMarketsTokenListComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
    private val clickIntents: ClickIntents,
) : ComposableModularBottomSheetContentComponent, AppComponentContext by appComponentContext {

    private val model: MarketsListModel = getOrCreateModel<MarketsListModel, ModelParams>(
        params = ModelParams(
            params = params,
            clickIntents = clickIntents,
        ),
    )

    @Composable
    override fun Title(bottomSheetState: State<BottomSheetState>) {
        val state by model.state.collectAsStateWithLifecycle()
        val bsState by bottomSheetState
        val background = LocalMainBottomSheetColor.current.value

        if (LocalRedesignEnabled.current) {
            FeedSearchBar(
                isSearchBarClickable = bottomSheetState.value == BottomSheetState.EXPANDED,
                feedListSearchBar = state.feedListSearchBar,
                modifier = Modifier
                    .drawBehind { drawRect(background) }
                    .hazeEffectTangem {
                        progressive = HazeProgressive.verticalGradient(
                            startIntensity = .55f,
                            endIntensity = 0f,
                            preferPerformance = true,
                            easing = EaseOut,
                        )
                    },
                startContent = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_back_28),
                        contentDescription = null,
                        tint = TangemTheme.colors2.graphic.neutral.primary,
                        modifier = Modifier
                            .size(TangemTheme.dimens2.x11)
                            .background(
                                color = TangemTheme.colors2.button.backgroundSecondary,
                                shape = CircleShape,
                            )
                            .clickableSingle(
                                onClick = clickIntents.onBackClicked,
                                enabled = bottomSheetState.value == BottomSheetState.EXPANDED,
                            )
                            .padding(TangemTheme.dimens2.x2),
                    )
                },
            )
        } else {
            TopBarWithSearch(
                onBackClick = clickIntents.onBackClicked,
                onSearchClick = state.onSearchClicked,
                marketsSearchBar = state.marketsSearchBar,
                bottomSheetState = bsState,
            )
        }
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        modifier: Modifier,
    ) {
        LifecycleStartEffect(Unit) {
            model.isVisibleOnScreen.value = true
            onStopOrDispose {
                model.isVisibleOnScreen.value = false
            }
        }

        val bsState by bottomSheetState
        val state by model.state.collectAsStateWithLifecycle()

        LaunchedEffect(bsState) {
            model.containerBottomSheetState.value = bsState
        }

        MarketsList(
            contentPadding = contentPadding,
            modifier = modifier,
            state = state,
        )
    }

    @Serializable
    data class Params(
        val preselectedSortType: SortByTypeUM,
        val shouldAlwaysShowSearchBar: Boolean,
    )

    data class ClickIntents(
        val onBackClicked: () -> Unit,
        val onSearchClicked: () -> Unit,
        val onTokenClick: ((TokenMarketParams, AppCurrency) -> Unit),
    )

    data class ModelParams(
        val params: Params,
        val clickIntents: ClickIntents,
    )
}
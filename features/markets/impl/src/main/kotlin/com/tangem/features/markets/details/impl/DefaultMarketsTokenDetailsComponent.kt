package com.tangem.features.markets.details.impl

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.blockchainsdk.compatibility.getTokenIdIfL2Network
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.child
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.markets.details.MarketsTokenDetailsComponent
import com.tangem.features.markets.details.MarketsTokenDetailsComponent.Params
import com.tangem.features.markets.details.impl.model.MarketsTokenDetailsModel
import com.tangem.features.markets.details.impl.model.state.TokenNetworksState
import com.tangem.features.markets.details.impl.ui.MarketsTokenDetailsContent
import com.tangem.features.markets.entry.BottomSheetState
import com.tangem.features.markets.portfolio.api.MarketsPortfolioComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Stable
internal class DefaultMarketsTokenDetailsComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Params,
    portfolioComponentFactory: MarketsPortfolioComponent.Factory,
) : AppComponentContext by appComponentContext, MarketsTokenDetailsComponent {

    // applying l2 compatibility
    private val updatedParams = params.copy(
        token = params.token.copy(
            id = getTokenIdIfL2Network(params.token.id),
        ),
    )
    private val model: MarketsTokenDetailsModel = getOrCreateModel(updatedParams)

    private val portfolioComponent: MarketsPortfolioComponent? = if (updatedParams.showPortfolio) {
        portfolioComponentFactory.create(
            context = child("my_portfolio"),
            params = MarketsPortfolioComponent.Params(updatedParams.token),
        )
    } else {
        null
    }

    init {
        componentScope.launch {
            model.networksState.collectLatest {
                when (it) {
                    is TokenNetworksState.NetworksAvailable -> portfolioComponent?.setTokenNetworks(it.networks)
                    TokenNetworksState.NoNetworksAvailable -> portfolioComponent?.setNoNetworksAvailable()
                    else -> {}
                }
            }
        }
    }

    @Composable
    override fun BottomSheetContent(
        bottomSheetState: State<BottomSheetState>,
        onHeaderSizeChange: (Dp) -> Unit,
        modifier: Modifier,
    ) {
        LifecycleStartEffect(Unit) {
            model.isVisibleOnScreen.value = true
            onStopOrDispose {
                model.isVisibleOnScreen.value = false
            }
        }

        val state by model.state.collectAsStateWithLifecycle()
        val bsState by bottomSheetState

        LaunchedEffect(bsState) {
            model.isVisibleOnScreen.value = bsState == BottomSheetState.EXPANDED
        }

        BackHandler(enabled = bsState == BottomSheetState.EXPANDED) {
            navigateBack()
        }

        MarketsTokenDetailsContent(
            modifier = modifier,
            backgroundColor = LocalMainBottomSheetColor.current.value,
            addTopBarStatusBarPadding = false,
            state = state,
            onBackClick = {
                if (bsState == BottomSheetState.EXPANDED) {
                    navigateBack()
                }
            },
            onHeaderSizeChange = onHeaderSizeChange,
            portfolioBlock = portfolioComponent?.let { component ->
                { blockModifier ->
                    component.Content(blockModifier)
                }
            },
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler {
            navigateBack()
        }

        LifecycleStartEffect(Unit) {
            model.isVisibleOnScreen.value = true
            onStopOrDispose {
                model.isVisibleOnScreen.value = false
            }
        }

        val state by model.state.collectAsStateWithLifecycle()

        MarketsTokenDetailsContent(
            modifier = modifier,
            backgroundColor = TangemTheme.colors.background.tertiary,
            addTopBarStatusBarPadding = true,
            state = state,
            onBackClick = ::navigateBack,
            onHeaderSizeChange = {},
            portfolioBlock = portfolioComponent?.let { component ->
                { blockModifier ->
                    component.Content(blockModifier)
                }
            },
        )
    }

    private fun navigateBack() = router.pop()

    @AssistedFactory
    interface Factory : MarketsTokenDetailsComponent.Factory {
        override fun create(context: AppComponentContext, params: Params): DefaultMarketsTokenDetailsComponent
    }
}

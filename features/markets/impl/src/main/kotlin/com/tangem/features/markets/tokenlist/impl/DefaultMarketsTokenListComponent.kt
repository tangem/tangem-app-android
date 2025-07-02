package com.tangem.features.markets.tokenlist.impl

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRoute.MarketsTokenDetails.AnalyticsParams
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.WindowInsetsZero
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.TokenMarketParams
import com.tangem.domain.markets.toSerializableParam
import com.tangem.features.markets.entry.BottomSheetState
import com.tangem.features.markets.tokenlist.MarketsTokenListComponent
import com.tangem.features.markets.tokenlist.impl.model.MarketsListModel
import com.tangem.features.markets.tokenlist.impl.ui.MarketsList
import com.tangem.features.markets.tokenlist.impl.ui.MarketsListWithBack
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Suppress("UnusedPrivateMember")
internal class DefaultMarketsTokenListComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
    @Assisted onTokenClick: ((TokenMarketParams, AppCurrency) -> Unit)? = null,
) : AppComponentContext by appComponentContext, MarketsTokenListComponent {

    private val model: MarketsListModel = getOrCreateModel()

    init {
        model.tokenSelected
            .onEach { (token, appCurrency) ->
                // If specific routing call back is provided, invoke it (for example inner routing in Bottom Sheet)
                // Otherwise navigate using app routing
                if (onTokenClick != null) {
                    onTokenClick(token.toSerializableParam(), appCurrency)
                } else {
                    router.push(
                        AppRoute.MarketsTokenDetails(
                            token = token.toSerializableParam(),
                            appCurrency = appCurrency,
                            showPortfolio = true,
                            analyticsParams = AnalyticsParams(
                                blockchain = null,
                                source = "Market",
                            ),
                        ),
                    )
                }
            }
            .launchIn(componentScope)
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
            model.containerBottomSheetState.value = bsState
        }

        MarketsList(
            modifier = modifier,
            state = state,
            onHeaderSizeChange = onHeaderSizeChange,
            bottomSheetState = bsState,
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

        Scaffold(
            contentWindowInsets = WindowInsetsZero,
            containerColor = TangemTheme.colors.background.primary,
        ) {
            MarketsListWithBack(
                modifier = Modifier
                    .statusBarsPadding()
                    .imePadding()
                    .padding(it),
                state = state,
                bottomSheetState = BottomSheetState.EXPANDED,
                onBackClick = router::pop,
            )
        }
    }

    @AssistedFactory
    interface FactoryBottomSheet : MarketsTokenListComponent.FactoryBottomSheet {
        override fun create(
            context: AppComponentContext,
            params: Unit,
            onTokenClick: ((TokenMarketParams, AppCurrency) -> Unit)?,
        ): DefaultMarketsTokenListComponent
    }
}
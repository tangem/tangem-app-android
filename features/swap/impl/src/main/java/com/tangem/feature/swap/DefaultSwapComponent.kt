package com.tangem.feature.swap

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.common.ui.swapStoriesScreen.SwapStoriesScreen
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.model.SwapModel
import com.tangem.feature.swap.router.SwapNavScreen
import com.tangem.feature.swap.ui.SwapScreen
import com.tangem.feature.swap.ui.SwapSelectTokenScreen
import com.tangem.feature.swap.ui.SwapSuccessScreen
import com.tangem.features.swap.SwapComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("UnusedPrivateMember")
internal class DefaultSwapComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: SwapComponent.Params,
) : SwapComponent, AppComponentContext by appComponentContext {

    private val model: SwapModel = getOrCreateModel(params)

    init {
        lifecycle.subscribe(
            onStart = model::onStart,
            onStop = model::onStop,
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        Crossfade(
            modifier = Modifier.background(TangemTheme.colors.background.secondary),
            targetState = model.currentScreen,
            label = "",
        ) { screen ->
            when (screen) {
                SwapNavScreen.PromoStories -> {
                    val storiesConfig = model.uiState.storiesConfig
                    if (storiesConfig != null) {
                        SwapStoriesScreen(config = storiesConfig)
                    } else {
                        SwapScreen(stateHolder = model.uiState)
                    }
                }
                SwapNavScreen.Main -> SwapScreen(stateHolder = model.uiState)
                SwapNavScreen.Success -> {
                    val successState = model.uiState.successState
                    if (successState != null) {
                        SwapSuccessScreen(state = successState, model.uiState.onBackClicked)
                    } else {
                        SwapScreen(stateHolder = model.uiState)
                    }
                }
                SwapNavScreen.SelectToken -> {
                    val tokenState = model.uiState.selectTokenState
                    if (tokenState != null) {
                        SwapSelectTokenScreen(state = tokenState, onBack = model.uiState.onBackClicked)
                    } else {
                        SwapScreen(stateHolder = model.uiState)
                    }
                }
            }
        }
    }

    @AssistedFactory
    interface Factory : SwapComponent.Factory {
        override fun create(context: AppComponentContext, params: SwapComponent.Params): DefaultSwapComponent
    }
}
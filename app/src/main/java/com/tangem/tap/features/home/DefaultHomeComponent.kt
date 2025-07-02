package com.tangem.tap.features.home

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.utils.ChangeRootBackgroundColorEffect
import com.tangem.core.ui.utils.findActivity
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.home.api.HomeComponent
import com.tangem.tap.features.home.compose.StoriesScreen
import com.tangem.tap.features.home.compose.StoriesScreenV2
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.store
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.rekotlin.StoreSubscriber

@Suppress("UnusedPrivateMember")
internal class DefaultHomeComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
) : HomeComponent, AppComponentContext by appComponentContext, StoreSubscriber<HomeState> {

    private val model: HomeModel = getOrCreateModel()

    private var homeState: MutableState<HomeState> = mutableStateOf(store.state.homeState)

    init {
        lifecycle.subscribe(
            onCreate = {
                store.dispatch(HomeAction.OnCreate)
            },
            onStart = {
                store.subscribe(subscriber = this) { state ->
                    state
                        .skipRepeats { oldState, newState -> oldState.homeState == newState.homeState }
                        .select(AppState::homeState)
                }
            },
            onStop = {
                store.unsubscribe(this)
            },
        )
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val activity = LocalContext.current.findActivity()
        BackHandler(onBack = activity::finish)
        SystemBarsIconsDisposable(darkIcons = false)
        if (homeState.value.isV2StoriesEnabled) {
            StoriesScreenV2(
                homeState = homeState,
                onCreateNewWalletButtonClick = model::onCreateNewWalletScreen,
                onAddExistingWalletButtonClick = model::onAddExistingWalletScreen,
                onScanButtonClick = model::onScanClick,
            )
        } else {
            StoriesScreen(
                homeState = homeState,
                onScanButtonClick = model::onScanClick,
                onShopButtonClick = model::onShopClick,
                onSearchTokensClick = model::onSearchClick,
            )
        }

        ChangeRootBackgroundColorEffect(Color(color = 0xFF010101))
    }

    override fun newState(state: HomeState) {
        homeState.value = state
    }

    @AssistedFactory
    interface Factory : HomeComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultHomeComponent
    }
}
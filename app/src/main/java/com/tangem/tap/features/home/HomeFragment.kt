package com.tangem.tap.features.home

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.domain.tokens.TokensAction
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.home.compose.StoriesScreen
import com.tangem.tap.features.home.featuretoggles.HomeFeatureToggles
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.store
import dagger.hilt.android.AndroidEntryPoint
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@AndroidEntryPoint
internal class HomeFragment : ComposeFragment(), StoreSubscriber<HomeState> {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Inject
    lateinit var homeFeatureToggles: HomeFeatureToggles

    private var homeState: MutableState<HomeState> = mutableStateOf(store.state.homeState)

    private val viewModel by viewModels<HomeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store.dispatch(HomeAction.OnCreate)
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        BackHandler(onBack = requireActivity()::finish)
        SystemBarsIconsDisposable(darkIcons = false)
        ScreenContent()
    }

    override fun onStart() {
        super.onStart()

        store.subscribe(subscriber = this) { state ->
            state
                .skipRepeats { oldState, newState -> oldState.homeState == newState.homeState }
                .select(AppState::homeState)
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun newState(state: HomeState) {
        if (activity == null || view == null) return

        homeState.value = state
    }

    @Suppress("TopLevelComposableFunctions")
    @Composable
    private fun ScreenContent() {
        StoriesScreen(
            homeState = homeState,
            onScanButtonClick = {
                if (homeFeatureToggles.isCallbacksRefactoringEnabled) {
                    viewModel.onScanClick()
                } else {
                    Analytics.send(IntroductionProcess.ButtonScanCard())
                    store.dispatch(action = HomeAction.ReadCard(scope = requireActivity().lifecycleScope))
                }
            },
            onShopButtonClick = {
                if (homeFeatureToggles.isCallbacksRefactoringEnabled) {
                    viewModel.onShopClick()
                } else {
                    Analytics.send(IntroductionProcess.ButtonBuyCards())
                    store.dispatch(HomeAction.GoToShop)
                }
            },
            onSearchTokensClick = {
                if (homeFeatureToggles.isCallbacksRefactoringEnabled) {
                    viewModel.onSearchClick()
                } else {
                    Analytics.send(IntroductionProcess.ButtonTokensList())
                    store.dispatchNavigationAction { push(AppRoute.ManageTokens(readOnlyContent = true)) }
                    store.dispatch(TokensAction.SetArgs.ReadAccess)
                }
            },
        )
    }
}
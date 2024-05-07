package com.tangem.tap.features.home

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.AppScreen
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.haptic.HapticManager
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.domain.tokens.TokensAction
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.home.compose.StoriesScreen
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.store
import dagger.hilt.android.AndroidEntryPoint
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : ComposeFragment(), StoreSubscriber<HomeState> {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    override lateinit var hapticManager: HapticManager

    private var homeState: MutableState<HomeState> = mutableStateOf(store.state.homeState)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store.dispatch(HomeAction.OnCreate)
        store.dispatch(HomeAction.Init)
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        BackHandler(onBack = requireActivity()::finish)
        SystemBarsEffect {
            setSystemBarsColor(color = Color.Transparent, darkIcons = false)
        }
        ScreenContent()
    }

    override fun onStart() {
        super.onStart()
        activity?.window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }

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
                Analytics.send(IntroductionProcess.ButtonScanCard())
                store.dispatch(action = HomeAction.ReadCard(scope = requireActivity().lifecycleScope))
            },
            onShopButtonClick = {
                Analytics.send(IntroductionProcess.ButtonBuyCards())
                store.dispatch(HomeAction.GoToShop(store.state.globalState.userCountryCode))
            },
            onSearchTokensClick = {
                Analytics.send(IntroductionProcess.ButtonTokensList())
                store.dispatch(NavigationAction.NavigateTo(AppScreen.ManageTokens))
                store.dispatch(TokensAction.SetArgs.ReadAccess)
            },
        )
    }
}
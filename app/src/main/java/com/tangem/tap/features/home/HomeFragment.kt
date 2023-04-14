package com.tangem.tap.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.home.compose.StoriesScreen
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.features.tokens.legacy.redux.TokensAction
import com.tangem.tap.store
import org.rekotlin.StoreSubscriber

class HomeFragment : Fragment(), StoreSubscriber<HomeState> {

    private var homeState: MutableState<HomeState> = mutableStateOf(store.state.homeState)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store.dispatch(HomeAction.OnCreate)
        store.dispatch(HomeAction.Init)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(inflater.context).apply {
            setContent {
                BackHandler {
                    requireActivity().finish()
                }

                AppCompatTheme {
                    ScreenContent()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }

        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.homeState == newState.homeState
            }.select { it.homeState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rollbackStatusBarIconsColor()
    }

    override fun newState(state: HomeState) {
        if (activity == null || view == null) return

        homeState.value = state
    }

    @Suppress("TopLevelComposableFunctions")
    @Composable
    private fun ScreenContent() {
        StoriesScreen(
            homeState,
            onScanButtonClick = {
                Analytics.send(IntroductionProcess.ButtonScanCard())
                store.dispatch(HomeAction.ReadCard())
            },
            onShopButtonClick = {
                Analytics.send(IntroductionProcess.ButtonBuyCards())
                store.dispatch(HomeAction.GoToShop(store.state.globalState.userCountryCode))
            },
            onSearchTokensClick = {
                Analytics.send(IntroductionProcess.ButtonTokensList())
                store.dispatch(NavigationAction.NavigateTo(AppScreen.AddTokens))
                store.dispatch(TokensAction.AllowToAddTokens(false))
                store.dispatch(TokensAction.LoadCurrencies())
            },
        )
    }

    /*
     * !!! Workaround !!!
     * Used to roll back the color of icons in the system bars after the stories screen
     */
    private fun rollbackStatusBarIconsColor() {
        val windowInsetsController = WindowInsetsControllerCompat(
            activity?.window ?: return,
            view ?: return,
        )
        windowInsetsController.isAppearanceLightStatusBars = true
        windowInsetsController.isAppearanceLightNavigationBars = true
    }
}

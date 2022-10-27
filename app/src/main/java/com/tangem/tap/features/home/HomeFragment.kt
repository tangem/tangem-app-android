package com.tangem.tap.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.home.compose.StoriesScreen
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.store
import org.rekotlin.StoreSubscriber

class HomeFragment : Fragment(), StoreSubscriber<HomeState> {

    private var homeState: MutableState<HomeState> = mutableStateOf(store.state.homeState)
    private var composeView: ComposeView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store.dispatch(HomeAction.Init)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val context = container?.context ?: return null

        composeView = ComposeView(context).apply {
            setContent {
                AppCompatTheme {
                    ScreenContent()
                }
            }
        }

        return composeView
    }

    override fun onStart() {
        super.onStart()
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
        composeView = null
    }

    override fun newState(state: HomeState) {
        if (activity == null || view == null) return

        homeState.value = state
    }

    @Composable
    private fun ScreenContent() {
        StoriesScreen(
            homeState,
            onScanButtonClick = { store.dispatch(HomeAction.ReadCard) },
            onShopButtonClick = { store.dispatch(HomeAction.GoToShop(store.state.globalState.userCountryCode)) },
            onSearchTokensClick = {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.AddTokens))
                store.dispatch(TokensAction.AllowToAddTokens(false))
                store.dispatch(TokensAction.LoadCurrencies())
            },
        )
    }

    /*
    * !!! Workaround !!!
    * Used to roll back the color of icons in the system bars after the stories screen
    * */
    private fun rollbackStatusBarIconsColor() {
        val windowInsetsController = WindowInsetsControllerCompat(
            activity?.window ?: return,
            view ?: return,
        )
        windowInsetsController.isAppearanceLightStatusBars = true
        windowInsetsController.isAppearanceLightNavigationBars = true
    }
}
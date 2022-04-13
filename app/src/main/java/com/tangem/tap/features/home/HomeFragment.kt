package com.tangem.tap.features.home

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.home.compose.StoriesScreen
import com.tangem.tap.features.home.redux.HomeAction
import com.tangem.tap.features.home.redux.HomeState
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupAction
import com.tangem.tap.features.tokens.redux.TokensAction
import com.tangem.tap.store
import com.tangem.wallet.R
import org.rekotlin.StoreSubscriber

class HomeFragment : Fragment(R.layout.fragment_home), StoreSubscriber<HomeState> {

    var homeState: MutableState<HomeState> = mutableStateOf(store.state.homeState)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store.dispatch(HomeAction.Init)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        store.dispatch(BackupAction.CheckForUnfinishedBackup)


        getView()?.findViewById<ComposeView>(R.id.cv_stories)?.setContent {
            AppCompatTheme {
                StoriesScreen(
                    homeState,
                    onScanButtonClick = { store.dispatch(HomeAction.ReadCard) },
                    onShopButtonClick = { store.dispatch(HomeAction.GoToShop(getRegionProvider())) },
                    onSearchTokensClick = {
                        store.dispatch(TokensAction.AllowToAddTokens(false))
                        store.dispatch(TokensAction.LoadCurrencies())
                        store.dispatch(NavigationAction.NavigateTo(AppScreen.AddTokens))
                    }
                )
            }
        }
    }

    private fun getRegionProvider(): RegionProvider = RegionService(
        listOf(
//            TelephonyManagerRegionProvider(requireContext()),
            LocaleRegionProvider()
        )
    )

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


    override fun newState(state: HomeState) {
        if (activity == null || view == null) return

        homeState.value = state
    }

}
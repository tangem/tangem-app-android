package com.tangem.tap.features.details.ui.walletconnect

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.transition.TransitionInflater
import com.tangem.core.analytics.Analytics
import com.tangem.core.navigation.NavigationAction
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.tap.common.analytics.events.WalletConnect
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectState
import com.tangem.tap.store
import com.tangem.wallet.R
import dagger.hilt.android.AndroidEntryPoint
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@AndroidEntryPoint
internal class WalletConnectFragment : ComposeFragment(), StoreSubscriber<WalletConnectState> {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    private val viewModel = WalletConnectViewModel(store)
    private var screenState: MutableState<WalletConnectScreenState> =
        mutableStateOf(viewModel.updateState(store.state.walletConnectState))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.send(WalletConnect.ScreenOpened())
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        WalletConnectScreen(
            modifier = modifier,
            state = screenState.value,
            onBackClick = {
                if (screenState.value.isLoading) {
                    store.dispatch(
                        WalletConnectAction.FailureEstablishingSession(
                            store.state.walletConnectState.newSessionData?.session?.session,
                        ),
                    )
                }
                store.dispatch(NavigationAction.PopBackTo())
            },
        )
    }

    override fun TransitionInflater.inflateTransitions(): Boolean {
        enterTransition = inflateTransition(R.transition.fade)
        exitTransition = inflateTransition(R.transition.fade)

        return true
    }

    override fun onStart() {
        super.onStart()
        store.subscribe(this) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.walletConnectState == newState.walletConnectState
            }.select { it.walletConnectState }
        }
    }

    override fun onStop() {
        super.onStop()
        store.unsubscribe(this)
    }

    override fun newState(state: WalletConnectState) {
        if (activity == null || view == null) return
        screenState.value = viewModel.updateState(state)
    }
}
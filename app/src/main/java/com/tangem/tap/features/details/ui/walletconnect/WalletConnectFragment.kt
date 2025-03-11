package com.tangem.tap.features.details.ui.walletconnect

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.Analytics
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.tap.common.analytics.events.WalletConnect
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectState
import com.tangem.tap.store
import dagger.hilt.android.AndroidEntryPoint
import org.rekotlin.StoreSubscriber
import javax.inject.Inject

@AndroidEntryPoint
internal class WalletConnectFragment : ComposeFragment(), StoreSubscriber<WalletConnectState> {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    private val viewModel: WalletConnectViewModel by viewModels()

    private var screenState: MutableState<WalletConnectScreenState>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.send(WalletConnect.ScreenOpened())
        lifecycle.addObserver(viewModel)
        screenState = mutableStateOf(viewModel.updateState(store.state.walletConnectState))
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        val state = screenState?.value ?: return
        WalletConnectScreen(
            modifier = modifier,
            state = state,
            onBackClick = {
                store.dispatchNavigationAction(AppRouter::pop)
            },
        )
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
        screenState?.value = viewModel.updateState(state)
    }
}
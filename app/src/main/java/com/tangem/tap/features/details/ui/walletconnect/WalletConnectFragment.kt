package com.tangem.tap.features.details.ui.walletconnect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectState
import com.tangem.tap.store
import org.rekotlin.StoreSubscriber

class WalletConnectFragment : Fragment(), StoreSubscriber<WalletConnectState> {
    private val viewModel = WalletConnectViewModel(store)
    private var screenState: MutableState<WalletConnectScreenState> =
        mutableStateOf(viewModel.updateState(store.state.walletConnectState))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(android.R.transition.fade)
        exitTransition = inflater.inflateTransition(android.R.transition.fade)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                isTransitionGroup = true
                TangemTheme {
                    WalletConnectScreen(
                        state = screenState.value,
                        onBackPressed = {
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
            }
        }
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

package com.tangem.tap.features.details.ui.walletconnect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.lifecycle.subscribe
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.Analytics
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.tap.common.analytics.events.WalletConnect
import com.tangem.tap.common.extensions.dispatchNavigationAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectState
import com.tangem.tap.features.details.ui.walletconnect.api.WalletConnectComponent
import com.tangem.tap.store
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.rekotlin.StoreSubscriber

@Suppress("UnusedPrivateMember")
internal class DefaultWalletConnectComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: Unit,
) : WalletConnectComponent, AppComponentContext by appComponentContext, StoreSubscriber<WalletConnectState> {

    private val model: WalletConnectModel = getOrCreateModel()

    private var screenState: MutableState<WalletConnectScreenState>? = null

    init {
        lifecycle.subscribe(
            onCreate = {
                Analytics.send(WalletConnect.ScreenOpened())
                screenState = mutableStateOf(model.updateState(store.state.walletConnectState))
            },
            onStart = {
                store.subscribe(this) { state ->
                    state.skipRepeats { oldState, newState ->
                        oldState.walletConnectState == newState.walletConnectState
                    }.select { it.walletConnectState }
                }
            },
            onStop = {
                store.unsubscribe(this)
            },
        )
    }

    override fun newState(state: WalletConnectState) {
        screenState?.value = model.updateState(state)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state = screenState?.value ?: return
        WalletConnectScreen(
            modifier = modifier,
            state = state,
            onBackClick = {
                store.dispatchNavigationAction(AppRouter::pop)
            },
        )
    }

    @AssistedFactory
    interface Factory : WalletConnectComponent.Factory {
        override fun create(context: AppComponentContext, params: Unit): DefaultWalletConnectComponent
    }
}

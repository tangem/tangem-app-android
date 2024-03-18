package com.tangem.tap.features.details.ui.walletconnect

import androidx.lifecycle.*
import arrow.core.getOrElse
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectSession
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectState
import com.tangem.tap.store
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class WalletConnectViewModel @Inject constructor(
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
) : ViewModel(), DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        viewModelScope.launch {
            listenToQrScanningUseCase(SourceType.WALLET_CONNECT)
                .getOrElse { emptyFlow() }
                .flowWithLifecycle(owner.lifecycle, minActiveState = Lifecycle.State.CREATED)
                .collect { store.dispatch(WalletConnectAction.OpenSession(it)) }
        }
    }

    fun updateState(state: WalletConnectState): WalletConnectScreenState {
        Timber.d("WC2 Sessions: ${state.wc2Sessions}")
        val sessions = state.sessions.map { wcSession -> WcSessionForScreen.fromSession(wcSession) } + state.wc2Sessions
        return WalletConnectScreenState(
            sessions.toImmutableList(),
            isLoading = state.loading,
            onRemoveSession = { sessionUri -> onRemoveSession(sessionUri, state.sessions, state.wc2Sessions) },
            onAddSession = { copiedUri -> store.dispatch(WalletConnectAction.StartWalletConnect(copiedUri)) },
        )
    }

    private fun onRemoveSession(
        sessionUri: String,
        sessions: List<WalletConnectSession>,
        wc2sessions: List<WcSessionForScreen>,
    ) {
        sessions
            .firstOrNull { it.session.toUri() == sessionUri }
            ?.let { baseSession ->
                store.dispatch(WalletConnectAction.DisconnectSession(baseSession.session.topic, baseSession.session))
                return
            }
        wc2sessions.firstOrNull { it.sessionId == sessionUri }?.let {
            store.dispatch(WalletConnectAction.DisconnectSession(sessionUri, null))
        }
    }
}

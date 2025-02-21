package com.tangem.tap.features.details.ui.walletconnect

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectState
import com.tangem.tap.store
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
@ComponentScoped
internal class WalletConnectModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
    private val clipboardManager: ClipboardManager,
) : Model() {

    init {
        modelScope.launch {
            listenToQrScanningUseCase(SourceType.WALLET_CONNECT)
                .getOrElse { emptyFlow() }
                .collect { store.dispatch(WalletConnectAction.OpenSession(it)) }
        }
    }

    fun updateState(state: WalletConnectState): WalletConnectScreenState {
        Timber.d("WC2 Sessions: ${state.wc2Sessions}")
        val sessions = state.wc2Sessions
        return WalletConnectScreenState(
            sessions.toImmutableList(),
            isLoading = state.loading,
            onRemoveSession = { sessionUri -> onRemoveSession(sessionUri, sessions) },
            onAddSession = {
                store.dispatch(
                    WalletConnectAction.StartWalletConnect(copiedUri = clipboardManager.getText()),
                )
            },
        )
    }

    private fun onRemoveSession(sessionUri: String, wc2sessions: List<WcSessionForScreen>) {
        wc2sessions.firstOrNull { it.sessionId == sessionUri }?.let {
            store.dispatch(WalletConnectAction.DisconnectSession(sessionUri))
        }
    }
}
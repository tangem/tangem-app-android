package com.tangem.tap.features.details.ui.walletconnect

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectState
import com.tangem.tap.features.details.ui.walletconnect.api.WalletConnectComponent
import com.tangem.tap.store
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
@ModelScoped
internal class WalletConnectModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<WalletConnectComponent.Params>()

    init {
        modelScope.launch {
            listenToQrScanningUseCase(SourceType.WALLET_CONNECT)
                .getOrElse { emptyFlow() }
                .map {
                    WalletConnectAction.OpenSession(
                        wcUri = it,
                        source = WalletConnectAction.OpenSession.SourceType.QR,
                        userWalletId = params.userWalletId,
                    )
                }
                .collect { store.dispatch(it) }
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
                store.dispatch(WalletConnectAction.StartWalletConnect)
            },
        )
    }

    private fun onRemoveSession(sessionUri: String, wc2sessions: List<WcSessionForScreen>) {
        wc2sessions.firstOrNull { it.sessionId == sessionUri }?.let {
            store.dispatch(WalletConnectAction.DisconnectSession(sessionUri))
        }
    }
}
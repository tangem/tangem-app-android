package com.tangem.features.walletconnect.connections.model

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.walletconnect.usecase.WcSessionsUseCase
import com.tangem.features.walletconnect.connections.entity.WcConnectionsState
import com.tangem.features.walletconnect.connections.entity.WcConnectionsTopAppBarConfig
import com.tangem.features.walletconnect.connections.model.transformers.WcSessionsTransformer
import com.tangem.features.walletconnect.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@ModelScoped
internal class WcConnectionsModel @Inject constructor(
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
    private val wcSessionsUseCase: WcSessionsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val uiState: StateFlow<WcConnectionsState>
    field = MutableStateFlow<WcConnectionsState>(getInitialState())

    init {
        listenQrUpdates()
        listenWcSessions()
    }

    private fun listenQrUpdates() {
        listenToQrScanningUseCase(SourceType.WALLET_CONNECT)
            .getOrElse { emptyFlow() }
            .onEach {
                // TODO: [REDACTED_JIRA]
                Timber.d(it)
            }
            .launchIn(modelScope)
    }

    private fun listenWcSessions() {
        wcSessionsUseCase.invoke()
            .conflate()
            .distinctUntilChanged()
            .onEach { sessionsMap ->
                uiState.update(
                    WcSessionsTransformer(
                        sessionsMap = sessionsMap,
                        openAppInfoModal = { Timber.d("Open app info for session: $it") },
                    ),
                )
            }
            .launchIn(modelScope)
    }

    private fun openQrScanScreen() {
        router.push(AppRoute.QrScanning(source = AppRoute.QrScanning.Source.WalletConnect))
    }

    private fun disconnectAll() {
        val message = DialogMessage(
            title = resourceReference(R.string.wc_disconnect_all_alert_title),
            message = resourceReference(R.string.wc_disconnect_all_alert_desc),
            firstActionBuilder = {
                EventMessageAction(
                    title = resourceReference(R.string.common_disconnect),
                    onClick = {
                        // TODO(wc): disconnect all
                    },
                )
            },
            secondActionBuilder = { cancelAction() },
        )
        uiMessageSender.send(message)
    }

    private fun getInitialState(): WcConnectionsState {
        return WcConnectionsState(
            topAppBarConfig = WcConnectionsTopAppBarConfig(
                startButtonUM = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_back_24,
                    onIconClicked = router::pop,
                    enabled = true,
                ),
                disconnectAllItem = TangemDropdownMenuItem(
                    title = resourceReference(R.string.wc_disconnect_all),
                    textColorProvider = { TangemTheme.colors.text.warning },
                    onClick = ::disconnectAll,
                ),
            ),
            connections = persistentListOf(),
            onNewConnectionClick = ::openQrScanScreen,
        )
    }
}
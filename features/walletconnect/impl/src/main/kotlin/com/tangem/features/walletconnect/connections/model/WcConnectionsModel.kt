package com.tangem.features.walletconnect.connections.model

import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.usecase.WcSessionsUseCase
import com.tangem.domain.walletconnect.usecase.disconnect.WcDisconnectUseCase
import com.tangem.features.walletconnect.connections.components.WcConnectionsComponent
import com.tangem.features.walletconnect.connections.entity.WcConnectionsState
import com.tangem.features.walletconnect.connections.entity.WcConnectionsTopAppBarConfig
import com.tangem.features.walletconnect.connections.model.transformers.WcSessionsTransformer
import com.tangem.features.walletconnect.connections.routes.WcConnectionsBottomSheetConfig
import com.tangem.features.walletconnect.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class WcConnectionsModel @Inject constructor(
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
    private val wcSessionsUseCase: WcSessionsUseCase,
    private val wcDisconnectUseCase: WcDisconnectUseCase,
    private val wcPairService: WcPairService,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analytics: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<WcConnectionsComponent.Params>()
    val uiState: StateFlow<WcConnectionsState>
    field = MutableStateFlow<WcConnectionsState>(getInitialState())
    val bottomSheetNavigation: SlotNavigation<WcConnectionsBottomSheetConfig> = SlotNavigation()

    init {
        analytics.send(WcAnalyticEvents.ScreenOpened)
        listenQrUpdates()
        listenWcSessions()
    }

    private fun listenQrUpdates() {
        listenToQrScanningUseCase(SourceType.WALLET_CONNECT)
            .getOrElse { emptyFlow() }
            .onEach { wcUrl ->
                wcPairService.pair(
                    WcPairRequest(
                        userWalletId = params.userWalletId,
                        uri = wcUrl,
                        source = WcPairRequest.Source.QR,
                    ),
                )
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
                        openAppInfoModal = ::openAppInfoModal,
                    ),
                )
            }
            .launchIn(modelScope)
    }

    private fun openAppInfoModal(session: WcSession) {
        bottomSheetNavigation.activate(WcConnectionsBottomSheetConfig.ConnectedApp(session.sdkModel.topic))
    }

    private fun openQrScanScreen() {
        router.push(AppRoute.QrScanning(source = AppRoute.QrScanning.Source.WalletConnect))
    }

    private fun showDisconnectAllDialog() {
        val message = DialogMessage(
            title = resourceReference(R.string.wc_disconnect_all_alert_title),
            message = resourceReference(R.string.wc_disconnect_all_alert_desc),
            firstActionBuilder = {
                EventMessageAction(
                    title = resourceReference(R.string.common_disconnect),
                    onClick = ::disconnectAllSessions,
                )
            },
            secondActionBuilder = { cancelAction() },
        )
        uiMessageSender.send(message)
    }

    private fun disconnectAllSessions() {
        modelScope.launch {
            wcDisconnectUseCase.disconnectAll()
            uiMessageSender.send(SnackbarMessage(message = stringReference("All dApps disconnected")))
        }
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
                    onClick = ::showDisconnectAllDialog,
                ),
            ),
            connections = persistentListOf(),
            onNewConnectionClick = ::openQrScanScreen,
        )
    }
}
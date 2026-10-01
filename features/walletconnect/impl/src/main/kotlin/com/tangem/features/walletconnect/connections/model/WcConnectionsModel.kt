package com.tangem.features.walletconnect.connections.model

import android.net.Uri
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.common.routing.deeplink.DeeplinkConst.USER_CODE_KEY
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.mcp.McpRequestService
import com.tangem.domain.mcp.models.McpRequest
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.qrscanning.models.QrResultSource
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.walletconnect.WcAnalyticEvents
import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.model.WcPairRequest
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.usecase.WcSessionsUseCase
import com.tangem.domain.walletconnect.usecase.disconnect.WcDisconnectUseCase
import com.tangem.features.mcp.api.McpFeatureToggles
import com.tangem.features.walletconnect.connections.components.WcConnectionsComponent
import com.tangem.features.walletconnect.connections.entity.McpAgentInfo
import com.tangem.features.walletconnect.connections.entity.WcConnections
import com.tangem.features.walletconnect.connections.entity.WcConnectionsState
import com.tangem.features.walletconnect.connections.entity.WcConnectionsTopAppBarConfig
import com.tangem.features.walletconnect.connections.model.transformers.WcSessionsAccountModeTransformer
import com.tangem.features.walletconnect.connections.model.transformers.WcSessionsTransformer
import com.tangem.features.walletconnect.connections.routes.WcConnectionsBottomSheetConfig
import com.tangem.features.walletconnect.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.Transformer
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.toPersistentList
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
    private val multiAccountListSupplier: MultiAccountListSupplier,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val wcPairService: WcPairService,
    private val mcpRequestService: McpRequestService,
    private val mcpFeatureToggles: McpFeatureToggles,
    private val mcpRepository: McpRepository,
    override val dispatchers: CoroutineDispatcherProvider,
    analytics: AnalyticsEventHandler,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<WcConnectionsComponent.Params>()
    val uiState: StateFlow<WcConnectionsState>
        field = MutableStateFlow<WcConnectionsState>(getInitialState())
    val bottomSheetNavigation: SlotNavigation<WcConnectionsBottomSheetConfig> = SlotNavigation()

    init {
        analytics.send(WcAnalyticEvents.ScreenOpened())
        listenQrUpdates()
        listenWcSessions()
        if (mcpFeatureToggles.isMcpEnabled) loadMcpSessions()
    }

    private fun loadMcpSessions() {
        mcpRepository.getSessions()
            .onEach { sessions ->
                val agents = sessions.map { session ->
                    McpAgentInfo(
                        id = session.id,
                        // clientOrigin is the only verified part of a client's identity
                        name = session.clientOrigin ?: session.clientName ?: session.clientId,
                        subtitle = session.scopes.joinToString(),
                        onClick = {
                            bottomSheetNavigation.activate(WcConnectionsBottomSheetConfig.McpSession(session.id))
                        },
                    )
                }.toPersistentList()

                uiState.update { state -> state.copy(connectedAgents = agents) }
            }
            .launchIn(modelScope)
    }

    private fun listenQrUpdates() {
        listenToQrScanningUseCase.listen(SourceType.WALLET_CONNECT)
            .getOrElse { emptyFlow() }
            .onEach { result ->
                val mcpUserCode = parseMcpUserCode(result.qrCode)
                if (mcpUserCode != null) {
                    mcpRequestService.request(McpRequest.Pair(userCode = mcpUserCode))
                    return@onEach
                }
                val source = when (result.resultSource) {
                    QrResultSource.CLIPBOARD -> WcPairRequest.Source.CLIPBOARD
                    QrResultSource.CAMERA,
                    QrResultSource.GALLERY,
                    -> WcPairRequest.Source.QR
                }
                wcPairService.pair(
                    WcPairRequest(
                        userWalletId = params.userWalletId,
                        uri = result.qrCode,
                        source = source,
                        screen = WcPairRequest.Screen.WALLET_CONNECT,
                    ),
                )
            }
            .launchIn(modelScope)
    }

    /**
     * The screen lists connected MCP agents next to the dApp sessions, so its scanner accepts an MCP pairing link
     * too — anything else stays on the WalletConnect path.
     */
    private fun parseMcpUserCode(qrCode: String): String? {
        if (!mcpFeatureToggles.isMcpEnabled) return null

        val uri = runCatching { Uri.parse(qrCode) }.getOrNull() ?: return null
        if (uri.scheme != DeepLinkScheme.Tangem.scheme) return null
        if (uri.host != DeepLinkRoute.McpAuthorization.host) return null

        return runCatching { uri.getQueryParameter(USER_CODE_KEY) }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun listenWcSessions() {
        combine(
            flow = wcSessionsUseCase.invoke().distinctUntilChanged(),
            flow2 = isAccountsModeEnabledUseCase(),
            flow3 = multiAccountListSupplier(Unit),
            transform = { sessionsMap, isAccountMode, accountList ->
                val transformer: Transformer<WcConnectionsState> =
                    if (isAccountMode) {
                        WcSessionsAccountModeTransformer(
                            sessionsMap = sessionsMap,
                            accountList = accountList,
                            openAppInfoModal = ::openAppInfoModal,
                        )
                    } else {
                        WcSessionsTransformer(
                            sessionsMap = sessionsMap,
                            openAppInfoModal = ::openAppInfoModal,
                        )
                    }
                uiState.update(transformer)
            },
        )
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
            uiMessageSender.send(SnackbarMessage(message = resourceReference(R.string.wc_all_dapps_disconnected)))
        }
    }

    private fun getInitialState(): WcConnectionsState {
        return WcConnectionsState(
            topAppBarConfig = WcConnectionsTopAppBarConfig(
                startButtonUM = TopAppBarButtonUM.Back(
                    onBackClicked = router::pop,
                    enabled = true,
                ),
                disconnectAllItem = TangemDropdownMenuItem(
                    title = resourceReference(R.string.wc_disconnect_all),
                    textColor = themedColor { TangemTheme.colors.text.warning },
                    onClick = ::showDisconnectAllDialog,
                ),
            ),
            connections = WcConnections.Loading,
            onNewConnectionClick = ::openQrScanScreen,
        )
    }
}
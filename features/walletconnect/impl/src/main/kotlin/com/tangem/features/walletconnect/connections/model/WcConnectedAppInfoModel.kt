package com.tangem.features.walletconnect.connections.model

import androidx.compose.runtime.Stable
import com.domain.blockaid.models.dapp.CheckDAppResult
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.iconResId
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.usecase.WcSessionsUseCase
import com.tangem.domain.walletconnect.usecase.disconnect.WcDisconnectUseCase
import com.tangem.features.walletconnect.connections.components.WcConnectedAppInfoComponent
import com.tangem.features.walletconnect.connections.entity.VerifiedDAppState
import com.tangem.features.walletconnect.connections.entity.WcConnectedAppInfoUM
import com.tangem.features.walletconnect.connections.entity.WcNetworkInfoItem
import com.tangem.features.walletconnect.connections.entity.WcPrimaryButtonConfig
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
@ModelScoped
internal class WcConnectedAppInfoModel @Inject constructor(
    private val wcSessionsUseCase: WcSessionsUseCase,
    private val messageSender: UiMessageSender,
    private val disconnectUseCase: WcDisconnectUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params = paramsContainer.require<WcConnectedAppInfoComponent.Params>()
    val uiState: StateFlow<WcConnectedAppInfoUM?>
    field = MutableStateFlow<WcConnectedAppInfoUM?>(null)

    init {
        loadDAppInfo(params.topic)
    }

    private fun loadDAppInfo(topic: String) {
        modelScope.launch {
            val session = wcSessionsUseCase.findByTopic(topic = topic)
            if (session == null) {
                Timber.e("Can not find WcSession by topic: $topic")
                messageSender.send(SnackbarMessage(stringReference("Can not find WcSession by topic: $topic")))
                dismiss()
            } else {
                uiState.update { state ->
                    WcConnectedAppInfoUM(
                        appName = session.sdkModel.appMetaData.name,
                        appIcon = session.sdkModel.appMetaData.icons.firstOrNull().orEmpty(),
                        isVerified = session.securityStatus == CheckDAppResult.SAFE,
                        verifiedDAppState = extractVerifiedState(session),
                        appSubtitle = session.sdkModel.appMetaData.url,
                        walletName = session.wallet.name,
                        networks = session.networks
                            .map {
                                WcNetworkInfoItem.Required(
                                    id = it.rawId,
                                    icon = it.iconResId,
                                    name = it.name,
                                    symbol = it.currencySymbol,
                                )
                            }.toImmutableList(),
                        disconnectButtonConfig = WcPrimaryButtonConfig(
                            showProgress = false,
                            enabled = true,
                            onClick = { disconnect(session) },
                        ),
                        onDismiss = ::dismiss,
                    )
                }
            }
        }
    }

    private fun extractVerifiedState(session: WcSession): VerifiedDAppState {
        return if (session.securityStatus == CheckDAppResult.SAFE) {
            VerifiedDAppState.Verified(onVerifiedClick = {})
        } else {
            VerifiedDAppState.Unknown
        }
    }

    private fun disconnect(session: WcSession) {
        uiState.update { state ->
            state?.copy(
                disconnectButtonConfig = state.disconnectButtonConfig.copy(showProgress = true),
            )
        }
        modelScope.launch {
            disconnectUseCase.disconnect(session)
            messageSender.send(SnackbarMessage(message = stringReference("dApp disconnected")))
            dismiss()
        }
    }

    fun dismiss() {
        params.onDismiss()
    }
}
package com.tangem.features.walletconnect.connections.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.features.walletconnect.connections.entity.WcConnectionsState
import com.tangem.features.walletconnect.connections.ui.preview.WcConnectionsPreviewData
import com.tangem.features.walletconnect.impl.R
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@ModelScoped
internal class WcConnectionsModel @Inject constructor(
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val uiState: StateFlow<WcConnectionsState> = MutableStateFlow(
        WcConnectionsPreviewData.stateForModel(
            onBackClicked = router::pop,
            openQrScanScreen = ::openQrScanScreen,
            disconnectAll = ::disconnectAll,
        ),
    ).asStateFlow()

    private fun openQrScanScreen() {
        // TODO("[REDACTED_JIRA]")
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
}
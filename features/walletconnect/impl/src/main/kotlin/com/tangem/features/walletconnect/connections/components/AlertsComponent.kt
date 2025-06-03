package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.connections.ui.AlertsModalBottomSheet
import kotlinx.serialization.Serializable

internal class AlertsComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        params.alertType.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        AlertsModalBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = params.alertType.onDismiss,
                content = params.alertType,
            ),
        )
    }

    internal data class Params(val alertType: AlertType)

    @Immutable
    @Serializable
    internal sealed class AlertType : TangemBottomSheetConfigContent {
        abstract val onDismiss: () -> Unit

        @Serializable
        data class VerifiedDomain(val appName: String, override val onDismiss: () -> Unit) : AlertType()

        @Serializable
        data class UnknownDomain(val onConnect: () -> Unit, override val onDismiss: () -> Unit) : AlertType()

        @Serializable
        data class UnsafeDomain(val onConnect: () -> Unit, override val onDismiss: () -> Unit) : AlertType()

        @Serializable
        data class UnsupportedNetworks(val appName: String, override val onDismiss: () -> Unit) : AlertType()

        @Serializable
        data class WcDisconnected(override val onDismiss: () -> Unit) : AlertType()

        @Serializable
        data class UnknownError(val errorCode: Int, override val onDismiss: () -> Unit) : AlertType()

        @Serializable
        data class WrongCardSelected(override val onDismiss: () -> Unit) : AlertType()

        @Serializable
        data class ConnectionTimeout(val onTryAgain: () -> Unit, override val onDismiss: () -> Unit) : AlertType()

        @Serializable
        data class MaliciousTransaction(
            val descriptionMessage: String,
            val onConnect: () -> Unit,
            override val onDismiss: () -> Unit,
        ) : AlertType()
    }
}
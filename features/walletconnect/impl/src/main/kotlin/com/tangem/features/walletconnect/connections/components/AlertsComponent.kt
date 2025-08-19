package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.walletconnect.connections.ui.AlertsModalBottomSheet
import kotlinx.serialization.Serializable

internal class AlertsComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
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
        data class UnsupportedMethod(override val onDismiss: () -> Unit) : AlertType()

        @Serializable
        data class WcDisconnected(override val onDismiss: () -> Unit) : AlertType()

        @Serializable
        data class TangemUnsupportedNetwork(
            val network: String,
            override val onDismiss: () -> Unit,
        ) : AlertType()

        @Serializable
        data class RequiredAddNetwork(
            val network: String,
            override val onDismiss: () -> Unit,
        ) : AlertType()

        @Serializable
        data class RequiredReconnectWithNetwork(
            val network: String,
            override val onDismiss: () -> Unit,
        ) : AlertType()
    }
}
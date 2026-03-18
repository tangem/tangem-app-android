package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheet
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

internal class AlertsComponentV2(
    appComponentContext: AppComponentContext,
    private val messageUM: MessageBottomSheetUM,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        messageUM.onDismissRequest()
        router.pop()
    }

    @Composable
    override fun BottomSheet() {
        MessageBottomSheet(state = messageUM, onDismissRequest = ::dismiss)
    }
}
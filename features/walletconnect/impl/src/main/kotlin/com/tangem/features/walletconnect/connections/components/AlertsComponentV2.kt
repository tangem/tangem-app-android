package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetV2
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

internal class AlertsComponentV2(
    appComponentContext: AppComponentContext,
    private val messageUM: MessageBottomSheetUMV2,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        messageUM.onDismissRequest()
        router.pop()
    }

    @Composable
    override fun BottomSheet() {
        MessageBottomSheetV2(state = messageUM, onDismissRequest = ::dismiss)
    }
}
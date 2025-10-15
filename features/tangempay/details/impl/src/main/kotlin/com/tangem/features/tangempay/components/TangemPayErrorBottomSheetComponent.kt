package com.tangem.features.tangempay.components

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetV2
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

internal class TangemPayErrorBottomSheetComponent(
    appComponentContext: AppComponentContext,
    private val messageUM: MessageBottomSheetUMV2,
    private val onDismiss: () -> Unit,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        MessageBottomSheetV2(state = messageUM, onDismissRequest = ::dismiss)
    }
}
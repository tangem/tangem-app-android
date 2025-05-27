package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetV2
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import kotlinx.collections.immutable.ImmutableList

internal class AlertsComponentV2(
    appComponentContext: AppComponentContext,
    private val elements: ImmutableList<MessageBottomSheetUMV2.Element>,
    private val onDismissRequest: () -> Unit = {},
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        router.pop()
    }

    @Composable
    override fun BottomSheet() {
        MessageBottomSheetV2(
            state = MessageBottomSheetUMV2(elements = elements, onDismissRequest = onDismissRequest),
            onDismissRequest = ::dismiss,
        )
    }
}
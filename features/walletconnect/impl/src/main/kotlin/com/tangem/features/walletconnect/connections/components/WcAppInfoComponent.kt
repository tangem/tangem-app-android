package com.tangem.features.walletconnect.connections.components

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.connections.ui.WcAppInfoModalBottomSheet

internal class WcAppInfoComponent(
    private val appComponentContext: AppComponentContext,
    private val params: Params,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        WcAppInfoModalBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty, // todo(wc) will be fixed in next PR's
            ),
        )
    }

    data class Params(val onDismiss: () -> Unit)
}
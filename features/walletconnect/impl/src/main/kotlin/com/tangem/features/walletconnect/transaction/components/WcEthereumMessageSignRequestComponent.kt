package com.tangem.features.walletconnect.transaction.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.transaction.model.WcEthereumMessageSignRequestModel
import com.tangem.features.walletconnect.transaction.ui.sign.WcEthereumMessageSignRequestModalBottomSheet

internal class WcEthereumMessageSignRequestComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    private val model: WcEthereumMessageSignRequestModel = getOrCreateModel()

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val content by model.uiState.collectAsStateWithLifecycle()
        content?.let {
            WcEthereumMessageSignRequestModalBottomSheet(
                config = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = ::dismiss,
                    content = it,
                ),
            )
        }
    }

    data class Params(val onDismiss: () -> Unit)
}
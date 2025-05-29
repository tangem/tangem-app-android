package com.tangem.features.walletconnect.transaction.components.chain

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.transaction.model.WcAddNetworkModel
import com.tangem.features.walletconnect.transaction.ui.chain.WcAddEthereumChainModalBottomSheetContent

internal class WcAddNetworkComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcAddNetworkModel,
    private val transactionInfoOnClick: () -> Unit,
    private val onDismiss: () -> Unit,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        val content by model.uiState.collectAsStateWithLifecycle()

        if (content != null) {
            WcAddEthereumChainModalBottomSheetContent(
                state = content!!.transaction,
                onClickTransactionRequest = transactionInfoOnClick,
                onBack = router::pop,
                onDismiss = ::dismiss,
            )
        }
    }
}
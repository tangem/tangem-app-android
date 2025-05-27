package com.tangem.features.walletconnect.transaction.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.walletconnect.transaction.model.WcAddNetworkModel
import com.tangem.features.walletconnect.transaction.ui.chain.WcAddEthereumChainModalBottomSheetContent

internal class WcAddNetworkComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcAddNetworkModel,
    private val transactionInfoOnClick: () -> Unit,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        val content by model.uiState.collectAsStateWithLifecycle()

        if (content != null) {
            WcAddEthereumChainModalBottomSheetContent(
                state = content!!.transaction,
                onClickTransactionRequest = transactionInfoOnClick,
            )
        }
    }
}
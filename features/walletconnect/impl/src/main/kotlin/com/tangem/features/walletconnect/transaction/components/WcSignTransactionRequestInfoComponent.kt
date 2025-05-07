package com.tangem.features.walletconnect.transaction.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.walletconnect.transaction.model.WcSignTransactionModel
import com.tangem.features.walletconnect.transaction.ui.common.TransactionRequestInfoContent

internal class WcSignTransactionRequestInfoComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcSignTransactionModel,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        val content = model.uiState.collectAsStateWithLifecycle().value

        if (content != null) {
            TransactionRequestInfoContent(content.transactionRequestInfo)
        }
    }
}
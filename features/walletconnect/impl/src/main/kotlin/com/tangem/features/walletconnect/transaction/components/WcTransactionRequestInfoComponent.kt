package com.tangem.features.walletconnect.transaction.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM
import com.tangem.features.walletconnect.transaction.ui.common.TransactionRequestInfoContent

internal class WcTransactionRequestInfoComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcTransactionRequestInfoUM?,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        if (model != null) {
            TransactionRequestInfoContent(model)
        }
    }
}
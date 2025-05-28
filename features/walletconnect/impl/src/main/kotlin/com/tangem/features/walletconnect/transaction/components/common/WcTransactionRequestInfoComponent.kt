package com.tangem.features.walletconnect.transaction.components.common

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM
import com.tangem.features.walletconnect.transaction.ui.common.TransactionRequestInfoContent

internal class WcTransactionRequestInfoComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcTransactionRequestInfoUM?,
    private val onDismiss: () -> Unit,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        if (model != null) {
            TransactionRequestInfoContent(model, onBack = router::pop, onDismiss = ::dismiss)
        }
    }
}
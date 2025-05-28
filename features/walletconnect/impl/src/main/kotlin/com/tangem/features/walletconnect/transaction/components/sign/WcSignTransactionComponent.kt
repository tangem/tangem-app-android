package com.tangem.features.walletconnect.transaction.components.sign

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.transaction.model.WcSignTransactionModel
import com.tangem.features.walletconnect.transaction.ui.sign.WcSignTransactionModalBottomSheetContent

internal class WcSignTransactionComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcSignTransactionModel,
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
            WcSignTransactionModalBottomSheetContent(
                state = content!!.transaction,
                onClickTransactionRequest = transactionInfoOnClick,
                onBack = router::pop,
                onDismiss = ::dismiss,
            )
        }
    }
}
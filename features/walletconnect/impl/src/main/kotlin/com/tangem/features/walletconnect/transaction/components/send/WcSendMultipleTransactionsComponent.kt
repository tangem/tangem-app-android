package com.tangem.features.walletconnect.transaction.components.send

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.transaction.model.WcSendTransactionModel
import com.tangem.features.walletconnect.transaction.ui.send.WcSendMultipleTransactionsModalBottomSheet

internal class WcSendMultipleTransactionsComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcSendTransactionModel,
    private val onConfirm: () -> Unit,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        WcSendMultipleTransactionsModalBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = { model.popBack() },
                content = TangemBottomSheetConfigContent.Empty,
            ),
            onConfirm = onConfirm,
            onBack = { model.popBack() },
        )
    }
}
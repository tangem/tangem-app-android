package com.tangem.features.walletconnect.transaction.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.walletconnect.transaction.entity.common.WcCommonTransactionModel
import com.tangem.features.walletconnect.transaction.ui.common.TransactionRequestInfoContent

internal class WcTransactionRequestInfoComponent(
    private val appComponentContext: AppComponentContext,
    private val model: WcCommonTransactionModel,
) : AppComponentContext by appComponentContext, ComposableBottomSheetComponent {

    override fun dismiss() {
        model.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val content by model.uiState.collectAsStateWithLifecycle()
        if (content?.transactionRequestInfo != null) {
            TransactionRequestInfoContent(
                state = content!!.transactionRequestInfo,
                onBack = router::pop,
                onDismiss = ::dismiss,
            )
        }
    }
}
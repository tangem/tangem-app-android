package com.tangem.features.jointaccount.main.component

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.jointaccount.main.ui.AboutSignaturesBS

internal class AboutSignaturesComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {
    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        AboutSignaturesBS(threshold = params.threshold, total = params.total, onDismiss = ::dismiss)
    }

    data class Params(
        val threshold: Int,
        val total: Int,
        val onDismiss: () -> Unit,
    )
}
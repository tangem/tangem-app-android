package com.tangem.features.tangempay.cashback.impl

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

internal class CashbackBottomSheetComponent(
    appComponentContext: AppComponentContext,
    private val onDismiss: () -> Unit,
    private val content: @Composable () -> Unit,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        content()
    }
}
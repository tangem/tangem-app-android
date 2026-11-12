package com.tangem.features.collectibles.impl.main.sheet

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.collectibles.impl.main.ui.components.CollectiblesPaymentInProgressSheet

internal class CollectiblesPaymentInProgressSheetComponent(
    appComponentContext: AppComponentContext,
    private val onDismiss: () -> Unit,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        CollectiblesPaymentInProgressSheet(onCloseClick = onDismiss, onDismiss = onDismiss)
    }
}
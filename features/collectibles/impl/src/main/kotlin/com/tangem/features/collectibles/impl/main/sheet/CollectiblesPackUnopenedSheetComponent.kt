package com.tangem.features.collectibles.impl.main.sheet

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder.CollectiblesPackUnopenedSheet

internal class CollectiblesPackUnopenedSheetComponent(
    appComponentContext: AppComponentContext,
    private val onOpenPackClick: () -> Unit,
    private val onDismiss: () -> Unit,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        CollectiblesPackUnopenedSheet(onOpenPackClick = onOpenPackClick, onDismiss = onDismiss)
    }
}
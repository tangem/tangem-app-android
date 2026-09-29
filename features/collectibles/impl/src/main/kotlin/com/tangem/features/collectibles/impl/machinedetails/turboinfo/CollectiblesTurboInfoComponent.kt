package com.tangem.features.collectibles.impl.machinedetails.turboinfo

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

/** Explains Turbo mode, opened from the (i) next to its switch on the machine details screen. */
internal class CollectiblesTurboInfoComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        params.onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        CollectiblesTurboInfoBottomSheet(onDismiss = params.onDismiss)
    }

    data class Params(val onDismiss: () -> Unit)
}
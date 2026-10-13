package com.tangem.features.collectibles.impl.packopening.sheet

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.collectibles.impl.packopening.CollectiblesConfirmOrderBottomSheetConfig
import com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder.CollectiblesLowInventorySheet
import com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder.CollectiblesPackUnopenedSheet

/** A purchase outcome that needs the user's choice: a pack left unopened or low inventory. */
internal class CollectiblesConfirmOrderSheetComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        when (params.config) {
            is CollectiblesConfirmOrderBottomSheetConfig.PackUnopened -> params.onExplorePacks()
            CollectiblesConfirmOrderBottomSheetConfig.LowInventory -> params.onDismiss()
        }
    }

    @Composable
    override fun BottomSheet() {
        when (val config = params.config) {
            is CollectiblesConfirmOrderBottomSheetConfig.PackUnopened -> CollectiblesPackUnopenedSheet(
                onOpenPackClick = { params.onOpenPack(config.txId) },
                onDismiss = params.onExplorePacks,
            )
            CollectiblesConfirmOrderBottomSheetConfig.LowInventory -> CollectiblesLowInventorySheet(
                onEnableTurboModeClick = params.onEnableTurboMode,
                onExplorePacksClick = params.onExplorePacks,
                onDismiss = params.onDismiss,
            )
        }
    }

    data class Params(
        val config: CollectiblesConfirmOrderBottomSheetConfig,
        val onOpenPack: (txId: String) -> Unit,
        val onEnableTurboMode: () -> Unit,
        val onExplorePacks: () -> Unit,
        val onDismiss: () -> Unit,
    )
}
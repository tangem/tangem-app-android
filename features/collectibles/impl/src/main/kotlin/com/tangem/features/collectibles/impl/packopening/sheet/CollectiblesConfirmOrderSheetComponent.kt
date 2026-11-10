package com.tangem.features.collectibles.impl.packopening.sheet

import androidx.compose.runtime.Composable
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.collectibles.impl.packopening.CollectiblesConfirmOrderBottomSheetConfig
import com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder.CollectiblesAutoBuybackSheet
import com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder.CollectiblesLowInventorySheet
import com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder.CollectiblesPackUnopenedSheet
import com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder.CollectiblesRefundOnTheWaySheet

/** A purchase outcome to show: a pack left unopened, low inventory, a refund on its way or a card sold by Turbo mode. */
internal class CollectiblesConfirmOrderSheetComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        when (params.config) {
            is CollectiblesConfirmOrderBottomSheetConfig.PackUnopened -> params.onExplorePacks()
            CollectiblesConfirmOrderBottomSheetConfig.LowInventory -> params.onDismiss()
            CollectiblesConfirmOrderBottomSheetConfig.RefundOnTheWay -> params.onExplorePacks()
            is CollectiblesConfirmOrderBottomSheetConfig.AutoBuyback -> params.onOpenAgain()
        }
    }

    @Composable
    override fun BottomSheet() {
        when (val config = params.config) {
            is CollectiblesConfirmOrderBottomSheetConfig.PackUnopened -> CollectiblesPackUnopenedSheet(
                onOpenPackClick = { params.onOpenPack(config.txId) },
                onDismiss = whileShown(params.onExplorePacks),
            )
            CollectiblesConfirmOrderBottomSheetConfig.LowInventory -> CollectiblesLowInventorySheet(
                onEnableTurboModeClick = params.onEnableTurboMode,
                onExplorePacksClick = params.onExplorePacks,
                onDismiss = params.onDismiss,
            )
            CollectiblesConfirmOrderBottomSheetConfig.RefundOnTheWay -> CollectiblesRefundOnTheWaySheet(
                onCloseClick = params.onExplorePacks,
                onDismiss = whileShown(params.onExplorePacks),
            )
            is CollectiblesConfirmOrderBottomSheetConfig.AutoBuyback -> CollectiblesAutoBuybackSheet(
                amount = config.amount,
                price = config.price,
                onOpenAgainClick = params.onOpenAgain,
                onExplorePacksClick = params.onExplorePacks,
                onDismiss = whileShown(params.onOpenAgain),
            )
        }
    }

    /**
     * The modal reports its dismissal again once a button has already removed the sheet, at the end of its collapse
     * animation; navigating there would take the screen away from under the action the button started.
     */
    private fun whileShown(action: () -> Unit): () -> Unit = {
        if (lifecycle.state != Lifecycle.State.DESTROYED) action()
    }

    data class Params(
        val config: CollectiblesConfirmOrderBottomSheetConfig,
        val onOpenPack: (txId: String) -> Unit,
        val onEnableTurboMode: () -> Unit,
        val onOpenAgain: () -> Unit,
        val onExplorePacks: () -> Unit,
        val onDismiss: () -> Unit,
    )
}
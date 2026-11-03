package com.tangem.features.collectibles.impl.packopening.sheet

import androidx.compose.runtime.Composable
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.collectibles.impl.packopening.CollectiblesConfirmOrderBottomSheetConfig
import com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder.CollectiblesAutoBuybackSheet
import com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder.CollectiblesLowInventorySheet
import com.tangem.features.collectibles.impl.packopening.ui.components.confirmorder.CollectiblesPackUnopenedSheet

/** A purchase outcome that needs the user's choice: a pack left unopened, low inventory or a card sold by Turbo mode. */
internal class CollectiblesConfirmOrderSheetComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        when (params.config) {
            is CollectiblesConfirmOrderBottomSheetConfig.PackUnopened -> params.onExplorePacks()
            CollectiblesConfirmOrderBottomSheetConfig.LowInventory -> params.onDismiss()
            is CollectiblesConfirmOrderBottomSheetConfig.AutoBuyback -> params.onOpenAgain()
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
            is CollectiblesConfirmOrderBottomSheetConfig.AutoBuyback -> CollectiblesAutoBuybackSheet(
                amount = config.amount,
                price = config.price,
                onOpenAgainClick = params.onOpenAgain,
                onExplorePacksClick = params.onExplorePacks,
                onDismiss = ::onAutoBuybackDismiss,
            )
        }
    }

    /**
     * The modal reports its dismissal again once a button has already removed the sheet and navigated, at the end of
     * its collapse animation; a second navigation there would step one screen too far back.
     */
    private fun onAutoBuybackDismiss() {
        if (lifecycle.state != Lifecycle.State.DESTROYED) params.onOpenAgain()
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
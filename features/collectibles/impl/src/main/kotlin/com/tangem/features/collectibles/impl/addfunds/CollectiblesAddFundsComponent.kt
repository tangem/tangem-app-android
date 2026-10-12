package com.tangem.features.collectibles.impl.addfunds

import androidx.compose.runtime.Composable
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_grid_20
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.features.collectibles.impl.addfunds.ui.CollectiblesAddFundsBottomSheet
import kotlinx.collections.immutable.persistentListOf

/** The "Add funds" bottom sheet shown from the Collectibles main screen. */
internal class CollectiblesAddFundsComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    private val uiState = CollectiblesAddFundsUM(
        items = persistentListOf(
            CollectiblesAddFundsUM.Item(
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_logo_tangem_20,
                    tintReference = { TangemTheme.colors3.icon.brand },
                ),
                title = stringReference("From your Tangem Wallet"),
                description = stringReference("Use crypto from your wallet to top up your packs account"),
                onClick = params.callback::onFromTangemWalletClick,
            ),
            CollectiblesAddFundsUM.Item(
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_grid_20,
                    tintReference = { TangemTheme.colors3.icon.brand },
                ),
                title = stringReference("From another wallet or exchange"),
                description = stringReference("Send USDC Solana to your account's address"),
                onClick = params.callback::onFromAnotherWalletClick,
            ),
        ),
        onDismiss = params.callback::onDismissAddFunds,
    )

    override fun dismiss() {
        params.callback.onDismissAddFunds()
    }

    @Composable
    override fun BottomSheet() {
        CollectiblesAddFundsBottomSheet(state = uiState)
    }

    data class Params(val callback: Callback)

    interface Callback {
        fun onFromTangemWalletClick()
        fun onFromAnotherWalletClick()
        fun onDismissAddFunds()
    }
}
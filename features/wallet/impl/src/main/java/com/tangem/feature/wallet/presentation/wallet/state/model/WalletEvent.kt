package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class WalletEvent {

    /** Change wallet with animation. Using [prevIndex] and [newIndex] to calculate offset */
    data class ChangeWallet(val prevIndex: Int, val newIndex: Int) : WalletEvent()

    data class ChangeWalletWithoutScroll(val newIndex: Int) : WalletEvent()

    data class ShowError(val text: TextReference) : WalletEvent()

    data class ShowAlert(val state: WalletAlertState) : WalletEvent()

    data object CopyAddress : WalletEvent()

    data class RateApp(val onDismissClick: () -> Unit) : WalletEvent()

    data class DemonstrateWalletsScrollPreview(val direction: Direction) : WalletEvent() {

        enum class Direction {

            /** 1 -> 2 */
            LEFT,

            /** 1 <- 2 */
            RIGHT,
        }
    }
}
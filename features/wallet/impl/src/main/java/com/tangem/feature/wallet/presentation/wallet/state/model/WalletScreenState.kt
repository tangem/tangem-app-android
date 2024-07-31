package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.event.StateEvent
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class WalletScreenState(
    val onBackClick: () -> Unit,
    val topBarConfig: WalletTopBarConfig,
    val selectedWalletIndex: Int,
    val wallets: ImmutableList<WalletState>,
    val onWalletChange: (Int) -> Unit,
    val event: StateEvent<WalletEvent>,
    val isHidingMode: Boolean,
)

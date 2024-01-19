package com.tangem.feature.wallet.presentation.wallet.state2.model

import com.tangem.core.ui.event.StateEvent
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTopBarConfig
import kotlinx.collections.immutable.ImmutableList

internal data class WalletScreenState(
    val onBackClick: () -> Unit,
    val topBarConfig: WalletTopBarConfig,
    val selectedWalletIndex: Int,
    val wallets: ImmutableList<WalletState>,
    val onWalletChange: (Int) -> Unit,
    val event: StateEvent<WalletEvent>,
    val isHidingMode: Boolean,
)
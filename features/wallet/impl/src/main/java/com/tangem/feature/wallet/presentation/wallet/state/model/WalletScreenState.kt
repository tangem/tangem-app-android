package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.MutableState
import com.tangem.core.ui.event.StateEvent
import com.tangem.features.managetokens.navigation.ExpandableState
import kotlinx.collections.immutable.ImmutableList

internal data class WalletScreenState(
    val onBackClick: () -> Unit,
    val manageTokensExpandableState: MutableState<ExpandableState>,
    val topBarConfig: WalletTopBarConfig,
    val selectedWalletIndex: Int,
    val wallets: ImmutableList<WalletState>,
    val onWalletChange: (Int) -> Unit,
    val event: StateEvent<WalletEvent>,
    val isHidingMode: Boolean,
    val manageTokenRedesignToggle: Boolean,
)
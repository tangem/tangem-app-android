package com.tangem.managetokens.presentation.customtokens.state

import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.common.state.Event
import kotlinx.collections.immutable.ImmutableSet

internal data class AddCustomTokenState(
    val chooseWalletState: ChooseWalletState,
    val chooseNetworkState: ChooseNetworkState,
    val chooseDerivationState: ChooseDerivationState?,
    val tokenData: CustomTokenData?,
    val warnings: ImmutableSet<AddCustomTokenWarning>,
    val addTokenButton: ButtonState,
    val showChooseWalletScreen: Boolean = false,
    val event: StateEvent<Event> = consumedEvent(),
)
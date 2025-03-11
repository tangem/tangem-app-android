package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.event.triggeredEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState

internal class SendEventTransformer(
    private val event: WalletEvent,
    private val onConsume: () -> Unit,
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            event = triggeredEvent(data = event, onConsume = onConsume),
        )
    }
}
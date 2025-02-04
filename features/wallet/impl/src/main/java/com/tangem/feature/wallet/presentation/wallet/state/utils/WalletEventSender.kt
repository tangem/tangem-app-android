package com.tangem.feature.wallet.presentation.wallet.state.utils

import com.tangem.core.ui.event.consumedEvent
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SendEventTransformer
import javax.inject.Inject

/**
 * Component for sending events [WalletEvent] on WalletScreen
 *
 * @property stateHolder state holder for changing state
 *
[REDACTED_AUTHOR]
 */
internal class WalletEventSender @Inject constructor(
    private val stateHolder: WalletStateController,
) {

    fun send(event: WalletEvent) {
        stateHolder.update(transformer = SendEventTransformer(event = event, onConsume = ::onConsume))
    }

    fun onConsume() {
        stateHolder.update {
            it.copy(event = consumedEvent())
        }
    }
}
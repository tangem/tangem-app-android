package com.tangem.feature.wallet.presentation.wallet.state2.utils

import com.tangem.core.ui.event.consumedEvent
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SendEventTransformer
import javax.inject.Inject

/**
 * Component for sending events [WalletEvent] on WalletScreen
 *
 * @property stateHolder state holder for changing state
 *
 * @author Andrew Khokhlov on 24/11/2023
 */
internal class WalletEventSender @Inject constructor(
    private val stateHolder: WalletStateController,
) {

    fun send(event: WalletEvent) {
        stateHolder.update(transformer = SendEventTransformer(event = event, onConsume = ::onConsume))
    }

    private fun onConsume() {
        stateHolder.update {
            it.copy(event = consumedEvent())
        }
    }
}

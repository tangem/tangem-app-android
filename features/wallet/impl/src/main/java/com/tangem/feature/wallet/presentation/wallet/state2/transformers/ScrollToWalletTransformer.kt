package com.tangem.feature.wallet.presentation.wallet.state2.transformers

import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state2.WalletScreenState
import com.tangem.utils.Provider

internal class ScrollToWalletTransformer(
    private val index: Int,
    private val currentStateProvider: Provider<WalletScreenState>,
    private val stateUpdater: (WalletScreenState) -> Unit,
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            event = triggeredEvent(
                data = WalletEvent.ChangeWallet(index),
                onConsume = {
                    stateUpdater(
                        currentStateProvider().copy(
                            selectedWalletIndex = index,
                            event = consumedEvent(),
                        ),
                    )
                },
            ),
        )
    }
}
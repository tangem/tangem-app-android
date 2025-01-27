package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.utils.Provider

internal class ScrollToWalletTransformer(
    private val prevIndex: Int,
    private val newIndex: Int,
    private val currentStateProvider: Provider<WalletScreenState>,
    private val stateUpdater: (WalletScreenState) -> Unit,
    private val onConsume: () -> Unit = {},
) : WalletScreenStateTransformer {

    override fun transform(prevState: WalletScreenState): WalletScreenState {
        return prevState.copy(
            event = triggeredEvent(
                data = WalletEvent.ChangeWallet(prevIndex = prevIndex, newIndex = newIndex),
                onConsume = {
                    stateUpdater(
                        currentStateProvider().copy(event = consumedEvent()),
                    )

                    onConsume()
                },
            ),
        )
    }
}
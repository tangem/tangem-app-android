package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetWalletCardDropDownItemsTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class WalletDropDownItemsSubscriber(
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
) : WalletSubscriber() {
    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        // it might be simplified but left for possible expanding
        // possible will be removed at all after redesign
        return flow<Unit> {
            stateHolder.update(
                SetWalletCardDropDownItemsTransformer(
                    dropdownEnabled = true,
                    clickIntents = clickIntents,
                ),
            )
        }
    }
}
package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetBalancesAndLimitsTransformer
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

internal class VisaWalletBalancesAndLimitsSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntentsV2,
) : WalletSubscriber() {

    // TODO: Implement in [REDACTED_JIRA]
    override fun create(coroutineScope: CoroutineScope): Flow<*> = suspend {
        delay(timeMillis = 500)
        stateHolder.update(SetBalancesAndLimitsTransformer(userWallet, clickIntents))
    }.asFlow()
}
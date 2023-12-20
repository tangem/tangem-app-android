package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetWarningsTransformer
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

internal class MultiWalletWarningsSubscriber(
    private val userWalletId: UserWalletId,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntentsV2,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<ImmutableList<WalletNotification>> {
        return getMultiWalletWarningsFactory.create(clickIntents)
            .conflate()
            .distinctUntilChanged()
            .onEach { warnings ->
                stateHolder.update(SetWarningsTransformer(userWalletId, warnings))
                walletWarningsAnalyticsSender.send(warnings)
            }
    }
}
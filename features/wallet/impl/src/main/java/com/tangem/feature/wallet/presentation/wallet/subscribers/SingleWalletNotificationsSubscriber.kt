package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetSingleWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetWarningsTransformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

/**
[REDACTED_AUTHOR]
 */
internal class SingleWalletNotificationsSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val getSingleWalletWarningsFactory: GetSingleWalletWarningsFactory,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val clickIntents: WalletClickIntents,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<ImmutableList<WalletNotification>> {
        return getSingleWalletWarningsFactory.create(userWallet, clickIntents)
            .conflate()
            .distinctUntilChanged()
            .onEach { warnings ->
                val displayedState = stateHolder.getWalletState(userWallet.walletId)

                stateHolder.update(SetWarningsTransformer(userWallet.walletId, warnings))
                walletWarningsAnalyticsSender.send(displayedState, warnings)
            }
    }
}
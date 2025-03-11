package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetMultiWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetWarningsTransformer
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

internal class MultiWalletWarningsSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val getMultiWalletWarningsFactory: GetMultiWalletWarningsFactory,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<ImmutableList<WalletNotification>> {
        return getMultiWalletWarningsFactory.create(userWallet, clickIntents)
            .conflate()
            .distinctUntilChanged()
            .onEach { warnings ->
                val displayedState = stateHolder.getWalletState(userWallet.walletId)

                stateHolder.update(SetWarningsTransformer(userWallet.walletId, warnings))
                walletWarningsAnalyticsSender.send(displayedState, warnings)
                walletWarningsSingleEventSender.send(
                    userWalletId = userWallet.walletId,
                    displayedUiState = displayedState,
                    newWarnings = warnings,
                )
            }
    }
}
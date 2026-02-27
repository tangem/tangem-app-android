package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetSingleWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetWarningsTransformer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

/**
[REDACTED_AUTHOR]
 */
@Deprecated("Remove with main toggle [DesignFeatureToggles.isRedesignEnabled]")
internal class SingleWalletNotificationsSubscriber @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val stateController: WalletStateController,
    private val getSingleWalletWarningsFactory: GetSingleWalletWarningsFactory,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val clickIntents: WalletClickIntents,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<ImmutableList<WalletNotification>> {
        return getSingleWalletWarningsFactory.create(userWallet, clickIntents)
            .conflate()
            .distinctUntilChanged()
            .onEach { warnings ->
                val displayedState = stateController.getWalletState(userWallet.walletId)

                stateController.update(SetWarningsTransformer(userWallet.walletId, warnings, persistentListOf()))
                walletWarningsAnalyticsSender.send(displayedState, warnings)
            }
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): SingleWalletNotificationsSubscriber
    }
}
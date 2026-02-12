package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetWalletStackableNotificationsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.GetWalletWarningsFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetWarningsTransformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

internal class MultiWalletWarningsSubscriberV2(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val getWalletWarningsFactory: GetWalletWarningsFactory,
    private val getWalletStackableNotificationsFactory: GetWalletStackableNotificationsFactory,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<ImmutableList<WalletNotificationUM>> {
        return combine(
            flow = getWalletWarningsFactory.create(userWallet, clickIntents).conflate().distinctUntilChanged(),
            flow2 = getWalletStackableNotificationsFactory.create(userWallet, clickIntents).conflate()
                .distinctUntilChanged(),
        ) { notifications, stackableNotifications ->
            val displayedWalletUM = stateHolder.getWalletUM(userWallet.walletId)

            // Wait until the wallet appears in the list
            stateHolder.uiState.first {
                it.wallets.any { walletState -> walletState.walletCardState.id == userWallet.walletId }
            }

            stateHolder.update(
                SetWarningsTransformer(
                    userWalletId = userWallet.walletId,
                    warnings = persistentListOf(),
                    notifications = notifications,
                    stackedNotifications = stackableNotifications,
                ),
            )
            // todo redesign main
            // walletWarningsAnalyticsSender.send(displayedState, warnings)
            walletWarningsSingleEventSender.send(
                userWalletId = userWallet.walletId,
                displayedWalletUM = displayedWalletUM,
                newNotifications = notifications,
            )

            notifications
        }
    }
}
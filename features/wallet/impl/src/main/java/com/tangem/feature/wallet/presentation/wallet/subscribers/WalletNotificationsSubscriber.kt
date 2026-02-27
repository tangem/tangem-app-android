package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetWalletNotificationsCarouselFactory
import com.tangem.feature.wallet.presentation.wallet.domain.GetWalletNotificationsFactory
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetWarningsTransformer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class WalletNotificationsSubscriber @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val getWalletNotificationsFactory: GetWalletNotificationsFactory,
    private val getWalletNotificationsCarouselFactory: GetWalletNotificationsCarouselFactory,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<ImmutableList<WalletNotificationUM>> {
        return combine(
            flow = getWalletNotificationsFactory.create(userWallet, clickIntents).conflate().distinctUntilChanged(),
            flow2 = getWalletNotificationsCarouselFactory.create(userWallet, clickIntents).conflate()
                .distinctUntilChanged(),
        ) { notifications, notificationsCarousel ->
            val displayedWalletUM = stateHolder.getWalletUM(userWallet.walletId)

            // Wait until the wallet appears in the list
            stateHolder.uiState.first {
                it.wallets2.any { walletUM -> walletUM.walletsBalanceUM.id == userWallet.walletId }
            }

            // If there are notifications, we need to filter out the RateApp notification from stackable notifications,
            // because it should not be shown together with other notifications.
            val alteredNotificationsCarousel = if (notifications.isNotEmpty()) {
                notificationsCarousel.filterNot { it is WalletNotificationUM.RateApp }
            } else {
                notificationsCarousel
            }.toPersistentList()

            stateHolder.update(
                SetWarningsTransformer(
                    userWalletId = userWallet.walletId,
                    warnings = persistentListOf(),
                    notifications = notifications,
                    notificationsCarousel = alteredNotificationsCarousel,
                ),
            )

            val totalNotifications = (notifications + alteredNotificationsCarousel).toPersistentList()

            walletWarningsAnalyticsSender.send(displayedWalletUM, totalNotifications)
            walletWarningsSingleEventSender.send(
                userWalletId = userWallet.walletId,
                displayedWalletUM = displayedWalletUM,
                newNotifications = totalNotifications,
            )

            totalNotifications
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): WalletNotificationsSubscriber
    }
}
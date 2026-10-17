package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.stories.GetStoryContentUseCase
import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsAnalyticsSender
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletWarningsSingleEventSender
import com.tangem.feature.wallet.presentation.wallet.domain.GetPendingJointOperationsFlowFactory
import com.tangem.feature.wallet.presentation.wallet.domain.GetWalletNotificationsCarouselFactory
import com.tangem.feature.wallet.presentation.wallet.domain.GetWalletNotificationsFactory
import com.tangem.feature.wallet.presentation.wallet.domain.WalletNotificationsSnapshot
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetWarningsTransformer
import com.tangem.features.hotwallet.HardwareUpgradeLauncher
import com.tangem.features.promobanners.api.PromoBannersFeatureToggles
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class WalletNotificationsSubscriber @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val getWalletNotificationsFactory: GetWalletNotificationsFactory,
    private val getWalletNotificationsCarouselFactory: GetWalletNotificationsCarouselFactory,
    private val getPendingJointOperationsFlowFactory: GetPendingJointOperationsFlowFactory,
    private val walletWarningsAnalyticsSender: WalletWarningsAnalyticsSender,
    private val walletWarningsSingleEventSender: WalletWarningsSingleEventSender,
    private val getStoryContentUseCase: GetStoryContentUseCase,
    private val hardwareUpgradeLauncher: HardwareUpgradeLauncher,
    private val promoBannersFeatureToggles: PromoBannersFeatureToggles,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<ImmutableList<WalletNotificationUM>> {
        var wasUpgradePromoShown = false
        val isHidingPromoCards = promoBannersFeatureToggles.isHideMarketingBannersWhileSystemEnabled
        val promoCardsTriggerFlow = if (isHidingPromoCards) getPromoCardsTriggerFlow() else flowOf(null)

        return combine(
            flow = getWalletNotificationsFactory.create(userWallet, clickIntents).distinct(isHidingPromoCards),
            flow2 = getWalletNotificationsCarouselFactory.create(userWallet, clickIntents).distinct(isHidingPromoCards),
            flow3 = promoCardsTriggerFlow,
        ) { notificationsSnapshot, notificationsCarouselSnapshot, hasJointOperations ->
            val notifications = notificationsSnapshot.notifications
            val notificationsCarousel = notificationsCarouselSnapshot.notifications

            if (notificationsCarousel.any { it is WalletNotificationUM.YieldBoostPromo }) {
                coroutineScope.launch {
                    getStoryContentUseCase.invokeSync(
                        id = StoryContentIds.STORY_FIRST_TIME_YIELD_PROMO.id,
                        refresh = true,
                    )
                }
            }

            // Only when the banner appears: a failed prefetch isn't cached, so each emission would redo it.
            val isUpgradePromoShown = notifications.any { it is WalletNotificationUM.UpgradeHotWalletPromo }
            if (isUpgradePromoShown && !wasUpgradePromoShown) hardwareUpgradeLauncher.prefetch()
            wasUpgradePromoShown = isUpgradePromoShown

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
                    promoCardsBalanceState = if (isHidingPromoCards) {
                        minOf(notificationsSnapshot.balanceState, notificationsCarouselSnapshot.balanceState)
                    } else {
                        null
                    },
                    hasJointOperations = hasJointOperations,
                ),
            )

            val totalNotifications = if (isHidingPromoCards) {
                getShownNotifications()
            } else {
                (notifications + alteredNotificationsCarousel).toPersistentList()
            }

            walletWarningsAnalyticsSender.send(
                userWalletId = userWallet.walletId,
                displayedWalletUM = displayedWalletUM,
                newNotifications = totalNotifications,
            )
            walletWarningsSingleEventSender.send(
                userWalletId = userWallet.walletId,
                displayedWalletUM = displayedWalletUM,
                newNotifications = totalNotifications,
            )

            totalNotifications
        }
    }

    /** Without hiding the promo cards nothing reads the balance state, so only the notifications themselves count */
    private fun Flow<WalletNotificationsSnapshot>.distinct(
        isHidingPromoCards: Boolean,
    ): Flow<WalletNotificationsSnapshot> {
        return if (isHidingPromoCards) {
            conflate().distinctUntilChanged()
        } else {
            conflate().distinctUntilChangedBy(WalletNotificationsSnapshot::notifications)
        }
    }

    /**
     * Reruns the rule when a promo cards check becomes due or when the wallet's pending joint operations come and go,
     * and emits whether there are any. It is `null` until they are known: they come from the cache as soon as the
     * balance does, but along their own chain, and an open check passed without them would show the promo cards only
     * to hide them under the joint operation banners. Completing a check re-emits too; that run changes nothing.
     */
    private fun getPromoCardsTriggerFlow(): Flow<Boolean?> {
        val promoCardsCheckFlow = stateHolder.uiState
            .map { state ->
                val walletUM = state.wallets2.firstOrNull { it.walletsBalanceUM.id == userWallet.walletId }
                    as? WalletUM.Content

                walletUM?.promoCardsCheck
            }
            .distinctUntilChanged()

        val hasJointOperationsFlow = getPendingJointOperationsFlowFactory.create(userWallet)
            .map { operations -> operations.isNotEmpty() }
            .catch { error ->
                TangemLogger.e("Failed to get pending joint operations, the promo cards check goes without them", error)
                emit(false)
            }
            .onStart<Boolean?> { emit(null) }
            .distinctUntilChanged()

        return combine(promoCardsCheckFlow, hasJointOperationsFlow) { _, hasJointOperations -> hasJointOperations }
    }

    /** Whether the promo cards are shown is decided inside the state update, so what is shown is read back from it */
    private fun getShownNotifications(): ImmutableList<WalletNotificationUM> {
        val walletUM = stateHolder.getWalletUM(userWallet.walletId) ?: return persistentListOf()

        return (walletUM.notifications + walletUM.notificationsCarousel).toPersistentList()
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): WalletNotificationsSubscriber
    }
}
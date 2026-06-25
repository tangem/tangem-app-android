package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.common.TangemSiteUrlBuilder
import com.tangem.common.ui.notifications.NotificationId
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.settings.IsReadyToShowRateAppUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.yield.supply.promo.usecase.ShouldShowYieldBoostMainBannerUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetShouldShowMainPromoUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.utils.extensions.addIf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Factory for creating a list of notifications that can be shown on the wallet screen.
 * These notifications are not critical and can be stacked with each other.
 */
@Suppress("LongParameterList")
@ModelScoped
internal class GetWalletNotificationsCarouselFactory @Inject constructor(
    private val isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val notificationsRepository: NotificationsRepository,
    private val shouldShowYieldBoostMainBannerUseCase: ShouldShowYieldBoostMainBannerUseCase,
    private val yieldSupplyGetShouldShowMainPromoUseCase: YieldSupplyGetShouldShowMainPromoUseCase,
    private val yieldSupplyFeatureToggles: YieldSupplyFeatureToggles,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
) {
    fun create(userWallet: UserWallet, clickIntents: WalletClickIntents): Flow<ImmutableList<WalletNotificationUM>> {
        val isBalanceResolvedFlow = singleAccountStatusListSupplier(
            SingleAccountStatusListProducer.Params(userWallet.walletId),
        )
            .map { it.totalFiatBalance !is TotalFiatBalance.Loading }
            .distinctUntilChanged()

        return combine(
            flow = notificationsRepository.getShouldShowNotification(
                NotificationId.EnablePushesReminderNotification.key,
            ).distinctUntilChanged(),
            flow2 = isReadyToShowRateAppUseCase().distinctUntilChanged(),
            flow3 = getWalletsUseCase().conflate(),
            flow4 = yieldSupplyGetShouldShowMainPromoUseCase().distinctUntilChanged(),
            flow5 = isBalanceResolvedFlow,
        ) { showPushesNotification, showRateAppPromo, wallets, shouldShowYieldPromoLocal, isBalanceResolved ->

            buildList {
                addNoteMigrationNotification(userWallet, wallets, clickIntents)

                // isBalanceResolved gates Rate App on the balance leaving the loading state, so it does not
                // flash during loading and then get replaced once balance-dependent banners are resolved.
                addRateAppNotification(showRateAppPromo && isBalanceResolved, clickIntents)

                addPushNotification(
                    shouldShow = showPushesNotification,
                    isPushesAllowed = notificationsRepository.isUserAllowToSubscribeOnPushNotifications(),
                    clickIntents = clickIntents,
                )

                addYieldBoostBannerNotification(
                    userWallet = userWallet,
                    shouldShowLocal = shouldShowYieldPromoLocal,
                    clickIntents = clickIntents,
                )
            }.sortedBy { it.type.ordinal }.toImmutableList()
        }
    }

    private suspend fun MutableList<WalletNotificationUM>.addYieldBoostBannerNotification(
        userWallet: UserWallet,
        shouldShowLocal: Boolean,
        clickIntents: WalletClickIntents,
    ) {
        if (!shouldShowLocal) return
        if (!yieldSupplyFeatureToggles.isYieldPromoEnabled) return
        val shouldShow = shouldShowYieldBoostMainBannerUseCase(userWallet.walletId).getOrNull() == true
        if (!shouldShow) return
        add(
            WalletNotificationUM.YieldBoostPromo(
                onExploreClick = { clickIntents.onYieldBoostBannerClick(userWallet.walletId) },
                onLaterClick = { clickIntents.onDismissYieldBoostBanner(userWallet.walletId) },
            ),
        )
    }

    private fun MutableList<WalletNotificationUM>.addRateAppNotification(
        isReadyToShowRating: Boolean,
        clickIntents: WalletClickIntents,
    ) {
        addIf(isReadyToShowRating) {
            WalletNotificationUM.RateApp(
                onLikeClick = clickIntents::onLikeAppClick,
                onDislikeClick = clickIntents::onDislikeAppClick,
                onCloseClick = clickIntents::onCloseRateAppWarningClick,
            )
        }
    }

    private fun MutableList<WalletNotificationUM>.addNoteMigrationNotification(
        userWallet: UserWallet,
        userWallets: List<UserWallet>,
        clickIntents: WalletClickIntents,
    ) {
        val cardTypesResolver = (userWallet as? UserWallet.Cold)?.scanResponse?.cardTypesResolver

        val isUserHasWalletOrWallet2 = userWallets.filterIsInstance<UserWallet.Cold>().any { wallet ->
            val typesResolver = wallet.scanResponse.cardTypesResolver
            typesResolver.isTangemWallet() || typesResolver.isWallet2()
        }

        addIf(cardTypesResolver != null && cardTypesResolver.isTangemNote() && !isUserHasWalletOrWallet2) {
            WalletNotificationUM.NoteMigration(
                onClick = { clickIntents.onNoteMigrationButtonClick(TangemSiteUrlBuilder.NOTE_MIGRATION_URL) },
            )
        }
    }

    private fun MutableList<WalletNotificationUM>.addPushNotification(
        shouldShow: Boolean,
        isPushesAllowed: Boolean,
        clickIntents: WalletClickIntents,
    ) {
        addIf(shouldShow && !isPushesAllowed) {
            WalletNotificationUM.PushNotifications(
                onCloseClick = clickIntents::onDenyPermissions,
                onEnabledClick = clickIntents::onAllowPermissions,
            )
        }
    }
}
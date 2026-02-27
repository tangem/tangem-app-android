package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.common.TangemSiteUrlBuilder
import com.tangem.common.ui.notifications.NotificationId
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.promo.ShouldShowPromoWalletUseCase
import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.settings.IsReadyToShowRateAppUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.utils.extensions.addIf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/**
 * Factory for creating a list of notifications that can be shown on the wallet screen.
 * These notifications are not critical and can be stacked with each other.
 */
@ModelScoped
internal class GetWalletNotificationsCarouselFactory @Inject constructor(
    private val isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase,
    private val shouldShowPromoWalletUseCase: ShouldShowPromoWalletUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val notificationsRepository: NotificationsRepository,
) {
    fun create(userWallet: UserWallet, clickIntents: WalletClickIntents): Flow<ImmutableList<WalletNotificationUM>> {
        return combine(
            flow = shouldShowPromoWalletUseCase(userWalletId = userWallet.walletId, promoId = PromoId.YieldPromo)
                .distinctUntilChanged(),
            flow2 = notificationsRepository.getShouldShowNotification(
                NotificationId.EnablePushesReminderNotification.key,
            ).distinctUntilChanged(),
            flow3 = shouldShowPromoWalletUseCase(userWalletId = userWallet.walletId, promoId = PromoId.OnePlusOne)
                .distinctUntilChanged(),
            flow4 = isReadyToShowRateAppUseCase().distinctUntilChanged(),
            flow5 = getWalletsUseCase().conflate(),
        ) { showYieldPromo, showPushesNotification, showOnePlusOnePromo, showRateAppPromo, wallets ->

            buildList {
                addNoteMigrationNotification(userWallet, wallets, clickIntents)
                addRateAppNotification(showRateAppPromo, clickIntents)

                if (userWallet.isMultiCurrency) {
                    addOnePlusOnePromoNotification(clickIntents, showOnePlusOnePromo)
                    addYieldPromoNotification(clickIntents, showYieldPromo)
                }

                addPushNotification(
                    shouldShow = showPushesNotification,
                    isPushesAllowed = notificationsRepository.isUserAllowToSubscribeOnPushNotifications(),
                    clickIntents = clickIntents,
                )
            }.sortedBy { it.type.ordinal }.toImmutableList()
        }
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

    private fun MutableList<WalletNotificationUM>.addYieldPromoNotification(
        clickIntents: WalletClickIntents,
        shouldShowPromo: Boolean,
    ) {
        addIf(shouldShowPromo) {
            WalletNotificationUM.YieldPromo(
                onCloseClick = { clickIntents.onClosePromoClick(promoId = PromoId.YieldPromo) },
                onTermsAndConditionsClick = { clickIntents.onYieldPromoTermsAndConditionsClick() },
            )
        }
    }

    private fun MutableList<WalletNotificationUM>.addOnePlusOnePromoNotification(
        clickIntents: WalletClickIntents,
        shouldShowPromo: Boolean,
    ) {
        addIf(shouldShowPromo) {
            WalletNotificationUM.OnePlusOnePromo(
                onCloseClick = { clickIntents.onClosePromoClick(promoId = PromoId.OnePlusOne) },
                onClick = { clickIntents.onPromoClick(promoId = PromoId.OnePlusOne) },
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
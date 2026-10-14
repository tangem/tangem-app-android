package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.domain.NotificationsBalanceState
import com.tangem.feature.wallet.presentation.wallet.state.model.PromoCardsCheck
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import com.tangem.utils.annotations.RemoveWithToggle
import com.tangem.utils.logging.TangemLogger

/**
 * @property promoCardsBalanceState how far the balance behind the notifications has loaded; set only when the promo
 * cards give way to system cards (TWI-1841)
 */
internal class SetWarningsTransformer(
    userWalletId: UserWalletId,
    private val warnings: ImmutableList<WalletNotification>,
    private val notifications: ImmutableList<WalletNotificationUM> = persistentListOf(),
    private val notificationsCarousel: ImmutableList<WalletNotificationUM> = persistentListOf(),
    @RemoveWithToggle(
        toggleName = "TWI_1841_HIDE_MARKETING_BANNERS_WHILE_SYSTEM_ENABLED",
        description = "Make it non-null and drop the branch that sets the notifications as they are",
    )
    private val promoCardsBalanceState: NotificationsBalanceState? = null,
) : WalletStateTransformer(userWalletId) {

    override fun transform(prevState: WalletState): WalletState {
        return when (prevState) {
            is WalletState.MultiCurrency.Content -> prevState.copy(warnings = warnings)
            is WalletState.SingleCurrency.Content -> prevState.copy(warnings = warnings)
            is WalletState.MultiCurrency.Locked,
            is WalletState.SingleCurrency.Locked,
            -> {
                TangemLogger.w("Impossible to update notifications for locked wallet")
                prevState
            }
        }
    }

    override fun transform(walletUM: WalletUM): WalletUM {
        return when (walletUM) {
            is WalletUM.Content -> if (promoCardsBalanceState != null) {
                setNotificationsHidingPromoCards(walletUM, promoCardsBalanceState)
            } else {
                walletUM.copy(
                    notifications = notifications,
                    notificationsCarousel = notificationsCarousel,
                )
            }
            is WalletUM.Locked -> {
                TangemLogger.w("Impossible to update notifications for locked wallet")
                walletUM
            }
        }
    }

    /**
     * The promo cards hide as soon as a system card is shown, but come back only at a due [PromoCardsCheck].
     * The Tangem Pay promo is dropped from the list rather than from the UI, so its shown event is not sent.
     */
    private fun setNotificationsHidingPromoCards(
        walletUM: WalletUM.Content,
        balanceState: NotificationsBalanceState,
    ): WalletUM.Content {
        val hasSystemCard = notifications.any { it.isSystemCard() } ||
            notificationsCarousel.isNotEmpty() ||
            walletUM.jointOperationBanners.isNotEmpty()

        val isCheckDue = when (walletUM.promoCardsCheck) {
            PromoCardsCheck.None -> false
            PromoCardsCheck.OnOpen -> balanceState >= NotificationsBalanceState.Resolved
            PromoCardsCheck.AfterRefresh -> balanceState == NotificationsBalanceState.Settled
        }

        val arePromoCardsVisible = !hasSystemCard && (isCheckDue || walletUM.arePromoCardsVisible)

        return walletUM.copy(
            notifications = if (arePromoCardsVisible) {
                notifications
            } else {
                notifications.filterNot { it is WalletNotificationUM.TangemPayPromo }.toImmutableList()
            },
            notificationsCarousel = notificationsCarousel,
            arePromoCardsVisible = arePromoCardsVisible,
            promoCardsCheck = if (isCheckDue) PromoCardsCheck.None else walletUM.promoCardsCheck,
        )
    }

    /** Everything but the Tangem Pay cards: its promo is a promo card, its account warnings stay out of the rule */
    private fun WalletNotificationUM.isSystemCard(): Boolean {
        return this !is WalletNotificationUM.TangemPayPromo &&
            this !is WalletNotificationUM.TangemPayRefreshNeeded &&
            this !is WalletNotificationUM.TangemPayUnreachable
    }
}
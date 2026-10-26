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
 * @property hasJointOperations whether the wallet has pending joint operations, whose banners are system cards;
 * `null` until they are known. Read only with [promoCardsBalanceState]
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
    private val hasJointOperations: Boolean?,
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
     * "Get your first crypto" hides only the marketing block: a new user still sees the Tangem Pay promo, above it.
     * The Tangem Pay promo is dropped from the list rather than from the UI, so its shown event is not sent.
     */
    private fun setNotificationsHidingPromoCards(
        walletUM: WalletUM.Content,
        balanceState: NotificationsBalanceState,
    ): WalletUM.Content {
        val systemNotifications = notifications.filter { it.isSystemCard() }
        val shouldHideTangemPayPromo = systemNotifications.any { it !is WalletNotificationUM.AddFunds } ||
            notificationsCarousel.isNotEmpty() ||
            hasJointOperations == true
        val shouldHideMarketingBlock = shouldHideTangemPayPromo || systemNotifications.isNotEmpty()

        val isCheckDue = when (walletUM.promoCardsCheck) {
            PromoCardsCheck.None -> false
            PromoCardsCheck.OnOpen -> balanceState >= NotificationsBalanceState.Resolved && hasJointOperations != null
            PromoCardsCheck.AfterRefresh -> balanceState == NotificationsBalanceState.Settled
        }

        val isMarketingBlockVisible = !shouldHideMarketingBlock && (isCheckDue || walletUM.isMarketingBlockVisible)
        val isTangemPayPromoVisible = !shouldHideTangemPayPromo && (isCheckDue || walletUM.isTangemPayPromoVisible)

        return walletUM.copy(
            notifications = if (isTangemPayPromoVisible) {
                getNotificationsWithTangemPayPromoAboveAddFunds()
            } else {
                notifications.filterNot { it is WalletNotificationUM.TangemPayPromo }.toImmutableList()
            },
            notificationsCarousel = notificationsCarousel,
            isMarketingBlockVisible = isMarketingBlockVisible,
            isTangemPayPromoVisible = isTangemPayPromoVisible,
            promoCardsCheck = if (isCheckDue) PromoCardsCheck.None else walletUM.promoCardsCheck,
        )
    }

    /**
     * Everything but the Tangem Pay cards and the developer card: the promo is a promo card, the account warnings
     * stay out of the rule, and the developer card is on every test card, where it would leave no promo to check
     */
    private fun WalletNotificationUM.isSystemCard(): Boolean {
        return this !is WalletNotificationUM.TangemPayPromo &&
            this !is WalletNotificationUM.TangemPayRefreshNeeded &&
            this !is WalletNotificationUM.TangemPayUnreachable &&
            this !is WalletNotificationUM.DevCard
    }

    /** The notifications are sorted by type, which puts the promo below "Get your first crypto" */
    private fun getNotificationsWithTangemPayPromoAboveAddFunds(): ImmutableList<WalletNotificationUM> {
        val tangemPayPromo = notifications.firstOrNull { it is WalletNotificationUM.TangemPayPromo }
        val otherNotifications = notifications.filterNot { it is WalletNotificationUM.TangemPayPromo }
        val addFundsIndex = otherNotifications.indexOfFirst { it is WalletNotificationUM.AddFunds }
        if (tangemPayPromo == null || addFundsIndex == -1) return notifications

        return otherNotifications.toMutableList()
            .apply { add(addFundsIndex, tangemPayPromo) }
            .toImmutableList()
    }
}
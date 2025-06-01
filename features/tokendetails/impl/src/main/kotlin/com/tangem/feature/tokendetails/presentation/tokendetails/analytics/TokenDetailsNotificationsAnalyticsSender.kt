package com.tangem.feature.tokendetails.presentation.tokendetails.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.analytics.TokenSwapPromoAnalyticsEvent
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification

internal class TokenDetailsNotificationsAnalyticsSender(
    private val cryptoCurrency: CryptoCurrency,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun send(displayedUiState: TokenDetailsState, newNotifications: List<TokenDetailsNotification>) {
        if (newNotifications.isEmpty()) return
        if (displayedUiState.pullToRefreshConfig.isRefreshing) return

        val eventsFromNewWarnings = getEvents(newNotifications)
        val eventsFromDisplayedWarnings = getEvents(displayedUiState.notifications)
        val eventsToSend = eventsFromNewWarnings.filter { it !in eventsFromDisplayedWarnings }

        eventsToSend.forEach { event ->
            analyticsEventHandler.send(event)
        }
    }

    private fun getEvents(notifications: List<TokenDetailsNotification>): Set<AnalyticsEvent> {
        return notifications.mapNotNullTo(mutableSetOf(), ::getEvent)
    }

    private fun getEvent(notification: TokenDetailsNotification): AnalyticsEvent? {
        return when (notification) {
            is TokenDetailsNotification.NetworkFee,
            is TokenDetailsNotification.NetworkFeeWithBuyButton,
            -> TokenDetailsAnalyticsEvent.Notice.NotEnoughFee(
                currency = cryptoCurrency,
            )
            is TokenDetailsNotification.SwapPromo -> TokenSwapPromoAnalyticsEvent.NoticePromotionBanner(
                programName = TokenSwapPromoAnalyticsEvent.ProgramName.Empty, // Use it on new promo action
                source = AnalyticsParam.ScreensSources.Token,
            )
            is TokenDetailsNotification.KaspaIncompleteTransactionWarning -> TokenDetailsAnalyticsEvent.Notice.Reveal(
                currency = cryptoCurrency,
            )
            is TokenDetailsNotification.NetworksUnreachable,
            is TokenDetailsNotification.ExistentialDeposit,
            is TokenDetailsNotification.NetworksNoAccount,
            is TokenDetailsNotification.TopUpWithoutReserve,
            is TokenDetailsNotification.RentInfo,
            is TokenDetailsNotification.NetworkShutdown,
            is TokenDetailsNotification.HederaAssociateWarning,
            is TokenDetailsNotification.RequiredTrustlineWarning,
            is TokenDetailsNotification.KoinosMana,
            is TokenDetailsNotification.MigrationMaticToPol,
            is TokenDetailsNotification.UsedOutdatedData,
            -> null
        }
    }
}
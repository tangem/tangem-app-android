package com.tangem.feature.tokendetails.presentation.tokendetails.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics

internal class TokenDetailsNotificationsAnalyticsSender(
    private val cryptoCurrency: CryptoCurrency,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun send(
        displayedUiState: TokenDetailsState,
        newNotifications: List<TokenDetailsNotification>,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ) {
        if (newNotifications.isEmpty()) return
        if (displayedUiState.pullToRefreshConfig.isRefreshing) return

        val balance = AnalyticsParam.TokenBalanceState.fromAmount(cryptoCurrencyStatus.value.amount)
        val eventsFromNewWarnings = getEvents(newNotifications, balance)
        val eventsFromDisplayedWarnings = getEvents(displayedUiState.notifications, balance)
        val eventsToSend = eventsFromNewWarnings.filter { it !in eventsFromDisplayedWarnings }

        eventsToSend.forEach { event ->
            analyticsEventHandler.send(event)
        }
    }

    private fun getEvents(
        notifications: List<TokenDetailsNotification>,
        balance: AnalyticsParam.TokenBalanceState,
    ): Set<AnalyticsEvent> {
        return notifications.mapNotNullTo(mutableSetOf()) { getEvent(it, balance) }
    }

    private fun getEvent(
        notification: TokenDetailsNotification,
        balance: AnalyticsParam.TokenBalanceState,
    ): AnalyticsEvent? {
        return when (notification) {
            is TokenDetailsNotification.NetworkFee,
            is TokenDetailsNotification.NetworkFeeWithBuyButton,
            -> TokenDetailsAnalyticsEvent.Notice.NotEnoughFee(
                currency = cryptoCurrency,
                source = TokenDetailsAnalyticsEvent.Notice.NotEnoughFee.Source.DetailedScreen,
                balance = balance,
            )
            is TokenDetailsNotification.KaspaIncompleteTransactionWarning -> TokenDetailsAnalyticsEvent.Notice.Reveal(
                currency = cryptoCurrency,
            )
            is TokenDetailsNotification.YieldSupplyNotTransferedToAave -> YieldSupplyAnalytics.NoticeAmountNotDeposited(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            )
            is TokenDetailsNotification.DynamicAddressesFundsFound ->
                TokenDetailsAnalyticsEvent.Notice.AdditionalAddressesFound(
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
            is TokenDetailsNotification.MigrationClore,
            is TokenDetailsNotification.UsedOutdatedData,
            -> null
        }
    }
}
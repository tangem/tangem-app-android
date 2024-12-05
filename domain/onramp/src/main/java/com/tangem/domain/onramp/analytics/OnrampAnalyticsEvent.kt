package com.tangem.domain.onramp.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.onramp.model.OnrampSource

sealed class OnrampAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(
    category = "Onramp",
    event = event,
    params = params,
) {

    data class ScreenOpened(
        val source: OnrampSource,
        val tokenSymbol: String,
    ) : OnrampAnalyticsEvent(
        event = "Buy Screen Opened",
        params = mapOf(
            "Source" to source.analyticsName,
            "Token" to tokenSymbol,
        ),
    )

    data object SelectCurrencyScreenOpened : OnrampAnalyticsEvent(event = "Currency Screen Opened")

    data class FiatCurrencyChosen(
        private val currency: String,
    ) : OnrampAnalyticsEvent(
        event = "Currency Chosen",
        params = mapOf("Currency" to currency),
    )

    data object CloseOnramp : OnrampAnalyticsEvent(event = "Button - Close")

    data object SettingsOpened : OnrampAnalyticsEvent(event = "Onramp Settings Screen Opened")

    data object SelectResidenceOpened : OnrampAnalyticsEvent(event = "Residence Screen Opened")

    data class OnResidenceChosen(
        private val residence: String,
    ) : OnrampAnalyticsEvent(
        event = "Residence Chosen",
        params = mapOf("Residence" to residence),
    )

    data class ResidenceConfirmScreenOpened(
        private val residence: String,
    ) : OnrampAnalyticsEvent(
        event = "Residence Confirm Screen",
        params = mapOf("Residence" to residence),
    )

    data object OnResidenceChange : OnrampAnalyticsEvent(event = "Button - Change")

    data class OnResidenceConfirm(
        private val residence: String,
    ) : OnrampAnalyticsEvent(
        event = "Button - Confirm",
        params = mapOf("Residence" to residence),
    )

    data object ProvidersScreenOpened : OnrampAnalyticsEvent(event = "Providers Screen Opened")

    data class ProviderCalculated(
        private val providerName: String,
        private val tokenSymbol: String,
        private val paymentMethod: String,
    ) : OnrampAnalyticsEvent(
        event = "Provider Calculated",
        params = mapOf(
            "Provider" to providerName,
            "Token" to tokenSymbol,
            "Payment Method" to paymentMethod,
        ),
    )

    data object PaymentMethodsScreenOpened : OnrampAnalyticsEvent(event = "Payment Method Screen Opened")

    data class OnPaymentMethodChosen(
        private val paymentMethod: String,
    ) : OnrampAnalyticsEvent(
        event = "Method Chosen",
        params = mapOf(
            "Payment Method" to paymentMethod,
        ),
    )

    data class OnProviderChosen(
        private val providerName: String,
        private val tokenSymbol: String,
    ) : OnrampAnalyticsEvent(
        event = "Provider Chosen",
        params = mapOf(
            "Provider" to providerName,
            "Token" to tokenSymbol,
        ),
    )

    data class OnBuyClick(
        private val providerName: String,
        private val currency: String,
        private val tokenSymbol: String,
    ) : OnrampAnalyticsEvent(
        event = "Button - Buy",
        params = mapOf(
            "Provider" to providerName,
            "Currency" to currency,
            "Token" to tokenSymbol,
        ),
    )

    data class SuccessScreenOpened(
        private val providerName: String,
        private val currency: String,
        private val tokenSymbol: String,
        private val residence: String,
        private val paymentMethod: String,
    ) : OnrampAnalyticsEvent(
        event = "Buying In Progress Screen Opened",
        params = mapOf(
            "Token" to tokenSymbol,
            "Provider" to providerName,
            "Residence" to residence,
            "Currency" to currency,
            "Method" to paymentMethod,
        ),
    )

    data object MinAmountError : OnrampAnalyticsEvent(event = "Error - Min Amount")
    data object MaxAmountError : OnrampAnalyticsEvent(event = "Error - Max Amount")

    data class Errors(
        private val tokenSymbol: String,
        private val providerName: String,
        private val errorCode: String,
    ) : OnrampAnalyticsEvent(
        event = "Errors",
        params = mapOf(
            "Token" to tokenSymbol,
            "Provider" to providerName,
            "Error Code" to errorCode,
        ),
    )
}
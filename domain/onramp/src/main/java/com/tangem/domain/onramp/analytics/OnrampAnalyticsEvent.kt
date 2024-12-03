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
        val cryptoCurrency: String,
    ) : OnrampAnalyticsEvent(
        event = "Buy Screen Opened",
        params = mapOf(
            "Source" to source.analyticsName,
            "Token" to cryptoCurrency,
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
        private val cryptoCurrency: String,
    ) : OnrampAnalyticsEvent(
        event = "Provider Calculated",
        params = mapOf(
            "Provider" to providerName,
            "Token" to cryptoCurrency,
        ),
    )

    data object PaymentMethodsScreenOpened : OnrampAnalyticsEvent(event = "Payment Method Screen Opened")

    data object OnPaymentMethodChosen : OnrampAnalyticsEvent(event = "Method Chosen")

    data class OnProviderChosen(
        private val providerName: String,
        private val cryptoCurrency: String,
    ) : OnrampAnalyticsEvent(
        event = "Provider Chosen",
        params = mapOf(
            "Provider" to providerName,
            "Token" to cryptoCurrency,
        ),
    )

    data class OnBuyClick(
        private val providerName: String,
        private val currency: String,
        private val cryptoCurrency: String,
    ) : OnrampAnalyticsEvent(
        event = "Provider Chosen",
        params = mapOf(
            "Provider" to providerName,
            "Currency" to currency,
            "Token" to cryptoCurrency,
        ),
    )
}

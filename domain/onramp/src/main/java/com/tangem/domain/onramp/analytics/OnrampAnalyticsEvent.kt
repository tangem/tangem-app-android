package com.tangem.domain.onramp.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_DESCRIPTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.PAYMENT_METHOD
import com.tangem.core.analytics.models.AnalyticsParam.Key.PROVIDER
import com.tangem.core.analytics.models.AnalyticsParam.Key.RESIDENCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
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
            SOURCE to source.analyticsName,
            TOKEN_PARAM to tokenSymbol,
        ),
    )

    data object SelectCurrencyScreenOpened : OnrampAnalyticsEvent(event = "Currency Screen Opened")

    data class FiatCurrencyChosen(
        private val currency: String,
    ) : OnrampAnalyticsEvent(
        event = "Currency Chosen",
        params = mapOf("Currency Type" to currency),
    )

    data object CloseOnramp : OnrampAnalyticsEvent(event = "Button - Close")

    data object SettingsOpened : OnrampAnalyticsEvent(event = "Onramp Settings Screen Opened")

    data object SelectResidenceOpened : OnrampAnalyticsEvent(event = "Residence Screen Opened")

    data class OnResidenceChosen(
        private val residence: String,
    ) : OnrampAnalyticsEvent(
        event = "Residence Chosen",
        params = mapOf(RESIDENCE to residence),
    )

    data class ResidenceConfirmScreenOpened(
        private val residence: String,
    ) : OnrampAnalyticsEvent(
        event = "Residence Confirm Screen",
        params = mapOf(RESIDENCE to residence),
    )

    data object OnResidenceChange : OnrampAnalyticsEvent(event = "Button - Change")

    data class OnResidenceConfirm(
        private val residence: String,
    ) : OnrampAnalyticsEvent(
        event = "Button - Confirm",
        params = mapOf(RESIDENCE to residence),
    )

    data object ProvidersScreenOpened : OnrampAnalyticsEvent(event = "Providers Screen Opened")

    data class ProviderCalculated(
        private val providerName: String,
        private val tokenSymbol: String,
        private val paymentMethod: String,
    ) : OnrampAnalyticsEvent(
        event = "Provider Calculated",
        params = mapOf(
            PROVIDER to providerName,
            TOKEN_PARAM to tokenSymbol,
            PAYMENT_METHOD to paymentMethod,
        ),
    )

    data object PaymentMethodsScreenOpened : OnrampAnalyticsEvent(event = "Payment Method Screen Opened")

    data class OnPaymentMethodChosen(
        private val paymentMethod: String,
    ) : OnrampAnalyticsEvent(
        event = "Method Chosen",
        params = mapOf(
            PAYMENT_METHOD to paymentMethod,
        ),
    )

    data class OnProviderChosen(
        private val providerName: String,
        private val tokenSymbol: String,
    ) : OnrampAnalyticsEvent(
        event = "Provider Chosen",
        params = mapOf(
            PROVIDER to providerName,
            TOKEN_PARAM to tokenSymbol,
        ),
    )

    data class OnBuyClick(
        private val providerName: String,
        private val currency: String,
        private val tokenSymbol: String,
    ) : OnrampAnalyticsEvent(
        event = "Button - Buy",
        params = mapOf(
            PROVIDER to providerName,
            "Currency Type" to currency,
            TOKEN_PARAM to tokenSymbol,
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
            TOKEN_PARAM to tokenSymbol,
            PROVIDER to providerName,
            RESIDENCE to residence,
            "Currency Type" to currency,
            PAYMENT_METHOD to paymentMethod,
        ),
    )

    data object MinAmountError : OnrampAnalyticsEvent(event = "Error - Min Amount")
    data object MaxAmountError : OnrampAnalyticsEvent(event = "Error - Max Amount")

    data class Errors(
        private val tokenSymbol: String,
        private val errorCode: String,
        private val providerName: String?,
        private val paymentMethod: String?,
    ) : OnrampAnalyticsEvent(
        event = "Errors",
        params = buildMap {
            put(TOKEN_PARAM, tokenSymbol)
            providerName?.let { put(PROVIDER, providerName) }
            paymentMethod?.let { put(PAYMENT_METHOD, paymentMethod) }
            put(ERROR_CODE, errorCode)
        },
    )

    data class AppErrors(
        private val tokenSymbol: String,
        private val errorDescription: String,
    ) : OnrampAnalyticsEvent(
        event = "App Errors",
        params = mapOf(
            TOKEN_PARAM to tokenSymbol,
            ERROR_DESCRIPTION to errorDescription,
        ),
    )

    data class FastestBuyMethodClicked(
        private val tokenSymbol: String,
        private val providerName: String,
        private val paymentMethod: String,
    ) : OnrampAnalyticsEvent(
        event = "Fastest Method Clicked",
        params = mapOf(
            TOKEN_PARAM to tokenSymbol,
            PROVIDER to providerName,
            PAYMENT_METHOD to paymentMethod,
        ),
    )

    data class BestRateClicked(
        private val tokenSymbol: String,
        private val providerName: String,
        private val paymentMethod: String,
    ) : OnrampAnalyticsEvent(
        event = "Best Rate Clicked",
        params = mapOf(
            TOKEN_PARAM to tokenSymbol,
            PROVIDER to providerName,
            PAYMENT_METHOD to paymentMethod,
        ),
    )

    data object AllOffersClicked : OnrampAnalyticsEvent(
        event = "Button - All Offers",
        params = emptyMap(),
    )
}
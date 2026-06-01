package com.tangem.feature.tokendetails.presentation.tokendetails.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.tokendetails.presentation.tokendetails.analytics.utils.toAnalyticsParams

internal open class TokenDetailsAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category = "Token", event, params) {

    class DynamicAddressesScreenOpened(currency: CryptoCurrency) : TokenDetailsAnalyticsEvent(
        event = "Dynamic Addresses Screen Opened",
        params = currency.toAnalyticsParams(),
    )

    class ButtonEnableDynamicAddresses(currency: CryptoCurrency) : TokenDetailsAnalyticsEvent(
        event = "Button - Enable Dynamic Addresses",
        params = currency.toAnalyticsParams(),
    )

    class DynamicAddressesEnabled(currency: CryptoCurrency) : TokenDetailsAnalyticsEvent(
        event = "Dynamic Addresses Enabled",
        params = currency.toAnalyticsParams(),
    )

    class ButtonDisableDynamicAddresses(currency: CryptoCurrency) : TokenDetailsAnalyticsEvent(
        event = "Button - Disable Dynamic Addresses",
        params = currency.toAnalyticsParams(),
    )

    class DynamicAddressesDisabled(currency: CryptoCurrency) : TokenDetailsAnalyticsEvent(
        event = "Dynamic Addresses Disabled",
        params = currency.toAnalyticsParams(),
    )

    open class Notice(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : TokenDetailsAnalyticsEvent(event = "Notice - $event", params) {

        class NetworkUnreachable(currency: CryptoCurrency) : Notice(
            event = "Network Unreachable",
            params = currency.toAnalyticsParams(),
        )

        class NotEnoughFee(currency: CryptoCurrency, source: Source) : Notice(
            event = "Not Enough Fee",
            params = currency.toAnalyticsParams() + ("Source" to source.value),
        ) {
            enum class Source(val value: String) {
                DetailedScreen("Detailed Screen"),
                DynamicAddresses("Dynamic Addresses"),
            }
        }

        class Reveal(currency: CryptoCurrency) : Notice(
            event = "Reveal Transaction",
            params = currency.toAnalyticsParams(),
        )

        class DynamicAddressesUnavailable(currency: CryptoCurrency) : Notice(
            event = "Dynamic Addresses Unavailable",
            params = currency.toAnalyticsParams(),
        )

        class AdditionalAddressesFound(currency: CryptoCurrency) : Notice(
            event = "Additional Addresses Found",
            params = currency.toAnalyticsParams(),
        )
    }

    open class Error(
        event: String,
        params: Map<String, String> = emptyMap(),
    ) : TokenDetailsAnalyticsEvent(event = "Error - $event", params) {

        class DynamicAddressesUnavailable(currency: CryptoCurrency) : Error(
            event = "Dynamic Addresses Unavailable",
            params = currency.toAnalyticsParams(),
        )
    }
}
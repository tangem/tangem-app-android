package com.tangem.domain.onramp.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.OtelMetric
import com.tangem.domain.onramp.model.OnrampSource
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OnrampAnalyticsEventTest {

    @ParameterizedTest
    @MethodSource("events")
    fun `GIVEN event inputs WHEN event is created THEN id and complete payload match contract`(
        event: OnrampAnalyticsEvent,
        expectedId: String,
        expectedParams: Map<String, String>,
    ) {
        // Act
        val id = event.id
        val params = event.params

        // Assert
        assertThat(id).isEqualTo(expectedId)
        assertThat(params).isEqualTo(expectedParams)
    }

    @ParameterizedTest
    @CsvSource(
        "ACTION_BUTTONS,Main",
        "TOKEN_LONG_TAP,Long Tap",
        "TOKEN_DETAILS,Token",
        "MARKETS,Markets",
        "MARKETING_BANNER,Marketing Banner",
    )
    fun `GIVEN onramp source WHEN source events are created THEN source is mapped`(
        source: OnrampSource,
        expected: String,
    ) {
        // Act
        val opened = OnrampAnalyticsEvent.ScreenOpened(source, "BTC")
        val unsupported = OnrampAnalyticsEvent.NoticeBuyNotSupported(source, "BTC", "Ethereum")

        // Assert
        assertThat(opened.params).isEqualTo(mapOf("Source" to expected, "Token" to "BTC"))
        assertThat(unsupported.params).isEqualTo(
            mapOf("Source" to expected, "Token" to "BTC", "Blockchain" to "Ethereum"),
        )
    }

    @Test
    fun `GIVEN tracked events WHEN created THEN metric attributes and AppsFlyer contracts are preserved`() {
        // Act
        val opened = OnrampAnalyticsEvent.ScreenOpened(OnrampSource.MARKETS, "BTC")
        val success = OnrampAnalyticsEvent.SuccessScreenOpened("Provider", "USD", "BTC", "US", "Card")
        val errors = OnrampAnalyticsEvent.Errors("BTC", "E1", null, null)

        // Assert
        assertThat(opened).isInstanceOf(AppsFlyerIncludedEvent::class.java)
        assertThat(success).isInstanceOf(AppsFlyerIncludedEvent::class.java)
        assertThat(success.otelMetric).isEqualTo(OtelMetric.Counter(setOf("Provider", "Payment Method")))
        assertThat(errors.otelMetric).isEqualTo(
            OtelMetric.Counter(setOf("Error Code", "Token", "Provider", "Payment Method")),
        )
    }

    private fun events() = listOf(
        Arguments.of(
            OnrampAnalyticsEvent.ScreenOpened(OnrampSource.MARKETS, "BTC"),
            "[Onramp] Buy Screen Opened",
            mapOf("Source" to "Markets", "Token" to "BTC"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.SelectCurrencyScreenOpened(),
            "[Onramp] Currency Screen Opened",
            emptyMap<String, String>(),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.FiatCurrencyChosen("USD"),
            "[Onramp] Currency Chosen",
            mapOf("Currency Type" to "USD"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.CloseOnramp(),
            "[Onramp] Button - Close",
            emptyMap<String, String>(),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.SettingsOpened(),
            "[Onramp] Onramp Settings Screen Opened",
            emptyMap<String, String>(),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.SelectResidenceOpened(),
            "[Onramp] Residence Screen Opened",
            emptyMap<String, String>(),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.OnResidenceChosen("US"),
            "[Onramp] Residence Chosen",
            mapOf("Residence" to "US"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.ResidenceConfirmScreenOpened("US"),
            "[Onramp] Residence Confirm Screen",
            mapOf("Residence" to "US"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.OnResidenceChange(),
            "[Onramp] Button - Change",
            emptyMap<String, String>(),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.OnResidenceConfirm("US"),
            "[Onramp] Button - Confirm",
            mapOf("Residence" to "US"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.ProvidersScreenOpened(),
            "[Onramp] Providers Screen Opened",
            emptyMap<String, String>(),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.ProviderCalculated("Provider", "BTC", "Card"),
            "[Onramp] Provider Calculated",
            mapOf("Provider" to "Provider", "Token" to "BTC", "Payment Method" to "Card"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.PaymentMethodsScreenOpened(),
            "[Onramp] Payment Method Screen Opened",
            emptyMap<String, String>(),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.OnPaymentMethodChosen("Card"),
            "[Onramp] Method Chosen",
            mapOf("Payment Method" to "Card"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.OnProviderChosen("Provider", "BTC"),
            "[Onramp] Provider Chosen",
            mapOf("Provider" to "Provider", "Token" to "BTC"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.OnBuyClick("Provider", "USD", "BTC"),
            "[Onramp] Button - Buy",
            mapOf("Provider" to "Provider", "Currency Type" to "USD", "Token" to "BTC"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.SuccessScreenOpened("Provider", "USD", "BTC", "US", "Card"),
            "[Onramp] Buying In Progress Screen Opened",
            mapOf("Provider" to "Provider", "Currency Type" to "USD", "Token" to "BTC", "Residence" to "US", "Payment Method" to "Card"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.MinAmountError(),
            "[Onramp] Error - Min Amount",
            emptyMap<String, String>(),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.MaxAmountError(),
            "[Onramp] Error - Max Amount",
            emptyMap<String, String>(),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.Errors("BTC", "E1", "Provider", "Card"),
            "[Onramp] Errors",
            mapOf("Token" to "BTC", "Error Code" to "E1", "Provider" to "Provider", "Payment Method" to "Card"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.Errors("BTC", "E1", null, null),
            "[Onramp] Errors",
            mapOf("Token" to "BTC", "Error Code" to "E1"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.Errors("BTC", "E1", "Provider", null),
            "[Onramp] Errors",
            mapOf("Token" to "BTC", "Error Code" to "E1", "Provider" to "Provider"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.Errors("BTC", "E1", null, "Card"),
            "[Onramp] Errors",
            mapOf("Token" to "BTC", "Error Code" to "E1", "Payment Method" to "Card"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.AppErrors("BTC", "Invalid response"),
            "[Onramp] App Errors",
            mapOf("Token" to "BTC", "Error Description" to "Invalid response"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.FastestBuyMethodClicked("BTC", "Provider", "Card"),
            "[Onramp] Fastest Method Clicked",
            mapOf("Token" to "BTC", "Provider" to "Provider", "Payment Method" to "Card"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.BestRateClicked("BTC", "Provider", "Card"),
            "[Onramp] Best Rate Clicked",
            mapOf("Token" to "BTC", "Provider" to "Provider", "Payment Method" to "Card"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.RecentlyBuyClicked("BTC", "Provider", "Card"),
            "[Onramp] Recently Used Clicked",
            mapOf("Token" to "BTC", "Provider" to "Provider", "Payment Method" to "Card"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.AllOffersClicked(),
            "[Onramp] Button - All Offers",
            emptyMap<String, String>(),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.NoticeGeoRestriction("BTC", "Provider"),
            "[Onramp] Notice - Geo Restriction",
            mapOf("Token" to "BTC", "Provider" to "Provider"),
        ),
        Arguments.of(
            OnrampAnalyticsEvent.NoticeBuyNotSupported(OnrampSource.MARKETS, "BTC", "Ethereum"),
            "[Onramp] Notice - Buy Not Supported",
            mapOf("Source" to "Markets", "Token" to "BTC", "Blockchain" to "Ethereum"),
        ),
    )
}
package com.tangem.domain.onramp

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.OtelIncludedEvent
import com.tangem.domain.onramp.analytics.OnrampAnalyticsEvent
import com.tangem.domain.onramp.model.OnrampSource
import org.junit.jupiter.api.Test

internal class OnrampAnalyticsEventTest {

    @Test
    fun `WHEN events are created THEN they expose the onramp category and stable event ids`() {
        // Arrange
        val events = listOf(
            OnrampAnalyticsEvent.ScreenOpened(OnrampSource.MARKETS, "BTC"),
            OnrampAnalyticsEvent.SelectCurrencyScreenOpened(),
            OnrampAnalyticsEvent.FiatCurrencyChosen("USD"),
            OnrampAnalyticsEvent.CloseOnramp(),
            OnrampAnalyticsEvent.SettingsOpened(),
            OnrampAnalyticsEvent.SelectResidenceOpened(),
            OnrampAnalyticsEvent.OnResidenceChosen("US"),
            OnrampAnalyticsEvent.ResidenceConfirmScreenOpened("US"),
            OnrampAnalyticsEvent.OnResidenceChange(),
            OnrampAnalyticsEvent.OnResidenceConfirm("US"),
            OnrampAnalyticsEvent.ProvidersScreenOpened(),
            OnrampAnalyticsEvent.ProviderCalculated("Provider", "BTC", "Card"),
            OnrampAnalyticsEvent.PaymentMethodsScreenOpened(),
            OnrampAnalyticsEvent.OnPaymentMethodChosen("Card"),
            OnrampAnalyticsEvent.OnProviderChosen("Provider", "BTC"),
            OnrampAnalyticsEvent.OnBuyClick("Provider", "USD", "BTC"),
            OnrampAnalyticsEvent.SuccessScreenOpened("Provider", "USD", "BTC", "US", "Card"),
            OnrampAnalyticsEvent.MinAmountError(),
            OnrampAnalyticsEvent.MaxAmountError(),
            OnrampAnalyticsEvent.Errors("BTC", "E1", "Provider", "Card"),
            OnrampAnalyticsEvent.AppErrors("BTC", "Invalid response"),
            OnrampAnalyticsEvent.FastestBuyMethodClicked("BTC", "Provider", "Card"),
            OnrampAnalyticsEvent.BestRateClicked("BTC", "Provider", "Card"),
            OnrampAnalyticsEvent.RecentlyBuyClicked("BTC", "Provider", "Card"),
            OnrampAnalyticsEvent.AllOffersClicked(),
            OnrampAnalyticsEvent.NoticeGeoRestriction("BTC", "Provider"),
            OnrampAnalyticsEvent.NoticeBuyNotSupported(OnrampSource.MARKETS, "BTC", "Ethereum"),
        )

        // Act
        val ids = events.map { it.id }

        // Assert
        assertThat(events.map { it.category }.toSet()).containsExactly("Onramp")
        assertThat(ids).containsExactlyElementsIn(
            listOf(
                "[Onramp] Buy Screen Opened",
                "[Onramp] Currency Screen Opened",
                "[Onramp] Currency Chosen",
                "[Onramp] Button - Close",
                "[Onramp] Onramp Settings Screen Opened",
                "[Onramp] Residence Screen Opened",
                "[Onramp] Residence Chosen",
                "[Onramp] Residence Confirm Screen",
                "[Onramp] Button - Change",
                "[Onramp] Button - Confirm",
                "[Onramp] Providers Screen Opened",
                "[Onramp] Provider Calculated",
                "[Onramp] Payment Method Screen Opened",
                "[Onramp] Method Chosen",
                "[Onramp] Provider Chosen",
                "[Onramp] Button - Buy",
                "[Onramp] Buying In Progress Screen Opened",
                "[Onramp] Error - Min Amount",
                "[Onramp] Error - Max Amount",
                "[Onramp] Errors",
                "[Onramp] App Errors",
                "[Onramp] Fastest Method Clicked",
                "[Onramp] Best Rate Clicked",
                "[Onramp] Recently Used Clicked",
                "[Onramp] Button - All Offers",
                "[Onramp] Notice - Geo Restriction",
                "[Onramp] Notice - Buy Not Supported",
            ),
        ).inOrder()
    }

    @Test
    fun `WHEN event parameters are built THEN optional error parameters are omitted and source is mapped`() {
        // Act
        val event = OnrampAnalyticsEvent.Errors(
            tokenSymbol = "BTC",
            errorCode = "E1",
            providerName = null,
            paymentMethod = null,
        )
        val unsupported = OnrampAnalyticsEvent.NoticeBuyNotSupported(
            source = OnrampSource.TOKEN_LONG_TAP,
            tokenSymbol = "BTC",
            blockchain = "Ethereum",
        )

        // Assert
        assertThat(event.params).containsEntry("Token", "BTC")
        assertThat(event.params).containsEntry("Error Code", "E1")
        assertThat(unsupported.params).containsExactly(
            "Source", "Long Tap",
            "Token", "BTC",
            "Blockchain", "Ethereum",
        )
    }

    @Test
    fun `GIVEN tracked events WHEN created THEN analytics marker contracts are preserved`() {
        // Act
        val screenOpened = OnrampAnalyticsEvent.ScreenOpened(OnrampSource.MARKETS, "BTC")
        val success = OnrampAnalyticsEvent.SuccessScreenOpened("Provider", "USD", "BTC", "US", "Card")
        val errors = OnrampAnalyticsEvent.Errors("BTC", "E1", null, null)

        // Assert
        assertThat(screenOpened is AppsFlyerIncludedEvent).isTrue()
        assertThat(success is AppsFlyerIncludedEvent).isTrue()
        assertThat(success is OtelIncludedEvent).isTrue()
        assertThat(errors is OtelIncludedEvent).isTrue()
    }
}
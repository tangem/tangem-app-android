package com.tangem.common.routing.deeplink

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketingDeeplinkTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun resolveMarketingDeeplink(model: ResolveModel) {
        // Act
        val actual = resolveMarketingDeeplink(model.link)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    internal data class ResolveModel(val link: String, val expected: MarketingDeeplink)

    private fun provideTestModels() = listOf(
        // tangem:// swap/buy -> contextual
        ResolveModel(link = "tangem://swap", expected = MarketingDeeplink.SWAP),
        ResolveModel(link = "tangem://buy", expected = MarketingDeeplink.BUY),
        ResolveModel(link = "tangem://swap?foo=bar", expected = MarketingDeeplink.SWAP),
        ResolveModel(link = "tangem://buy/extra", expected = MarketingDeeplink.BUY),
        ResolveModel(link = "TANGEM://swap", expected = MarketingDeeplink.SWAP),
        // tangem:// other hosts -> external
        ResolveModel(link = "tangem://token", expected = MarketingDeeplink.EXTERNAL),
        ResolveModel(link = "tangem://promo", expected = MarketingDeeplink.EXTERNAL),
        // https T&S links -> external (iOS treats these as .link too)
        ResolveModel(link = "https://tangem.com/swap", expected = MarketingDeeplink.EXTERNAL),
        ResolveModel(link = "https://tangem.com/buy", expected = MarketingDeeplink.EXTERNAL),
        ResolveModel(link = "https://tangem.com/promo/summer", expected = MarketingDeeplink.EXTERNAL),
        // other schemes / garbage -> external
        ResolveModel(link = "wc://connect", expected = MarketingDeeplink.EXTERNAL),
        ResolveModel(link = "not a uri", expected = MarketingDeeplink.EXTERNAL),
        ResolveModel(link = "", expected = MarketingDeeplink.EXTERNAL),
    )
}
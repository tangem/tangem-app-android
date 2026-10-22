package com.tangem.features.polymarket.impl.common

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketResolutionSourceTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a source WHEN formatted THEN empty values hide and only web URLs become links`(model: SourceModel) {
        // Act
        val actual = model.source.toResolutionSourceUM()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        SourceModel(null, null),
        SourceModel("", null),
        SourceModel(" \n\t ", null),
        SourceModel("FIFA", PolymarketResolutionSourceUM(text = "FIFA", url = null)),
        SourceModel(" FIFA ", PolymarketResolutionSourceUM(text = "FIFA", url = null)),
        SourceModel(
            "https://www.binance.com/en/trade/BTC_USDT?type=spot#price",
            PolymarketResolutionSourceUM(
                text = "binance.com",
                url = "https://www.binance.com/en/trade/BTC_USDT?type=spot#price",
            ),
        ),
        SourceModel(
            " https://www.twitch.tv/ESLCS ",
            PolymarketResolutionSourceUM(text = "twitch.tv", url = "https://www.twitch.tv/ESLCS"),
        ),
        SourceModel("http://example.com", PolymarketResolutionSourceUM("example.com", "http://example.com")),
        SourceModel("https://", PolymarketResolutionSourceUM("https://", null)),
        SourceModel("Not a URL", PolymarketResolutionSourceUM("Not a URL", null)),
        SourceModel("javascript:alert(1)", PolymarketResolutionSourceUM("javascript:alert(1)", null)),
        SourceModel("file:///tmp/result", PolymarketResolutionSourceUM("file:///tmp/result", null)),
    )

    internal data class SourceModel(val source: String?, val expected: PolymarketResolutionSourceUM?)
}
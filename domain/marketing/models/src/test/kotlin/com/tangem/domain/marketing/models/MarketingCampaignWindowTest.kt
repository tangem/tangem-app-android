package com.tangem.domain.marketing.models

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Instant

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MarketingCampaignWindowTest {

    @ParameterizedTest
    @ProvideTestModels
    fun isActiveAt(model: Model) {
        // Act
        val actual = campaign(model.startAt, model.endAt).isActiveAt(NOW)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN unparseable bounds WHEN isActiveAt THEN invalid values reported`() {
        // Arrange
        val invalid = mutableListOf<String>()

        // Act
        campaign("garbage", "2026-13-99").isActiveAt(NOW) { invalid += it }

        // Assert
        assertThat(invalid).containsExactly("garbage", "2026-13-99").inOrder()
    }

    data class Model(val startAt: String?, val endAt: String?, val expected: Boolean)

    private fun provideTestModels() = listOf(
        Model(startAt = "2026-08-01T12:00:01Z", endAt = null, expected = false), // future start
        Model(startAt = null, endAt = "2026-08-01T11:59:59Z", expected = false), // past end
        Model(startAt = "2026-07-01T00:00:00Z", endAt = "2026-09-01T00:00:00Z", expected = true), // inside
        Model(startAt = "2026-08-01T12:00:00Z", endAt = null, expected = true), // exactly at start
        Model(startAt = null, endAt = "2026-08-01T12:00:00Z", expected = true), // exactly at end
        Model(startAt = "2026-07-01T00:00:00Z", endAt = null, expected = true), // start only
        Model(startAt = null, endAt = "2026-09-01T00:00:00Z", expected = true), // end only
        Model(startAt = null, endAt = null, expected = true), // no bounds
        Model(startAt = "2026-08-01T12:00:00.001Z", endAt = null, expected = false), // fractional, just after now
        Model(startAt = null, endAt = "2026-08-01T11:59:59.999Z", expected = false), // fractional, just before now
        Model(startAt = null, endAt = "2026-08-01T12:00:00.000Z", expected = true), // fractional, equals now
        Model(startAt = "garbage", endAt = null, expected = true), // unparseable start ignored
        Model(startAt = null, endAt = "garbage", expected = true), // unparseable end ignored
        Model(startAt = "garbage", endAt = "2026-08-01T11:59:59Z", expected = false), // valid bound still applies
    )

    private fun campaign(startAt: String?, endAt: String?) = MarketingCampaign(
        id = 1,
        type = MarketingScreenType.SWAP,
        priority = 1,
        startAt = startAt,
        endAt = endAt,
        minAmount = null,
        maxAmount = null,
        providerIds = null,
        banner = MarketingBanner(
            uiType = MarketingBanner.UiType.STANDALONE, text = null, iconUrl = null,
            iconAlign = null, bgColor = null, deeplink = null, isDismissible = true,
        ),
        targets = emptyList(),
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-08-01T12:00:00Z")
    }
}
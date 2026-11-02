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
    fun `GIVEN campaign with future start and end WHEN nextWindowTransition THEN returns start`() {
        // Arrange
        val campaign = campaign(startAt = "2026-08-01T12:05:00Z", endAt = "2026-08-01T12:15:00Z")

        // Act
        val next = campaign.nextWindowTransition(NOW)

        // Assert
        assertThat(next).isEqualTo(Instant.parse("2026-08-01T12:05:00Z"))
    }

    @Test
    fun `GIVEN active campaign with future end WHEN nextWindowTransition THEN returns end plus 1ms`() {
        // Arrange
        val campaign = campaign(startAt = "2026-08-01T11:00:00Z", endAt = "2026-08-01T12:05:00Z")

        // Act
        val next = campaign.nextWindowTransition(NOW)

        // Assert
        assertThat(next).isEqualTo(Instant.parse("2026-08-01T12:05:00.001Z"))
    }

    @Test
    fun `GIVEN expired campaign WHEN nextWindowTransition THEN returns null`() {
        // Arrange
        val campaign = campaign(startAt = "2026-08-01T10:00:00Z", endAt = "2026-08-01T11:59:59Z")

        // Act
        val next = campaign.nextWindowTransition(NOW)

        // Assert
        assertThat(next).isNull()
    }

    @Test
    fun `GIVEN campaigns list WHEN nextWindowTransition THEN returns earliest future transition`() {
        // Arrange
        val c1 = campaign(startAt = "2026-08-01T12:10:00Z", endAt = "2026-08-01T12:20:00Z")
        val c2 = campaign(startAt = "2026-08-01T11:00:00Z", endAt = "2026-08-01T12:05:00Z")

        // Act
        val next = listOf(c1, c2).nextWindowTransition(NOW)

        // Assert — c2's end is at 12:05:00.001Z which is earlier than c1's start at 12:10:00Z
        assertThat(next).isEqualTo(Instant.parse("2026-08-01T12:05:00.001Z"))
    }

    @Test
    fun `GIVEN invalid dates WHEN validateDates THEN reported`() {
        // Arrange
        val invalid = mutableListOf<String>()

        // Act
        campaign("not-a-date", "bad-end").validateDates { invalid += it }

        // Assert
        assertThat(invalid).containsExactly("not-a-date", "bad-end").inOrder()
    }

    @Test
    fun `GIVEN campaign without bounds WHEN nextWindowTransition THEN returns null`() {
        // Act
        val next = campaign(startAt = null, endAt = null).nextWindowTransition(NOW)

        // Assert
        assertThat(next).isNull()
    }

    @Test
    fun `GIVEN empty list WHEN nextWindowTransition THEN returns null`() {
        // Act
        val next = emptyList<MarketingCampaign>().nextWindowTransition(NOW)

        // Assert
        assertThat(next).isNull()
    }

    @Test
    fun `GIVEN valid dates WHEN validateDates THEN nothing reported`() {
        // Arrange
        val invalid = mutableListOf<String>()

        // Act
        campaign("2026-08-01T11:00:00Z", null).validateDates { invalid += it }

        // Assert
        assertThat(invalid).isEmpty()
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
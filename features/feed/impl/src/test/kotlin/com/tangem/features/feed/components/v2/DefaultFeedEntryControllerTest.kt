package com.tangem.features.feed.components.v2

import com.google.common.truth.Truth.assertThat
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.features.feed.v2.FeedSection
import org.junit.jupiter.api.Test

/**
 * The slot outlives its publisher on purpose — a deeplink normally lands before the hub exists — so
 * what is worth guarding is that it is spent exactly once, and that an identical repeat request still
 * lands.
 */
internal class DefaultFeedEntryControllerTest {

    private val controller = DefaultFeedEntryController()

    @Test
    fun `GIVEN a section WHEN requested THEN it is parked`() {
        // Act
        controller.request(FeedEntryRequest(section = FeedSection.Crypto))

        // Assert
        assertThat(controller.pendingEntry.value).isEqualTo(FeedEntryRequest(section = FeedSection.Crypto))
    }

    @Test
    fun `GIVEN nothing at all WHEN requested THEN the hub is still asked to open`() {
        // Act
        controller.request(FeedEntryRequest())

        // Assert - a bare deeplink opens the shtorka on its default section
        assertThat(controller.pendingEntry.value).isEqualTo(FeedEntryRequest())
    }

    @Test
    fun `GIVEN a parked request WHEN consumed THEN the slot is emptied`() {
        // Arrange
        controller.request(FeedEntryRequest(section = FeedSection.Crypto))

        // Act
        controller.consumeEntry()

        // Assert
        assertThat(controller.pendingEntry.value).isNull()
    }

    @Test
    fun `GIVEN a consumed request WHEN the identical one arrives again THEN it is parked again`() {
        // Arrange
        val request = FeedEntryRequest(section = FeedSection.Earn)
        controller.request(request)
        controller.consumeEntry()

        // Act
        controller.request(request)

        // Assert
        assertThat(controller.pendingEntry.value).isEqualTo(request)
    }
}
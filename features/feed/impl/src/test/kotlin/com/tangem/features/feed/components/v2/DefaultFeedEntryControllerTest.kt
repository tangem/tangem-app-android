package com.tangem.features.feed.components.v2

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.v2.FeedEntryFilter
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.features.feed.v2.FeedSection
import org.junit.jupiter.api.Test

/**
 * The slots outlive their publisher on purpose — a deeplink normally lands before the hub exists — so
 * what is worth guarding is that each is spent exactly once, and that an identical repeat request
 * still lands.
 */
internal class DefaultFeedEntryControllerTest {

    private val controller = DefaultFeedEntryController()

    @Test
    fun `GIVEN section and category WHEN requested THEN both slots are parked`() {
        // Act
        controller.request(FeedEntryRequest(section = FeedSection.Crypto, categoryId = "layer-1"))

        // Assert
        assertThat(controller.pendingEntry.value)
            .isEqualTo(FeedEntryRequest(section = FeedSection.Crypto, categoryId = "layer-1"))
        assertThat(controller.pendingFilter.value)
            .isEqualTo(FeedEntryFilter(tabId = FeedTabId.Crypto, categoryId = "layer-1"))
    }

    @Test
    fun `GIVEN earn type WHEN requested THEN the filter is addressed to the earn tab`() {
        // Act
        controller.request(
            FeedEntryRequest(section = FeedSection.Earn, earnType = PreselectedEarnType.Staking),
        )

        // Assert
        assertThat(controller.pendingFilter.value)
            .isEqualTo(FeedEntryFilter(tabId = FeedTabId.Earn, earnType = PreselectedEarnType.Staking))
    }

    @Test
    fun `GIVEN no section WHEN requested THEN no filter is parked because nothing would clear it`() {
        // Act
        controller.request(FeedEntryRequest(categoryId = "layer-1"))

        // Assert
        assertThat(controller.pendingFilter.value).isNull()
    }

    @Test
    fun `GIVEN a section with no filter values WHEN requested THEN no filter is parked`() {
        // Act
        controller.request(FeedEntryRequest(section = FeedSection.Assets))

        // Assert
        assertThat(controller.pendingEntry.value).isEqualTo(FeedEntryRequest(section = FeedSection.Assets))
        assertThat(controller.pendingFilter.value).isNull()
    }

    @Test
    fun `GIVEN nothing at all WHEN requested THEN the hub is still asked to open`() {
        // Act
        controller.request(FeedEntryRequest())

        // Assert - a bare deeplink opens the shtorka on its default section
        assertThat(controller.pendingEntry.value).isEqualTo(FeedEntryRequest())
        assertThat(controller.pendingFilter.value).isNull()
    }

    @Test
    fun `GIVEN parked slots WHEN consumed THEN each is emptied on its own`() {
        // Arrange
        controller.request(FeedEntryRequest(section = FeedSection.Crypto, categoryId = "layer-1"))

        // Act
        controller.consumeEntry()

        // Assert
        assertThat(controller.pendingEntry.value).isNull()
        assertThat(controller.pendingFilter.value).isNotNull()

        // Act
        controller.consumeFilter()

        // Assert
        assertThat(controller.pendingFilter.value).isNull()
    }

    @Test
    fun `GIVEN a consumed request WHEN the identical one arrives again THEN it is parked again`() {
        // Arrange
        val request = FeedEntryRequest(section = FeedSection.Earn, earnType = PreselectedEarnType.Yield)
        controller.request(request)
        controller.consumeEntry()
        controller.consumeFilter()

        // Act
        controller.request(request)

        // Assert
        assertThat(controller.pendingEntry.value).isEqualTo(request)
        assertThat(controller.pendingFilter.value).isNotNull()
    }
}
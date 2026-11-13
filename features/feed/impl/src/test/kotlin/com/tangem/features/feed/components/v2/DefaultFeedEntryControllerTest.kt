package com.tangem.features.feed.components.v2

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.v2.FeedEntryFilter
import com.tangem.features.feed.v2.FeedEntryRequest
import com.tangem.features.feed.v2.FeedFilterSheet
import com.tangem.features.feed.v2.FeedSection
import com.tangem.test.core.ProvideTestModels
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

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
    fun `GIVEN a section with only a sheet WHEN requested THEN the filter is parked for that sheet`() {
        // Act
        controller.request(FeedEntryRequest(section = FeedSection.Earn, sheet = FeedFilterSheet.Network))

        // Assert
        assertThat(controller.pendingFilter.value)
            .isEqualTo(FeedEntryFilter(tabId = FeedTabId.Earn, sheet = FeedFilterSheet.Network))
    }

    @Test
    fun `GIVEN for you WHEN requested THEN only the entry is parked`() {
        // Act
        controller.request(FeedEntryRequest(isForYou = true))

        // Assert
        assertThat(controller.pendingEntry.value).isEqualTo(FeedEntryRequest(isForYou = true))
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsLatest {

        @Test
        fun `GIVEN the latest request consumed WHEN its filter is checked THEN it is still the latest`() {
            // Arrange
            val controller = DefaultFeedEntryController()
            controller.request(sheetRequest)
            val filter = requireNotNull(controller.pendingFilter.value)
            controller.consumeEntry()
            controller.consumeFilter()

            // Act
            val actual = controller.isLatest(filter)

            // Assert
            assertThat(actual).isTrue()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a newer request WHEN the older filter is checked THEN it is superseded`(newer: FeedEntryRequest) {
            // Arrange
            val controller = DefaultFeedEntryController()
            controller.request(sheetRequest)
            val older = requireNotNull(controller.pendingFilter.value)
            controller.consumeFilter()

            // Act
            controller.request(newer)

            // Assert
            assertThat(controller.isLatest(older)).isFalse()
        }

        @Test
        fun `GIVEN an identical request while the first is pending WHEN the pending filter is checked THEN it is the latest`() {
            // Arrange
            val controller = DefaultFeedEntryController()
            controller.request(sheetRequest)
            controller.request(sheetRequest.copy())
            val pending = requireNotNull(controller.pendingFilter.value)

            // Act
            val actual = controller.isLatest(pending)

            // Assert
            assertThat(actual).isTrue()
        }

        @Test
        fun `GIVEN a tab consuming on emission WHEN requested THEN the delivered filter is the latest`() = runTest {
            // Arrange
            val controller = DefaultFeedEntryController()
            var delivered: FeedEntryFilter? = null
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                controller.pendingFilter.filterNotNull().collect { filter ->
                    delivered = filter
                    controller.consumeFilter()
                }
            }

            // Act
            controller.request(sheetRequest)

            // Assert
            assertThat(controller.isLatest(requireNotNull(delivered))).isTrue()
        }

        private fun provideTestModels() = listOf(
            FeedEntryRequest(isForYou = true),
            FeedEntryRequest(section = FeedSection.Earn, earnType = PreselectedEarnType.Yield),
            sheetRequest,
        )

        private val sheetRequest = FeedEntryRequest(section = FeedSection.Crypto, sheet = FeedFilterSheet.Category)
    }
}
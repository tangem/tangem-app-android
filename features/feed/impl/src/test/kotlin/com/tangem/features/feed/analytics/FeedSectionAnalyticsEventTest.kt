package com.tangem.features.feed.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.nav.analyticsSection
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * The Amplitude `Section` value deliberately differs from [FeedTabId.value] — the backend tab id
 * `real_assets` is reported as `Assets` — so both halves of the mapping are pinned here.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class FeedSectionAnalyticsEventTest {

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun analyticsSection(model: SectionModel) {
        // Act
        val actual = model.tabId.analyticsSection

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN real assets tab WHEN screen opened reported THEN event carries the agreed params`() {
        // Act
        val event = FeedSectionAnalyticsEvent.MarketsScreenOpened(FeedTabId.RealAssets.analyticsSection)

        // Assert
        assertThat(event.id).isEqualTo("[Markets] Markets Screen Opened")
        assertThat(event.params).containsExactlyEntriesIn(mapOf(SECTION_KEY to "Assets"))
    }

    @Test
    fun `GIVEN real assets tab WHEN section entry reported THEN event carries the agreed params`() {
        // Act
        val event = FeedSectionAnalyticsEvent.EnterSection(FeedTabId.RealAssets.analyticsSection)

        // Assert
        assertThat(event.id).isEqualTo("[Markets] Enter Section")
        assertThat(event.params).containsExactlyEntriesIn(mapOf(SECTION_KEY to "Assets"))
    }

    internal data class SectionModel(val tabId: FeedTabId, val expected: String)

    private fun provideTestModels() = listOf(
        SectionModel(tabId = FeedTabId.Crypto, expected = "Crypto"),
        SectionModel(tabId = FeedTabId.RealAssets, expected = "Assets"),
        SectionModel(tabId = FeedTabId.Earn, expected = "Earn"),
        SectionModel(tabId = FeedTabId.Predictions, expected = "Predictions"),
        SectionModel(tabId = FeedTabId("unknown"), expected = "unknown"),
    )

    private companion object {
        const val SECTION_KEY = "Section"
    }
}
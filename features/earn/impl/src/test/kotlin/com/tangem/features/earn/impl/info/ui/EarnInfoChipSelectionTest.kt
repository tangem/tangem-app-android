package com.tangem.features.earn.impl.info.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * The two device repros of the Info page at 2.5 px per dp: the list is 1700 px high, the stuck chips take 150 px, so
 * the reference line is 666 px. The list items are Balance 0, Rewards 1, Chart 2, Tabs 3 (the chips), MyFunds 4,
 * Benefits 5, Details 6.
 */
internal class EarnInfoChipSelectionTest {

    private val clickedSection = ClickedSection()

    private fun scrolledTab(visibleOffsets: Map<Int, Int>, firstVisibleIndex: Int, isAtEnd: Boolean): EarnInfoTab =
        selectedSection(
            sectionTops = sectionTops(
                sectionIndices = SECTION_INDICES,
                visibleOffsets = visibleOffsets,
                firstVisibleIndex = firstVisibleIndex,
            ),
            referenceLine = referenceLine(chipsBottom = CHIPS_BOTTOM, viewportHeight = LIST_HEIGHT),
            isAtEnd = isAtEnd,
        )

    private fun litTab(scrolledTab: EarnInfoTab): EarnInfoTab = clickedSection.tab ?: scrolledTab

    @Nested
    inner class ShortTailAtFontScale1 {

        private val atEnd = mapOf(3 to 0, 4 to -300, 5 to 500, 6 to 1000)
        private val atEndPosition = ListPosition(index = 4, scrollOffset = 300)

        private fun tapBenefitsAtEnd() {
            val tapId = clickedSection.onTap(EarnInfoTab.Benefits)
            clickedSection.onScrollEnded(tapId = tapId, position = atEndPosition)
        }

        @Test
        fun `GIVEN the end of a short tail WHEN Benefits is tapped THEN Benefits is lit and the list does not move`() {
            // Arrange
            val scrolled = scrolledTab(visibleOffsets = atEnd, firstVisibleIndex = 4, isAtEnd = true)

            // Act
            tapBenefitsAtEnd()
            clickedSection.onPositionChanged(atEndPosition)

            // Assert
            assertThat(scrolled).isEqualTo(EarnInfoTab.Details)
            assertThat(litTab(scrolled)).isEqualTo(EarnInfoTab.Benefits)
        }

        @Test
        fun `GIVEN Benefits tapped at the end WHEN the list is dragged 80 px down THEN Benefits stays lit`() {
            // Arrange
            tapBenefitsAtEnd()
            val afterDrag = mapOf(3 to 0, 4 to -220, 5 to 580, 6 to 1080)

            // Act
            clickedSection.onPositionChanged(ListPosition(index = 4, scrollOffset = 220))
            val scrolled = scrolledTab(visibleOffsets = afterDrag, firstVisibleIndex = 4, isAtEnd = false)

            // Assert
            assertThat(clickedSection.tab).isNull()
            assertThat(scrolled).isEqualTo(EarnInfoTab.Benefits)
            assertThat(litTab(scrolled)).isEqualTo(EarnInfoTab.Benefits)
        }

        @Test
        fun `GIVEN Benefits tapped at the end WHEN the list is dragged 170 px down THEN My Funds is lit`() {
            // Arrange
            tapBenefitsAtEnd()
            val afterDrag = mapOf(3 to 0, 4 to -130, 5 to 670, 6 to 1170)

            // Act
            clickedSection.onPositionChanged(ListPosition(index = 4, scrollOffset = 130))
            val scrolled = scrolledTab(visibleOffsets = afterDrag, firstVisibleIndex = 4, isAtEnd = false)

            // Assert
            assertThat(litTab(scrolled)).isEqualTo(EarnInfoTab.MyFunds)
        }
    }

    @Nested
    inner class ShortTailAtFontScale1Dot3 {

        private val atEnd = mapOf(3 to 0, 4 to -650, 5 to 150, 6 to 800)
        private val atEndPosition = ListPosition(index = 4, scrollOffset = 650)

        @Test
        fun `GIVEN Benefits tapped at the end WHEN a drag cannot move the list THEN Benefits stays lit`() {
            // Arrange
            val tapId = clickedSection.onTap(EarnInfoTab.Benefits)
            clickedSection.onScrollEnded(tapId = tapId, position = atEndPosition)

            // Act
            clickedSection.onPositionChanged(atEndPosition)
            val scrolled = scrolledTab(visibleOffsets = atEnd, firstVisibleIndex = 4, isAtEnd = true)

            // Assert
            assertThat(scrolled).isEqualTo(EarnInfoTab.Details)
            assertThat(litTab(scrolled)).isEqualTo(EarnInfoTab.Benefits)
        }
    }

    @Nested
    inner class LongPage {

        @Test
        fun `GIVEN a long page WHEN Benefits is scrolled under the chips THEN the scroll alone selects Benefits`() {
            // Arrange
            val benefitsUnderChips = mapOf(3 to 0, 4 to -750, 5 to CHIPS_BOTTOM, 6 to 1050)

            // Act
            val actual = scrolledTab(visibleOffsets = benefitsUnderChips, firstVisibleIndex = 4, isAtEnd = false)

            // Assert
            assertThat(actual).isEqualTo(EarnInfoTab.Benefits)
        }
    }

    /**
     * The list runs under the header (300 px of top content padding) and the footer (200 px of bottom content padding)
     * in a 2200 px viewport. Item offsets start under the header and the pinned chips sit there, so the chips end at
     * 150 and the area under them ends at the footer's top, 1700: the reference line stays at 666.
     */
    @Nested
    inner class UnderHeaderAndFooter {

        private fun scrolledTab(visibleOffsets: Map<Int, Int>, firstVisibleIndex: Int, isAtEnd: Boolean): EarnInfoTab =
            selectedSection(
                sectionTops = sectionTops(
                    sectionIndices = SECTION_INDICES,
                    visibleOffsets = visibleOffsets,
                    firstVisibleIndex = firstVisibleIndex,
                ),
                referenceLine = referenceLine(
                    chipsBottom = CHIPS_BOTTOM,
                    viewportHeight = visibleListHeight(
                        viewportHeight = PADDED_VIEWPORT_HEIGHT,
                        beforeContentPadding = HEADER_HEIGHT,
                        afterContentPadding = FOOTER_HEIGHT,
                    ),
                ),
                isAtEnd = isAtEnd,
            )

        @Test
        fun `GIVEN Benefits on the line WHEN the page scrolls THEN Benefits is lit`() {
            // Arrange
            val benefitsOnLine = mapOf(3 to -260, 4 to -150, 5 to 666, 6 to 1300)

            // Act
            val actual = scrolledTab(visibleOffsets = benefitsOnLine, firstVisibleIndex = 4, isAtEnd = false)

            // Assert
            assertThat(actual).isEqualTo(EarnInfoTab.Benefits)
        }

        @Test
        fun `GIVEN Benefits one pixel below the line WHEN the page scrolls THEN My Funds is lit`() {
            // Arrange
            val benefitsBelowLine = mapOf(3 to -261, 4 to -149, 5 to 667, 6 to 1301)

            // Act
            val actual = scrolledTab(visibleOffsets = benefitsBelowLine, firstVisibleIndex = 4, isAtEnd = false)

            // Assert
            assertThat(actual).isEqualTo(EarnInfoTab.MyFunds)
        }

        /** The whole 2200 px viewport would put the line at 150 + 2050 / 3 = 833 and select Benefits at 750. */
        @Test
        fun `GIVEN Benefits between the line and the full viewport line WHEN the page scrolls THEN My Funds is lit`() {
            // Arrange
            val benefitsBelowLine = mapOf(4 to -150, 5 to 750, 6 to 1400)

            // Act
            val actual = scrolledTab(visibleOffsets = benefitsBelowLine, firstVisibleIndex = 4, isAtEnd = false)

            // Assert
            assertThat(actual).isEqualTo(EarnInfoTab.MyFunds)
        }

        @Test
        fun `GIVEN a chip tap WHEN its section is scrolled under the pinned chips THEN that section is lit`() {
            // Arrange
            val benefitsUnderPinnedChips = mapOf(3 to -950, 4 to -800, 5 to CHIPS_BOTTOM, 6 to 1000)

            // Act
            val actual = scrolledTab(visibleOffsets = benefitsUnderPinnedChips, firstVisibleIndex = 4, isAtEnd = false)

            // Assert
            assertThat(actual).isEqualTo(EarnInfoTab.Benefits)
        }

        @Test
        fun `GIVEN the end of the page WHEN Details is below the line THEN Details is lit`() {
            // Arrange
            val atEnd = mapOf(4 to -300, 5 to 500, 6 to 1000)

            // Act
            val actual = scrolledTab(visibleOffsets = atEnd, firstVisibleIndex = 4, isAtEnd = true)

            // Assert
            assertThat(actual).isEqualTo(EarnInfoTab.Details)
        }
    }

    private companion object {
        const val PADDED_VIEWPORT_HEIGHT = 2200
        const val HEADER_HEIGHT = 300
        const val FOOTER_HEIGHT = 200
        const val CHIPS_BOTTOM = 150
        const val LIST_HEIGHT = 1700
        val SECTION_INDICES = mapOf(
            EarnInfoTab.MyFunds to 4,
            EarnInfoTab.Benefits to 5,
            EarnInfoTab.Details to 6,
        )
    }
}
package com.tangem.features.earn.impl.info.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab
import org.junit.jupiter.api.Test

internal class ClickedSectionTest {

    private val clickedSection = ClickedSection()

    @Test
    fun `GIVEN no tap WHEN created THEN nothing is held`() {
        // Assert
        assertThat(clickedSection.tab).isNull()
    }

    @Test
    fun `GIVEN a tap WHEN the programmatic scroll moves the list THEN the tab is held`() {
        // Arrange
        clickedSection.onTap(EarnInfoTab.Benefits)

        // Act
        clickedSection.onPositionChanged(ListPosition(index = 3, scrollOffset = 100))
        clickedSection.onPositionChanged(ListPosition(index = 4, scrollOffset = 700))

        // Assert
        assertThat(clickedSection.tab).isEqualTo(EarnInfoTab.Benefits)
    }

    @Test
    fun `GIVEN a tap and a finished scroll WHEN a drag leaves the position unchanged THEN the tab is held`() {
        // Arrange
        val tapId = clickedSection.onTap(EarnInfoTab.Benefits)
        clickedSection.onScrollEnded(tapId = tapId, position = AT_END)

        // Act
        clickedSection.onPositionChanged(AT_END)

        // Assert
        assertThat(clickedSection.tab).isEqualTo(EarnInfoTab.Benefits)
    }

    @Test
    fun `GIVEN a tap and a finished scroll WHEN the list moves THEN the tab is released`() {
        // Arrange
        val tapId = clickedSection.onTap(EarnInfoTab.Benefits)
        clickedSection.onScrollEnded(tapId = tapId, position = AT_END)

        // Act
        clickedSection.onPositionChanged(ListPosition(index = AT_END.index, scrollOffset = AT_END.scrollOffset - 80))

        // Assert
        assertThat(clickedSection.tab).isNull()
    }

    @Test
    fun `GIVEN a released tab WHEN the list returns to the old position THEN the tab stays released`() {
        // Arrange
        val tapId = clickedSection.onTap(EarnInfoTab.Benefits)
        clickedSection.onScrollEnded(tapId = tapId, position = AT_END)
        clickedSection.onPositionChanged(ListPosition(index = AT_END.index, scrollOffset = AT_END.scrollOffset - 80))

        // Act
        clickedSection.onPositionChanged(AT_END)

        // Assert
        assertThat(clickedSection.tab).isNull()
    }

    @Test
    fun `GIVEN a tap WHEN the scroll is interrupted THEN the tab is released`() {
        // Arrange
        val tapId = clickedSection.onTap(EarnInfoTab.Benefits)

        // Act
        clickedSection.onScrollInterrupted(tapId)

        // Assert
        assertThat(clickedSection.tab).isNull()
    }

    @Test
    fun `GIVEN two taps WHEN the first scroll is interrupted by the second THEN the second tab is held`() {
        // Arrange
        val firstTapId = clickedSection.onTap(EarnInfoTab.Benefits)
        clickedSection.onTap(EarnInfoTab.Details)

        // Act
        clickedSection.onScrollInterrupted(firstTapId)

        // Assert
        assertThat(clickedSection.tab).isEqualTo(EarnInfoTab.Details)
    }

    @Test
    fun `GIVEN two taps WHEN the first scroll reports its end late THEN it does not set the rest position`() {
        // Arrange
        val firstTapId = clickedSection.onTap(EarnInfoTab.Benefits)
        clickedSection.onTap(EarnInfoTab.Details)

        // Act
        clickedSection.onScrollEnded(tapId = firstTapId, position = AT_END)
        clickedSection.onPositionChanged(ListPosition(index = 2, scrollOffset = 40))

        // Assert
        assertThat(clickedSection.tab).isEqualTo(EarnInfoTab.Details)
    }

    @Test
    fun `GIVEN a second tap WHEN its scroll finishes and the list moves THEN the second tab is released`() {
        // Arrange
        clickedSection.onTap(EarnInfoTab.Benefits)
        val secondTapId = clickedSection.onTap(EarnInfoTab.Details)
        clickedSection.onScrollEnded(tapId = secondTapId, position = AT_END)

        // Act
        clickedSection.onPositionChanged(ListPosition(index = 2, scrollOffset = 40))

        // Assert
        assertThat(clickedSection.tab).isNull()
    }

    @Test
    fun `GIVEN a finished first tap WHEN a second tap scrolls the list THEN the second tab is held`() {
        // Arrange
        val firstTapId = clickedSection.onTap(EarnInfoTab.Benefits)
        clickedSection.onScrollEnded(tapId = firstTapId, position = AT_END)

        // Act
        clickedSection.onTap(EarnInfoTab.MyFunds)
        clickedSection.onPositionChanged(ListPosition(index = 3, scrollOffset = 120))

        // Assert
        assertThat(clickedSection.tab).isEqualTo(EarnInfoTab.MyFunds)
    }

    @Test
    fun `GIVEN a released tab WHEN another tab is tapped THEN the new tab is held`() {
        // Arrange
        val firstTapId = clickedSection.onTap(EarnInfoTab.Benefits)
        clickedSection.onScrollInterrupted(firstTapId)

        // Act
        clickedSection.onTap(EarnInfoTab.MyFunds)

        // Assert
        assertThat(clickedSection.tab).isEqualTo(EarnInfoTab.MyFunds)
    }

    private companion object {
        val AT_END = ListPosition(index = 4, scrollOffset = 300)
    }
}
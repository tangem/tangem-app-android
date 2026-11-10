package com.tangem.features.earn.impl.info.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SectionTopsTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN visible items WHEN sectionTops THEN hidden sections are placed above or below`(model: TopsModel) {
        // Act
        val actual = sectionTops(
            sectionIndices = ALL_SECTIONS,
            visibleOffsets = model.visibleOffsets,
            firstVisibleIndex = model.firstVisibleIndex,
        )

        // Assert
        assertThat(actual.entries.toList()).containsExactlyElementsIn(model.expected.entries.toList()).inOrder()
    }

    private fun provideTestModels() = listOf(
        TopsModel(
            name = "all sections visible",
            firstVisibleIndex = 3,
            visibleOffsets = mapOf(3 to -120, 4 to 340, 5 to 840, 6 to 1240),
            expected = mapOf(
                EarnInfoTab.MyFunds to 340,
                EarnInfoTab.Benefits to 840,
                EarnInfoTab.Details to 1240,
            ),
        ),
        TopsModel(
            name = "first section partly above the viewport",
            firstVisibleIndex = 4,
            visibleOffsets = mapOf(3 to 0, 4 to -300, 5 to 500, 6 to 1000),
            expected = mapOf(
                EarnInfoTab.MyFunds to -300,
                EarnInfoTab.Benefits to 500,
                EarnInfoTab.Details to 1000,
            ),
        ),
        TopsModel(
            name = "first section scrolled out above the viewport",
            firstVisibleIndex = 5,
            visibleOffsets = mapOf(3 to 0, 5 to -200, 6 to 300),
            expected = mapOf(
                EarnInfoTab.MyFunds to ABOVE_VIEWPORT,
                EarnInfoTab.Benefits to -200,
                EarnInfoTab.Details to 300,
            ),
        ),
        TopsModel(
            name = "last section not reached below the viewport",
            firstVisibleIndex = 0,
            visibleOffsets = mapOf(0 to -100, 1 to 400, 2 to 900, 3 to 1500, 4 to 1600),
            expected = mapOf(
                EarnInfoTab.MyFunds to 1600,
                EarnInfoTab.Benefits to BELOW_VIEWPORT,
                EarnInfoTab.Details to BELOW_VIEWPORT,
            ),
        ),
        TopsModel(
            name = "two sections scrolled out and the last one visible",
            firstVisibleIndex = 6,
            visibleOffsets = mapOf(6 to -50),
            expected = mapOf(
                EarnInfoTab.MyFunds to ABOVE_VIEWPORT,
                EarnInfoTab.Benefits to ABOVE_VIEWPORT,
                EarnInfoTab.Details to -50,
            ),
        ),
        TopsModel(
            name = "nothing laid out yet",
            firstVisibleIndex = 0,
            visibleOffsets = emptyMap(),
            expected = mapOf(
                EarnInfoTab.MyFunds to BELOW_VIEWPORT,
                EarnInfoTab.Benefits to BELOW_VIEWPORT,
                EarnInfoTab.Details to BELOW_VIEWPORT,
            ),
        ),
    )

    internal data class TopsModel(
        val name: String,
        val firstVisibleIndex: Int,
        val visibleOffsets: Map<Int, Int>,
        val expected: Map<EarnInfoTab, Int>,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        val ALL_SECTIONS = mapOf(
            EarnInfoTab.MyFunds to 4,
            EarnInfoTab.Benefits to 5,
            EarnInfoTab.Details to 6,
        )
    }
}
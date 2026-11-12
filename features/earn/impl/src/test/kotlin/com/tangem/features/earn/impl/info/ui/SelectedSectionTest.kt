package com.tangem.features.earn.impl.info.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.features.earn.impl.info.ui.state.EarnInfoTab
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class SelectedSectionTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SelectedSection {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN section tops WHEN selectedSection THEN last section at or above the line is selected`(
            model: SectionModel,
        ) {
            // Act
            val actual = selectedSection(
                sectionTops = model.sectionTops,
                referenceLine = model.referenceLine,
                isAtEnd = model.isAtEnd,
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SectionModel(
                name = "all tops above the line",
                sectionTops = mapOf(
                    EarnInfoTab.MyFunds to -300,
                    EarnInfoTab.Benefits to 100,
                    EarnInfoTab.Details to 500,
                ),
                expected = EarnInfoTab.Details,
            ),
            SectionModel(
                name = "Details below the line",
                sectionTops = mapOf(
                    EarnInfoTab.MyFunds to -300,
                    EarnInfoTab.Benefits to 500,
                    EarnInfoTab.Details to 1000,
                ),
                expected = EarnInfoTab.Benefits,
            ),
            SectionModel(
                name = "top exactly on the line",
                sectionTops = mapOf(
                    EarnInfoTab.MyFunds to -300,
                    EarnInfoTab.Benefits to REFERENCE_LINE,
                    EarnInfoTab.Details to 1000,
                ),
                expected = EarnInfoTab.Benefits,
            ),
            SectionModel(
                name = "top one pixel below the line",
                sectionTops = mapOf(
                    EarnInfoTab.MyFunds to -300,
                    EarnInfoTab.Benefits to REFERENCE_LINE + 1,
                    EarnInfoTab.Details to 1000,
                ),
                expected = EarnInfoTab.MyFunds,
            ),
            SectionModel(
                name = "every top below the line",
                sectionTops = mapOf(
                    EarnInfoTab.MyFunds to 900,
                    EarnInfoTab.Benefits to 1400,
                    EarnInfoTab.Details to 1900,
                ),
                expected = EarnInfoTab.MyFunds,
            ),
            SectionModel(
                name = "first section above the viewport, the others below it",
                sectionTops = mapOf(
                    EarnInfoTab.MyFunds to ABOVE_VIEWPORT,
                    EarnInfoTab.Benefits to BELOW_VIEWPORT,
                    EarnInfoTab.Details to BELOW_VIEWPORT,
                ),
                expected = EarnInfoTab.MyFunds,
            ),
            SectionModel(
                name = "first two sections above the viewport",
                sectionTops = mapOf(
                    EarnInfoTab.MyFunds to ABOVE_VIEWPORT,
                    EarnInfoTab.Benefits to ABOVE_VIEWPORT,
                    EarnInfoTab.Details to 800,
                ),
                expected = EarnInfoTab.Benefits,
            ),
            SectionModel(
                name = "page at its end while the last top is below the line",
                sectionTops = mapOf(
                    EarnInfoTab.MyFunds to -300,
                    EarnInfoTab.Benefits to 500,
                    EarnInfoTab.Details to 1000,
                ),
                isAtEnd = true,
                expected = EarnInfoTab.Details,
            ),
            SectionModel(
                name = "no Benefits, Details below the line",
                sectionTops = mapOf(
                    EarnInfoTab.MyFunds to 200,
                    EarnInfoTab.Details to 900,
                ),
                expected = EarnInfoTab.MyFunds,
            ),
            SectionModel(
                name = "no Benefits, Details above the line",
                sectionTops = mapOf(
                    EarnInfoTab.MyFunds to ABOVE_VIEWPORT,
                    EarnInfoTab.Details to 600,
                ),
                expected = EarnInfoTab.Details,
            ),
            SectionModel(
                name = "single section",
                sectionTops = mapOf(EarnInfoTab.MyFunds to BELOW_VIEWPORT),
                isAtEnd = true,
                expected = EarnInfoTab.MyFunds,
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ReferenceLine {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN chips and viewport WHEN referenceLine THEN a third of the space under the chips`(
            model: LineModel,
        ) {
            // Act
            val actual = referenceLine(chipsBottom = model.chipsBottom, viewportHeight = model.viewportHeight)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            LineModel(name = "tall phone", chipsBottom = 150, viewportHeight = 1700, expected = 666),
            LineModel(name = "chips not measured yet", chipsBottom = 0, viewportHeight = 900, expected = 300),
            LineModel(name = "nothing under the chips", chipsBottom = 150, viewportHeight = 150, expected = 150),
            LineModel(name = "space not divisible by three", chipsBottom = 100, viewportHeight = 800, expected = 333),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class VisibleListHeight {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN list under header and footer WHEN visibleListHeight THEN the paddings are left out`(
            model: HeightModel,
        ) {
            // Act
            val actual = visibleListHeight(
                viewportHeight = model.viewportHeight,
                beforeContentPadding = model.beforeContentPadding,
                afterContentPadding = model.afterContentPadding,
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            HeightModel(
                name = "header and footer over the list",
                viewportHeight = 2200,
                beforeContentPadding = 300,
                afterContentPadding = 200,
                expected = 1700,
            ),
            HeightModel(
                name = "overlays not measured yet",
                viewportHeight = 2200,
                beforeContentPadding = 0,
                afterContentPadding = 0,
                expected = 2200,
            ),
        )
    }

    internal data class HeightModel(
        val name: String,
        val viewportHeight: Int,
        val beforeContentPadding: Int,
        val afterContentPadding: Int,
        val expected: Int,
    ) {
        override fun toString(): String = name
    }

    internal data class SectionModel(
        val name: String,
        val sectionTops: Map<EarnInfoTab, Int>,
        val referenceLine: Int = REFERENCE_LINE,
        val isAtEnd: Boolean = false,
        val expected: EarnInfoTab,
    ) {
        override fun toString(): String = name
    }

    internal data class LineModel(
        val name: String,
        val chipsBottom: Int,
        val viewportHeight: Int,
        val expected: Int,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val REFERENCE_LINE = 666
    }
}
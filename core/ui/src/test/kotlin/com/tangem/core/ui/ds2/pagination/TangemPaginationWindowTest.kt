package com.tangem.core.ui.ds2.pagination

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

internal class TangemPaginationWindowTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NextWindowStart {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun `GIVEN previous window WHEN current page changes THEN window start is moved by the rule`(
            model: WindowModel,
        ) {
            // Act
            val actual = nextWindowStart(model.previousStart, model.currentPage, model.pageCount)

            // Assert
            assertThat(actual).isEqualTo(model.expectedStart)
        }

        @Test
        fun `GIVEN ten pages WHEN swiping 1 to 10 and back THEN window moves by one item and never past the edges`() {
            // Arrange
            val pageCount = 10
            val pages = (0 until pageCount) + (pageCount - 2 downTo 0)
            var start = 0

            // Act
            val starts = pages.map { page ->
                start = nextWindowStart(start, page, pageCount)
                start
            }

            // Assert
            assertThat(starts).containsExactly(
                0, 0, 0, 1, 2, 3, 4, 5, 6, 6,
                6, 6, 5, 4, 3, 2, 1, 0, 0,
            ).inOrder()
        }

        private fun provideTestModels() = listOf(
            WindowModel(pageCount = 3, previousStart = 0, currentPage = 2, expectedStart = 0),
            WindowModel(pageCount = 4, previousStart = 0, currentPage = 3, expectedStart = 0),
            WindowModel(pageCount = 10, previousStart = 0, currentPage = 2, expectedStart = 0),
            WindowModel(pageCount = 10, previousStart = 0, currentPage = 3, expectedStart = 1),
            WindowModel(pageCount = 10, previousStart = 3, currentPage = 4, expectedStart = 3),
            WindowModel(pageCount = 10, previousStart = 3, currentPage = 3, expectedStart = 2),
            WindowModel(pageCount = 10, previousStart = 6, currentPage = 9, expectedStart = 6),
            WindowModel(pageCount = 10, previousStart = 0, currentPage = 9, expectedStart = 6),
            WindowModel(pageCount = 10, previousStart = 6, currentPage = 0, expectedStart = 0),
            WindowModel(pageCount = 10, previousStart = 0, currentPage = 5, expectedStart = 3),
            WindowModel(pageCount = 5, previousStart = 9, currentPage = 4, expectedStart = 1),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsOverflowItem {

        @ParameterizedTest
        @MethodSource("provideTestModels")
        fun `GIVEN window WHEN checking each visible item THEN only edges with pages beyond are overflow`(
            model: OverflowModel,
        ) {
            // Act
            val actual = (model.windowStart until model.windowStart + PAGINATION_WINDOW_SIZE)
                .map { isOverflowItem(it, model.windowStart, model.pageCount) }

            // Assert
            assertThat(actual).containsExactlyElementsIn(model.expected).inOrder()
        }

        private fun provideTestModels() = listOf(
            OverflowModel(pageCount = 4, windowStart = 0, expected = listOf(false, false, false, false)),
            OverflowModel(pageCount = 10, windowStart = 0, expected = listOf(false, false, false, true)),
            OverflowModel(pageCount = 10, windowStart = 3, expected = listOf(true, false, false, true)),
            OverflowModel(pageCount = 10, windowStart = 6, expected = listOf(true, false, false, false)),
        )
    }

    data class WindowModel(val pageCount: Int, val previousStart: Int, val currentPage: Int, val expectedStart: Int)

    data class OverflowModel(val pageCount: Int, val windowStart: Int, val expected: List<Boolean>)
}
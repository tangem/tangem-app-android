package com.tangem.features.earn.impl.info.ui

import com.google.common.truth.Truth.assertThat
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class PinnedChipsTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AreChipsPinned {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN chips position WHEN areChipsPinned THEN pinned once they reach the header`(model: PinnedModel) {
            // Act
            val actual = areChipsPinned(
                chipsOffset = model.chipsOffset,
                chipsIndex = CHIPS_INDEX,
                firstVisibleIndex = model.firstVisibleIndex,
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            PinnedModel(name = "chips below the header", chipsOffset = 400, firstVisibleIndex = 0, expected = false),
            PinnedModel(name = "chips 1 px below the header", chipsOffset = 1, firstVisibleIndex = 2, expected = false),
            PinnedModel(name = "chips right under the header", chipsOffset = 0, firstVisibleIndex = 3, expected = true),
            PinnedModel(name = "chips under the header", chipsOffset = -200, firstVisibleIndex = 3, expected = true),
            PinnedModel(name = "chips not laid out above", chipsOffset = null, firstVisibleIndex = 5, expected = true),
            PinnedModel(name = "chips not laid out below", chipsOffset = null, firstVisibleIndex = 0, expected = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ChipsRowTop {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN chips position WHEN chipsRowTop THEN the row follows the item and stops under the header`(
            model: TopModel,
        ) {
            // Act
            val actual = chipsRowTop(
                chipsOffset = model.chipsOffset,
                chipsIndex = CHIPS_INDEX,
                firstVisibleIndex = model.firstVisibleIndex,
                navigationHeight = NAVIGATION_HEIGHT,
                viewportHeight = VIEWPORT_HEIGHT,
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            TopModel(name = "chips below the header", chipsOffset = 400, firstVisibleIndex = 0, expected = 700),
            TopModel(name = "chips right under the header", chipsOffset = 0, firstVisibleIndex = 3, expected = 300),
            TopModel(name = "chips under the header", chipsOffset = -200, firstVisibleIndex = 3, expected = 300),
            TopModel(name = "chips not laid out above", chipsOffset = null, firstVisibleIndex = 5, expected = 300),
            TopModel(name = "chips not laid out below", chipsOffset = null, firstVisibleIndex = 0, expected = 2200),
        )
    }

    internal data class PinnedModel(
        val name: String,
        val chipsOffset: Int?,
        val firstVisibleIndex: Int,
        val expected: Boolean,
    ) {
        override fun toString(): String = name
    }

    internal data class TopModel(
        val name: String,
        val chipsOffset: Int?,
        val firstVisibleIndex: Int,
        val expected: Int,
    ) {
        override fun toString(): String = name
    }

    private companion object {
        const val CHIPS_INDEX = 3
        const val NAVIGATION_HEIGHT = 300
        const val VIEWPORT_HEIGHT = 2200
    }
}
package com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

private const val TOLERANCE = 0.001f

/** The design's 260 x 406 card window. */
private const val CARD_ASPECT = 406f / 260f

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RewardCardTest {

    @ParameterizedTest
    @MethodSource("provideCaseSizeModels")
    fun `GIVEN the room left for the case WHEN rewardCaseSize THEN it keeps the design height unless it does not fit`(
        model: CaseSizeModel,
    ) {
        // Act
        val actual = rewardCaseSize(
            maxWidth = model.maxWidth.dp,
            maxHeight = model.maxHeight.dp,
            cardAspect = model.cardAspect,
        )

        // Assert
        assertThat(actual.width.value).isWithin(TOLERANCE).of(model.expectedWidth)
        assertThat(actual.height.value).isWithin(TOLERANCE).of(model.expectedHeight)
    }

    internal data class CaseSizeModel(
        val maxHeight: Float,
        val expectedWidth: Float,
        val expectedHeight: Float,
        val maxWidth: Float = 402f,
        val cardAspect: Float = CARD_ASPECT,
    )

    private fun provideCaseSizeModels() = listOf(
        // 260 x 406 window plus the case's 10 rail on every side
        CaseSizeModel(maxHeight = 600f, expectedWidth = 280f, expectedHeight = 426f),
        CaseSizeModel(maxHeight = 426f, expectedWidth = 280f, expectedHeight = 426f),
        CaseSizeModel(maxHeight = 200f, expectedWidth = 180f / CARD_ASPECT + 20f, expectedHeight = 200f),
        CaseSizeModel(maxWidth = 804f, maxHeight = 1_000f, expectedWidth = 540f, expectedHeight = 832f),
        // A tall image keeps the height and narrows
        CaseSizeModel(maxHeight = 600f, cardAspect = 2f, expectedWidth = 223f, expectedHeight = 426f),
        // A flat image hits the widest window and gives up height
        CaseSizeModel(maxHeight = 600f, cardAspect = 1f, expectedWidth = 280f, expectedHeight = 280f),
        CaseSizeModel(maxHeight = 20f, expectedWidth = 20f, expectedHeight = 20f),
    )
}
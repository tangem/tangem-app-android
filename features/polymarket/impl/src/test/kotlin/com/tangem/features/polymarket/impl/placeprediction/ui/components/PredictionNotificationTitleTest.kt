package com.tangem.features.polymarket.impl.placeprediction.ui.components

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionNotificationUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PredictionNotificationTitleTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a notification and side WHEN title THEN side specific string`(model: TitleModel) {
        // Act
        val actual = model.notification.title(side = model.side)

        // Assert
        assertThat(actual).isEqualTo(resourceReference(model.expectedRes))
    }

    internal data class TitleModel(
        val notification: PredictionNotificationUM,
        val side: PredictionOrderSide,
        val expectedRes: Int,
    )

    private fun provideTestModels() = listOf(
        TitleModel(
            notification = PredictionNotificationUM.PartialFill,
            side = PredictionOrderSide.BUY,
            expectedRes = R.string.prediction_place_notification_partial_title,
        ),
        TitleModel(
            notification = PredictionNotificationUM.PartialFill,
            side = PredictionOrderSide.SELL,
            expectedRes = R.string.prediction_close_notification_partial_title,
        ),
        TitleModel(
            notification = PredictionNotificationUM.NoLiquidity,
            side = PredictionOrderSide.BUY,
            expectedRes = R.string.prediction_place_notification_no_liquidity_title,
        ),
        TitleModel(
            notification = PredictionNotificationUM.NoLiquidity,
            side = PredictionOrderSide.SELL,
            expectedRes = R.string.prediction_close_notification_no_liquidity_title,
        ),
    )
}
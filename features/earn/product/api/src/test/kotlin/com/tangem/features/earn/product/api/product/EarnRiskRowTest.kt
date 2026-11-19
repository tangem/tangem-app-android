package com.tangem.features.earn.product.api.product

import androidx.annotation.StringRes
import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnRiskRowTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a risk level WHEN detailsRow THEN the row shows that level and its sheet explains all three`(
        model: RiskModel,
    ) {
        // Act
        val actual = model.risk.detailsRow()

        // Assert
        val expected = EarnRow(
            label = resourceReference(R.string.earn_product_details_risk),
            value = EarnValue.Text(resourceReference(model.levelRes)),
            info = EarnRowInfo.Items(
                title = resourceReference(R.string.earn_product_details_risk_info_title),
                items = listOf(
                    EarnTextBlock(
                        title = resourceReference(R.string.earn_risk_low),
                        description = resourceReference(R.string.earn_product_details_risk_low_description),
                    ),
                    EarnTextBlock(
                        title = resourceReference(R.string.earn_risk_medium),
                        description = resourceReference(R.string.earn_product_details_risk_medium_description),
                    ),
                    EarnTextBlock(
                        title = resourceReference(R.string.earn_risk_high),
                        description = resourceReference(R.string.earn_product_details_risk_high_description),
                    ),
                ),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    data class RiskModel(val risk: EarnRisk, @StringRes val levelRes: Int)

    private fun provideTestModels() = listOf(
        RiskModel(risk = EarnRisk.Low, levelRes = R.string.earn_risk_level_low),
        RiskModel(risk = EarnRisk.Medium, levelRes = R.string.earn_risk_level_medium),
        RiskModel(risk = EarnRisk.High, levelRes = R.string.earn_risk_level_high),
    )
}
package com.tangem.features.tangempay.cashback.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.TangemPayTariffPlan
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayCashbackInfoTilesConverterTest {

    private val converter = TangemPayCashbackInfoTilesConverter(
        onRateClick = {},
        onAccrualsClick = {},
    )

    @ParameterizedTest
    @MethodSource("rateSelectionModels")
    fun `GIVEN plan WHEN convert THEN rate tile shows the tier rate and the plan subtitle`(model: RateSelectionModel) {
        // Act
        val result = converter.convert(twoTiers(), model.plan, model.planName)

        // Assert
        assertThat(result.rate.title).isEqualTo(stringReference(model.expectedTitle))
        assertThat(result.rate.subtitle).isEqualTo(model.expectedSubtitle)
    }

    @Test
    fun `GIVEN empty tiers and unknown plan WHEN convert THEN rate tile has no percent and empty subtitle`() {
        // Act
        val result = converter.convert(emptyList(), TangemPayTariffPlan.Type.UNKNOWN, currentPlanName = null)

        // Assert
        assertThat(result.rate.title).isEqualTo(stringReference("Cashback"))
        assertThat(result.rate.subtitle).isEqualTo(TextReference.EMPTY)
    }

    @Test
    fun `GIVEN any tiers WHEN convert THEN accruals tile is static`() {
        // Act
        val result = converter.convert(twoTiers(), TangemPayTariffPlan.Type.BASIC, currentPlanName = "Basic")

        // Assert
        assertThat(result.accruals.title).isEqualTo(stringReference("Accruals"))
        assertThat(result.accruals.subtitle).isEqualTo(stringReference("Limits and exceptions"))
    }

    private fun rateSelectionModels() = listOf(
        RateSelectionModel(
            plan = TangemPayTariffPlan.Type.PLUS,
            planName = "Plus",
            expectedTitle = "Cashback 2%",
            expectedSubtitle = stringReference("With your Plus plan"),
        ),
        RateSelectionModel(
            plan = TangemPayTariffPlan.Type.BASIC,
            planName = "Basic",
            expectedTitle = "Cashback 1%",
            expectedSubtitle = stringReference("With your Basic plan"),
        ),
        RateSelectionModel(
            plan = TangemPayTariffPlan.Type.UNKNOWN,
            planName = null,
            expectedTitle = "Cashback 1%",
            expectedSubtitle = TextReference.EMPTY,
        ),
    )

    private fun twoTiers() = listOf(
        tier(TangemPayTariffPlan.Type.BASIC, rate = 1),
        tier(TangemPayTariffPlan.Type.PLUS, rate = 2),
    )

    private fun tier(planType: TangemPayTariffPlan.Type = TangemPayTariffPlan.Type.UNKNOWN, rate: Int? = null) =
        CashbackTier(
            planType = planType,
            rate = rate,
            label = "label",
            scope = "scope",
            minPurchase = null,
            monthlyCap = null,
        )

    data class RateSelectionModel(
        val plan: TangemPayTariffPlan.Type,
        val planName: String?,
        val expectedTitle: String,
        val expectedSubtitle: TextReference,
    )
}
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
        val result = converter.convert(twoTiers(), model.plan)

        // Assert
        assertThat(result.rate.title).isEqualTo(stringReference(model.expectedTitle))
        assertThat(result.rate.subtitle).isEqualTo(model.expectedSubtitle)
    }

    @Test
    fun `GIVEN empty tiers and no plan WHEN convert THEN rate tile has no percent and empty subtitle`() {
        // Act
        val result = converter.convert(emptyList(), currentPlan = null)

        // Assert
        assertThat(result.rate.title).isEqualTo(stringReference("Cashback"))
        assertThat(result.rate.subtitle).isEqualTo(TextReference.EMPTY)
    }

    @Test
    fun `GIVEN any tiers WHEN convert THEN accruals tile is static`() {
        // Act
        val result = converter.convert(twoTiers(), plan(tierId = "basic", name = "Basic"))

        // Assert
        assertThat(result.accruals.title).isEqualTo(stringReference("Accruals"))
        assertThat(result.accruals.subtitle).isEqualTo(stringReference("Limits and exceptions"))
    }

    private fun rateSelectionModels() = listOf(
        RateSelectionModel(
            plan = plan(tierId = "plus", name = "Plus"),
            expectedTitle = "Cashback 2%",
            expectedSubtitle = stringReference("With your Plus plan"),
        ),
        RateSelectionModel(
            plan = plan(tierId = "basic", name = "Basic"),
            expectedTitle = "Cashback 1%",
            expectedSubtitle = stringReference("With your Basic plan"),
        ),
        RateSelectionModel(
            plan = plan(tierId = "gold", name = "Gold"),
            expectedTitle = "Cashback 1%",
            expectedSubtitle = stringReference("With your Gold plan"),
        ),
        RateSelectionModel(
            plan = null,
            expectedTitle = "Cashback 1%",
            expectedSubtitle = TextReference.EMPTY,
        ),
    )

    private fun twoTiers() = listOf(
        tier(tierId = "basic", rate = 1),
        tier(tierId = "plus", rate = 2),
    )

    private fun tier(tierId: String, rate: Int? = null) = CashbackTier(
        tierId = tierId,
        rate = rate,
        label = "label",
        scope = "scope",
        minPurchase = null,
        monthlyCap = null,
    )

    private fun plan(tierId: String, name: String) = TangemPayTariffPlan(
        id = "plan-$tierId",
        tierId = tierId,
        isBasicTier = tierId == "basic",
        name = name,
        programName = "program",
        descriptionItems = emptyList(),
    )

    data class RateSelectionModel(
        val plan: TangemPayTariffPlan?,
        val expectedTitle: String,
        val expectedSubtitle: TextReference,
    )
}
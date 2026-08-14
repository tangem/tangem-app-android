package com.tangem.features.tangempay.cashback.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
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
    @MethodSource("titleModels")
    fun `GIVEN plan WHEN convert THEN rate title reflects the plan level`(model: TitleModel) {
        // Act
        val result = converter.convert(twoTiers(), model.plan)

        // Assert
        assertThat(result.rate.title).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN empty tiers WHEN convert THEN plain Cashback title`() {
        // Act
        val result = converter.convert(emptyList(), plan(tierId = "plus", name = "Plus"))

        // Assert
        assertThat(result.rate.title).isEqualTo(resourceReference(R.string.tangempay_cashback_title))
    }

    @ParameterizedTest
    @MethodSource("subtitleModels")
    fun `GIVEN plan WHEN convert THEN rate subtitle reflects the plan`(model: SubtitleModel) {
        // Act
        val result = converter.convert(twoTiers(), model.plan)

        // Assert
        assertThat(result.rate.subtitle).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN any tiers WHEN convert THEN accruals tile is static`() {
        // Act
        val result = converter.convert(twoTiers(), plan(tierId = "basic", name = "Basic"))

        // Assert
        assertThat(result.accruals.title).isEqualTo(resourceReference(R.string.tangempay_cashback_accruals_title))
        assertThat(result.accruals.subtitle)
            .isEqualTo(resourceReference(R.string.tangempay_cashback_accruals_subtitle))
    }

    private fun titleModels() = listOf(
        TitleModel(
            plan = plan(tierId = "plus", name = "Plus"),
            expected = resourceReference(R.string.tangempay_cashback_rate_title_up_to, wrappedList("2")),
        ),
        TitleModel(
            plan = plan(tierId = "gold", name = "Gold"),
            expected = resourceReference(R.string.tangempay_cashback_rate_title_up_to, wrappedList("2")),
        ),
        TitleModel(
            plan = plan(tierId = "basic", name = "Basic"),
            expected = resourceReference(R.string.tangempay_cashback_rate_title, wrappedList("1")),
        ),
        TitleModel(
            plan = null,
            expected = resourceReference(R.string.tangempay_cashback_rate_title, wrappedList("1")),
        ),
    )

    private fun subtitleModels() = listOf(
        SubtitleModel(
            plan = plan(tierId = "plus", name = "Plus"),
            expected = resourceReference(R.string.tangempay_cashback_rate_subtitle, wrappedList("Plus")),
        ),
        SubtitleModel(
            plan = plan(tierId = "basic", name = "Basic"),
            expected = resourceReference(R.string.tangempay_cashback_rate_subtitle, wrappedList("Basic")),
        ),
        SubtitleModel(plan = null, expected = TextReference.EMPTY),
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

    data class TitleModel(val plan: TangemPayTariffPlan?, val expected: TextReference)

    data class SubtitleModel(val plan: TangemPayTariffPlan?, val expected: TextReference)
}
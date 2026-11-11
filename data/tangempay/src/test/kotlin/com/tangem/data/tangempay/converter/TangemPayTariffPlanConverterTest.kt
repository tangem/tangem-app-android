package com.tangem.data.tangempay.converter

import com.tangem.domain.models.tangempay.TangemPayImage
import com.google.common.truth.Truth.assertThat
import com.tangem.spend.datasource.tangempay.models.response.CustomerMeResponse
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayTariffPlanConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN null plan or plan missing a required field WHEN convert THEN returns null`(model: InvalidPlanModel) {
        // Act
        val result = TangemPayTariffPlanConverter.convert(model.plan)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN basic tier in mixed case WHEN convert THEN isBasicTier is true`() {
        // Arrange
        val value = tariffPlan(type = "Basic")

        // Act
        val result = TangemPayTariffPlanConverter.convert(value)

        // Assert
        val expected = TangemPayTariffPlan(
            id = PLAN_ID,
            tierId = "Basic",
            isBasicTier = true,
            name = PLAN_NAME,
            programName = PROGRAM_NAME,
            descriptionItems = emptyList(),
            images = emptyList(),
            fees = emptyList(),
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN full valid plan WHEN convert THEN maps all fields`() {
        // Arrange
        val value = tariffPlan(
            type = "PLUS",
            descriptionItems = listOf(
                CustomerMeResponse.DescriptionItem(
                    type = "PLAN_RELATED",
                    order = 2,
                    title = "Title",
                    body = "Body",
                ),
            ),
            images = listOf(CustomerMeResponse.Image(type = "MAIN", url = "https://img")),
            fees = listOf(
                CustomerMeResponse.Fee(
                    type = "RECURRING",
                    amount = BigDecimal("9.99"),
                    currency = "USD",
                    description = "Monthly",
                    period = "MONTH",
                ),
            ),
        )

        // Act
        val result = TangemPayTariffPlanConverter.convert(value)

        // Assert
        val expected = TangemPayTariffPlan(
            id = PLAN_ID,
            tierId = "PLUS",
            isBasicTier = false,
            name = PLAN_NAME,
            programName = PROGRAM_NAME,
            descriptionItems = listOf(
                TangemPayTariffPlan.DescriptionItem(
                    section = TangemPayTariffPlan.Section.PLAN_RELATED,
                    order = 2,
                    title = "Title",
                    body = "Body",
                ),
            ),
            images = listOf(
                TangemPayImage(type = "MAIN", url = "https://img"),
            ),
            fees = listOf(
                TangemPayTariffPlan.Fee(
                    type = TangemPayTariffPlan.Fee.Type.RECURRING,
                    amount = BigDecimal("9.99"),
                    currency = "USD",
                    description = "Monthly",
                    period = TangemPayTariffPlan.Fee.Period.MONTH,
                ),
            ),
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN an image type this app does not know WHEN convert THEN the backend value is kept as is`() {
        // Arrange
        val value = tariffPlan(images = listOf(CustomerMeResponse.Image(type = "SPLASH", url = "https://img")))

        // Act
        val result = TangemPayTariffPlanConverter.convert(value)

        // Assert
        assertThat(result?.images).containsExactly(TangemPayImage(type = "SPLASH", url = "https://img"))
    }

    @Test
    fun `GIVEN unknown enum strings and null optional fields WHEN convert THEN falls back to defaults`() {
        // Arrange
        val value = tariffPlan(
            type = "SOMETHING_NEW",
            descriptionItems = listOf(
                CustomerMeResponse.DescriptionItem(type = "wat", order = null, title = "Title", body = null),
            ),
            images = listOf(CustomerMeResponse.Image(type = null, url = "https://img")),
            fees = listOf(
                CustomerMeResponse.Fee(
                    type = null,
                    amount = BigDecimal.ONE,
                    currency = null,
                    description = null,
                    period = null,
                ),
            ),
        )

        // Act
        val result = TangemPayTariffPlanConverter.convert(value)

        // Assert
        val expected = TangemPayTariffPlan(
            id = PLAN_ID,
            tierId = "SOMETHING_NEW",
            isBasicTier = false,
            name = PLAN_NAME,
            programName = PROGRAM_NAME,
            descriptionItems = listOf(
                TangemPayTariffPlan.DescriptionItem(
                    section = TangemPayTariffPlan.Section.UNKNOWN,
                    order = 0,
                    title = "Title",
                    body = "",
                ),
            ),
            images = listOf(
                TangemPayImage(type = "", url = "https://img"),
            ),
            fees = listOf(
                TangemPayTariffPlan.Fee(
                    type = TangemPayTariffPlan.Fee.Type.UNKNOWN,
                    amount = BigDecimal.ONE,
                    currency = "",
                    description = "",
                    period = null,
                ),
            ),
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `GIVEN nested items with missing required fields WHEN convert THEN filters them out`() {
        // Arrange
        val value = tariffPlan(
            descriptionItems = listOf(
                CustomerMeResponse.DescriptionItem(type = "PLAN_RELATED", order = 1, title = null, body = "Body"),
            ),
            images = listOf(CustomerMeResponse.Image(type = "MAIN", url = null)),
            fees = listOf(
                CustomerMeResponse.Fee(
                    type = "FREE",
                    amount = null,
                    currency = "USD",
                    description = "d",
                    period = null,
                ),
            ),
        )

        // Act
        val result = TangemPayTariffPlanConverter.convert(value)

        // Assert
        val expected = TangemPayTariffPlan(
            id = PLAN_ID,
            tierId = "BASIC",
            isBasicTier = true,
            name = PLAN_NAME,
            programName = PROGRAM_NAME,
            descriptionItems = emptyList(),
            images = emptyList(),
            fees = emptyList(),
        )
        assertThat(result).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        InvalidPlanModel(plan = null),
        InvalidPlanModel(plan = tariffPlan(id = null)),
        InvalidPlanModel(plan = tariffPlan(name = null)),
        InvalidPlanModel(plan = tariffPlan(type = null)),
        InvalidPlanModel(plan = tariffPlan(programName = null)),
    )

    internal data class InvalidPlanModel(val plan: CustomerMeResponse.TariffPlan?)

    private fun tariffPlan(
        id: String? = PLAN_ID,
        type: String? = "BASIC",
        name: String? = PLAN_NAME,
        programName: String? = PROGRAM_NAME,
        descriptionItems: List<CustomerMeResponse.DescriptionItem>? = null,
        images: List<CustomerMeResponse.Image>? = null,
        fees: List<CustomerMeResponse.Fee>? = null,
    ) = CustomerMeResponse.TariffPlan(
        id = id,
        type = type,
        name = name,
        programName = programName,
        descriptionItems = descriptionItems,
        images = images,
        fees = fees,
    )

    private companion object {
        const val PLAN_ID = "plan-1"
        const val PLAN_NAME = "Plus"
        const val PROGRAM_NAME = "program-1"
    }
}
package com.tangem.data.pay.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.pay.models.response.CustomerMeResponse
import com.tangem.domain.models.account.TangemPayTariffPlan
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class TangemPayTariffPlanConverterTest {

    @Test
    fun `GIVEN null value WHEN convert THEN returns null`() {
        assertThat(TangemPayTariffPlanConverter.convert(null)).isNull()
    }

    @Test
    fun `GIVEN missing id WHEN convert THEN returns null`() {
        val value = tariffPlan(id = null)

        assertThat(TangemPayTariffPlanConverter.convert(value)).isNull()
    }

    @Test
    fun `GIVEN missing name WHEN convert THEN returns null`() {
        val value = tariffPlan(name = null)

        assertThat(TangemPayTariffPlanConverter.convert(value)).isNull()
    }

    @Test
    fun `GIVEN missing type WHEN convert THEN returns null`() {
        val value = tariffPlan(type = null)

        assertThat(TangemPayTariffPlanConverter.convert(value)).isNull()
    }

    @Test
    fun `GIVEN missing programName WHEN convert THEN returns null`() {
        val value = tariffPlan(programName = null)

        assertThat(TangemPayTariffPlanConverter.convert(value)).isNull()
    }

    @Test
    fun `GIVEN basic tier in mixed case WHEN convert THEN isBasicTier is true`() {
        val value = tariffPlan(type = "Basic")

        val result = TangemPayTariffPlanConverter.convert(value)

        assertThat(result?.tierId).isEqualTo("Basic")
        assertThat(result?.isBasicTier).isTrue()
    }

    @Test
    fun `GIVEN full valid plan WHEN convert THEN maps all fields`() {
        // GIVEN
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

        // WHEN
        val result = TangemPayTariffPlanConverter.convert(value)

        // THEN
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
                TangemPayTariffPlan.Image(type = TangemPayTariffPlan.Image.Type.MAIN, url = "https://img"),
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
    fun `GIVEN unknown enum strings and null optional fields WHEN convert THEN falls back to defaults`() {
        // GIVEN
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

        // WHEN
        val result = TangemPayTariffPlanConverter.convert(value)

        // THEN
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
                TangemPayTariffPlan.Image(type = TangemPayTariffPlan.Image.Type.UNKNOWN, url = "https://img"),
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
        // GIVEN
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

        // WHEN
        val result = TangemPayTariffPlanConverter.convert(value)

        // THEN
        assertThat(result?.descriptionItems).isEmpty()
        assertThat(result?.images).isEmpty()
        assertThat(result?.fees).isEmpty()
    }

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
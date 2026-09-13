package com.tangem.features.tangempay.common

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.features.tangempay.customerTariffPlan
import com.tangem.features.tangempay.tariffPlanState
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PaymentAccountStatusExtTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN payment account status WHEN read tariff plan THEN expected plan is returned`(model: TariffPlanModel) {
        // Arrange
        val status = paymentStatus(model.value)

        // Act
        val actual = status.tariffPlan

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        TariffPlanModel(value = loadedWith(), expected = PLAN),
        TariffPlanModel(value = loadedWith(plan = null), expected = null),
        TariffPlanModel(value = inactiveWith(), expected = PLAN),
        TariffPlanModel(value = awaitingPlanSelectionWith(), expected = PLAN),
        TariffPlanModel(value = PaymentAccountStatusValue.Loading, expected = null),
        TariffPlanModel(value = PaymentAccountStatusValue.Empty, expected = null),
        TariffPlanModel(value = PaymentAccountStatusValue.NotCreated, expected = null),
        TariffPlanModel(value = PaymentAccountStatusValue.Error.Unavailable, expected = null),
        TariffPlanModel(value = PaymentAccountStatusValue.Error.NotSynced, expected = null),
        TariffPlanModel(value = PaymentAccountStatusValue.Error.ExposedDevice, expected = null),
        TariffPlanModel(value = PaymentAccountStatusValue.Error.CardIssueFailed("customer-id"), expected = null),
        TariffPlanModel(
            value = PaymentAccountStatusValue.Error.CardIssueFailed(
                customerId = "customer-id",
                tariffPlan = tariffPlanState(tariff = PLAN),
            ),
            expected = PLAN,
        ),
        TariffPlanModel(value = mockk<PaymentAccountStatusValue.UnderReview>(relaxed = true), expected = null),
        TariffPlanModel(value = mockk<PaymentAccountStatusValue.IssuingCard>(relaxed = true), expected = null),
        TariffPlanModel(value = mockk<PaymentAccountStatusValue.Deactivated>(relaxed = true), expected = null),
    )

    private fun paymentStatus(value: PaymentAccountStatusValue = loadedWith()): AccountStatus.Payment =
        mockk(relaxed = true) {
            every { this@mockk.value } returns value
        }

    private fun loadedWith(plan: TangemPayCustomerTariffPlan? = PLAN): PaymentAccountStatusValue.Loaded =
        mockk(relaxed = true) {
            every { tariffPlan } returns plan?.let { tariffPlanState(tariff = it) }
        }

    private fun inactiveWith(plan: TangemPayCustomerTariffPlan = PLAN): PaymentAccountStatusValue.Inactive =
        mockk(relaxed = true) {
            every { tariffPlan } returns tariffPlanState(tariff = plan)
        }

    private fun awaitingPlanSelectionWith(
        plan: TangemPayCustomerTariffPlan = PLAN,
    ): PaymentAccountStatusValue.AwaitingPlanSelection = mockk(relaxed = true) {
        every { tariffPlan } returns plan
    }

    internal data class TariffPlanModel(
        val value: PaymentAccountStatusValue,
        val expected: TangemPayCustomerTariffPlan?,
    ) {
        override fun toString(): String = "${value::class.simpleName} -> ${expected?.plan?.tierId}"
    }

    private companion object {
        val PLAN: TangemPayCustomerTariffPlan = customerTariffPlan()
    }
}
package com.tangem.domain.pay.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetCurrentTariffUseCaseTest {

    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk(relaxed = true)
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()

    private val useCase = GetCurrentTariffUseCase(
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
    )

    @Test
    fun `GIVEN status with tariff WHEN invoke THEN fetches and returns source with tariff`() = runTest {
        // GIVEN
        val status = paymentStatus(value = awaitingPlanSelection(StatusSource.ACTUAL, CUSTOMER_TARIFF))
        every { paymentAccountStatusSupplier.invoke(USER_WALLET_ID) } returns flowOf(status)

        // WHEN
        val result = useCase(USER_WALLET_ID)

        // THEN
        assertThat(result).isEqualTo(StatusSource.ACTUAL to CUSTOMER_TARIFF)
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN empty supplier flow WHEN invoke THEN returns null`() = runTest {
        // GIVEN
        every { paymentAccountStatusSupplier.invoke(USER_WALLET_ID) } returns emptyFlow()

        // WHEN
        val result = useCase(USER_WALLET_ID)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `GIVEN status without tariff WHEN invoke THEN returns null`() = runTest {
        // GIVEN
        val status = paymentStatus(value = PaymentAccountStatusValue.NotCreated)
        every { paymentAccountStatusSupplier.invoke(USER_WALLET_ID) } returns flowOf(status)

        // WHEN
        val result = useCase(USER_WALLET_ID)

        // THEN
        assertThat(result).isNull()
    }

    private fun paymentStatus(value: PaymentAccountStatusValue) = AccountStatus.Payment(
        account = mockk(),
        value = value,
    )

    private fun awaitingPlanSelection(source: StatusSource, tariff: TangemPayCustomerTariffPlan) =
        PaymentAccountStatusValue.AwaitingPlanSelection(
            source = source,
            tariffPlan = tariff,
        )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")

        val PLAN = TangemPayTariffPlan(
            id = "plan-plus",
            tierId = "PLUS",
            isBasicTier = false,
            name = "Plus",
            programName = "program-plus",
            descriptionItems = emptyList(),
        )
        val CUSTOMER_TARIFF = TangemPayCustomerTariffPlan(
            status = TangemPayCustomerTariffPlan.Status.ACTIVE,
            source = TangemPayCustomerTariffPlan.Source.CUSTOMER,
            plan = PLAN,
            nextBillingAt = null,
            pendingPlan = null,
            pendingTransitionAt = null,
        )
    }
}
package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class GetCurrentTariffUseCaseTest {

    private val onboardingRepository: OnboardingRepository = mockk()

    private val useCase = GetCurrentTariffUseCase(onboardingRepository = onboardingRepository)

    @Test
    fun `GIVEN customer info with tariff WHEN invoke THEN returns tariff`() = runTest {
        // GIVEN
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns customerInfo(CUSTOMER_TARIFF).right()

        // WHEN
        val result = useCase(USER_WALLET_ID)

        // THEN
        assertThat(result).isEqualTo(CUSTOMER_TARIFF.right())
    }

    @Test
    fun `GIVEN customer info without tariff WHEN invoke THEN returns null`() = runTest {
        // GIVEN
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns customerInfo(null).right()

        // WHEN
        val result = useCase(USER_WALLET_ID)

        // THEN
        assertThat(result).isEqualTo(null.right())
    }

    @Test
    fun `GIVEN customer info request fails WHEN invoke THEN returns error`() = runTest {
        // GIVEN
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns VisaApiError.Unspecified.left()

        // WHEN
        val result = useCase(USER_WALLET_ID)

        // THEN
        assertThat(result).isEqualTo(VisaApiError.Unspecified.left())
    }

    private fun customerInfo(tariffPlan: TangemPayCustomerTariffPlan?): CustomerInfo = mockk {
        every { this@mockk.tariffPlan } returns tariffPlan
    }

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
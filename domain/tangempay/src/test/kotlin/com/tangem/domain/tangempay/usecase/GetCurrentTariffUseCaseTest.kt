package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CustomerInfo
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetCurrentTariffUseCaseTest {

    private val onboardingRepository: OnboardingRepository = mockk()

    private val useCase = GetCurrentTariffUseCase(onboardingRepository = onboardingRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(onboardingRepository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN customer info with tariff, without tariff or failing WHEN invoke THEN returns tariff, null or error`(
        model: CustomerInfoModel,
    ) = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns model.customerInfo

        // Act
        val result = useCase(USER_WALLET_ID)

        // Assert
        assertThat(result).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        CustomerInfoModel(customerInfo = customerInfo(CUSTOMER_TARIFF).right(), expected = CUSTOMER_TARIFF.right()),
        CustomerInfoModel(customerInfo = customerInfo(null).right(), expected = null.right()),
        CustomerInfoModel(customerInfo = TangemPayApiError.Unspecified.left(), expected = TangemPayApiError.Unspecified.left()),
    )

    private fun customerInfo(tariffPlan: TangemPayCustomerTariffPlan?): CustomerInfo = mockk {
        every { this@mockk.tariffPlan } returns tariffPlan
    }

    internal data class CustomerInfoModel(
        val customerInfo: Either<TangemPayApiError, CustomerInfo>,
        val expected: Either<TangemPayApiError, TangemPayCustomerTariffPlan?>,
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
package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerOrderRequest
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.MoneyAccountDepositCredentials
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateOnboardingOrderUseCaseTest {

    private val customerOrderRepository: CustomerOrderRepository = mockk()

    private val useCase = CreateOnboardingOrderUseCase(customerOrderRepository = customerOrderRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(customerOrderRepository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN available products WHEN invoke THEN creates onboarding order with matching parts`(
        model: RequestModel,
    ) = runTest {
        // Arrange
        coEvery { customerOrderRepository.createOrder(any(), any(), any()) } returns order().right()

        // Act
        val actual = useCase(
            userWalletId = USER_WALLET_ID,
            tariffPlanTransition = model.tariffPlanTransition,
            depositCredentials = model.depositCredentials,
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            customerOrderRepository.createOrder(
                userWalletId = USER_WALLET_ID,
                request = model.expectedRequest,
                idempotencyKey = any(),
            )
        }
    }

    @Test
    fun `GIVEN createOrder fails WHEN invoke THEN returns the error`() = runTest {
        // Arrange
        coEvery { customerOrderRepository.createOrder(any(), any(), any()) } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(
            userWalletId = USER_WALLET_ID,
            tariffPlanTransition = TRANSITION,
            depositCredentials = CREDENTIALS,
        )

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    @Test
    fun `GIVEN two invocations WHEN invoke THEN each uses a new idempotency key`() = runTest {
        // Arrange
        val keys = mutableListOf<String>()
        coEvery { customerOrderRepository.createOrder(any(), any(), capture(keys)) } returns order().right()

        // Act
        repeat(times = 2) {
            useCase(
                userWalletId = USER_WALLET_ID,
                tariffPlanTransition = TRANSITION,
                depositCredentials = CREDENTIALS,
            )
        }

        // Assert
        assertThat(keys.toSet()).hasSize(2)
    }

    private fun provideTestModels() = listOf(
        RequestModel(
            tariffPlanTransition = TRANSITION,
            depositCredentials = CREDENTIALS,
            expectedRequest = CustomerOrderRequest.Onboarding(
                tariffPlanTransition = EXPECTED_TRANSITION,
                depositCredentials = CREDENTIALS,
            ),
        ),
        RequestModel(
            tariffPlanTransition = TRANSITION,
            depositCredentials = null,
            expectedRequest = CustomerOrderRequest.Onboarding(
                tariffPlanTransition = EXPECTED_TRANSITION,
                depositCredentials = null,
            ),
        ),
        RequestModel(
            tariffPlanTransition = null,
            depositCredentials = CREDENTIALS,
            expectedRequest = CustomerOrderRequest.Onboarding(
                tariffPlanTransition = null,
                depositCredentials = CREDENTIALS,
            ),
        ),
    )

    internal data class RequestModel(
        val tariffPlanTransition: TangemPayTariffPlanTransition?,
        val depositCredentials: MoneyAccountDepositCredentials?,
        val expectedRequest: CustomerOrderRequest.Onboarding,
    )

    private fun order(): Order = Order(
        id = "order",
        customerId = "customer",
        type = OrderType.ONBOARDING_RAIN,
        status = OrderStatus.NEW,
        step = OrderStep.UNKNOWN,
        stepChangeCode = null,
        productInstanceId = null,
        paymentAccountId = null,
        cardId = null,
        toTariffPlanId = PLAN_ID,
        withdrawTxHash = null,
        createdAt = null,
        updatedAt = null,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val PLAN_ID = "plan-plus"

        val TRANSITION = TangemPayTariffPlanTransition(
            type = TangemPayTariffPlanTransition.Type.ACTIVATION,
            plan = TangemPayTariffPlan(
                id = PLAN_ID,
                tierId = "PLUS",
                isBasicTier = false,
                name = "Plus",
                programName = "program-plus",
                descriptionItems = emptyList(),
            ),
        )

        val EXPECTED_TRANSITION = CustomerOrderRequest.TariffPlanTransition(
            targetTariffPlanId = PLAN_ID,
            transitionType = TangemPayTariffPlanTransition.Type.ACTIVATION,
        )

        val CREDENTIALS = MoneyAccountDepositCredentials(address = "0xdeposit", signature = "0xsig")
    }
}
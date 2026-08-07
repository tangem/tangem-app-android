package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class CancelTariffTransitionUseCaseTest {

    private val cancelTangemPayOrderUseCase: CancelTangemPayOrderUseCase = mockk()
    private val getTariffTransitionUseCase: GetTangemPayTariffPlanTransitionsUseCase = mockk()
    private val submitTariffTransitionUseCase: SubmitTariffTransitionUseCase = mockk()
    private val getCurrentTariffUseCase: GetCurrentTariffUseCase = mockk()

    private val useCase = CancelTariffTransitionUseCase(
        cancelTangemPayOrderUseCase = cancelTangemPayOrderUseCase,
        getTariffTransitionUseCase = getTariffTransitionUseCase,
        submitTariffTransitionUseCase = submitTariffTransitionUseCase,
        getCurrentTariffUseCase = getCurrentTariffUseCase,
    )

    @Test
    fun `GIVEN cancel order fails WHEN invoke THEN returns Left and skips further steps`() = runTest {
        // GIVEN
        coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns VisaApiError.Unspecified.left()

        // WHEN
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { getCurrentTariffUseCase(any()) }
        coVerify(exactly = 0) { getTariffTransitionUseCase(any()) }
        coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN current tariff is null WHEN invoke THEN returns Right and skips transition`() = runTest {
        // GIVEN
        coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns null

        // WHEN
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { getTariffTransitionUseCase(any()) }
        coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN source is not actual WHEN invoke THEN returns Right and skips transition`() = runTest {
        // GIVEN
        coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns (StatusSource.CACHE to DEFAULT_BASIC_TARIFF)

        // WHEN
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { getTariffTransitionUseCase(any()) }
        coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN tariff is not default WHEN invoke THEN returns Right and skips transition`() = runTest {
        // GIVEN
        coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns (StatusSource.ACTUAL to CUSTOMER_PLUS_TARIFF)

        // WHEN
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { getTariffTransitionUseCase(any()) }
        coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN actual default tariff AND getTransitions fails WHEN invoke THEN returns Left`() = runTest {
        // GIVEN
        coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns (StatusSource.ACTUAL to DEFAULT_BASIC_TARIFF)
        coEvery { getTariffTransitionUseCase(USER_WALLET_ID) } returns VisaApiError.Unspecified.left()

        // WHEN
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN actual default tariff AND basic transition exists WHEN invoke THEN submits basic transition`() =
        runTest {
            // GIVEN
            coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
            coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns (StatusSource.ACTUAL to DEFAULT_BASIC_TARIFF)
            coEvery { getTariffTransitionUseCase(USER_WALLET_ID) } returns
                listOf(PLUS_TRANSITION, BASIC_TRANSITION).right()
            coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, BASIC_TRANSITION) } returns Unit.right()

            // WHEN
            val result = useCase(USER_WALLET_ID, ORDER_ID)

            // THEN
            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 1) { submitTariffTransitionUseCase(USER_WALLET_ID, BASIC_TRANSITION) }
        }

    @Test
    fun `GIVEN actual default tariff AND no basic transition WHEN invoke THEN returns Right without submit`() =
        runTest {
            // GIVEN
            coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
            coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns (StatusSource.ACTUAL to DEFAULT_BASIC_TARIFF)
            coEvery { getTariffTransitionUseCase(USER_WALLET_ID) } returns listOf(PLUS_TRANSITION).right()

            // WHEN
            val result = useCase(USER_WALLET_ID, ORDER_ID)

            // THEN
            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
        }

    @Test
    fun `GIVEN actual default tariff AND submit fails WHEN invoke THEN returns Left`() = runTest {
        // GIVEN
        coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns (StatusSource.ACTUAL to DEFAULT_BASIC_TARIFF)
        coEvery { getTariffTransitionUseCase(USER_WALLET_ID) } returns listOf(BASIC_TRANSITION).right()
        coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, BASIC_TRANSITION) } returns
            VisaApiError.Unspecified.left()

        // WHEN
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val ORDER_ID = "order-test-1"

        val BASIC_PLAN = TangemPayTariffPlan(
            id = "plan-basic",
            tierId = "BASIC",
            isBasicTier = true,
            name = "Basic",
            programName = "program-basic",
            descriptionItems = emptyList(),
        )
        val PLUS_PLAN = TangemPayTariffPlan(
            id = "plan-plus",
            tierId = "PLUS",
            isBasicTier = false,
            name = "Plus",
            programName = "program-plus",
            descriptionItems = emptyList(),
        )
        val DEFAULT_BASIC_TARIFF = customerTariff(TangemPayCustomerTariffPlan.Source.DEFAULT, BASIC_PLAN)
        val CUSTOMER_PLUS_TARIFF = customerTariff(TangemPayCustomerTariffPlan.Source.CUSTOMER, PLUS_PLAN)

        val BASIC_TRANSITION = TangemPayTariffPlanTransition(
            type = TangemPayTariffPlanTransition.Type.SYSTEM_DOWNGRADE,
            plan = BASIC_PLAN,
        )
        val PLUS_TRANSITION = TangemPayTariffPlanTransition(
            type = TangemPayTariffPlanTransition.Type.UPGRADE,
            plan = PLUS_PLAN,
        )

        private fun customerTariff(source: TangemPayCustomerTariffPlan.Source, plan: TangemPayTariffPlan) =
            TangemPayCustomerTariffPlan(
                status = TangemPayCustomerTariffPlan.Status.ACTIVE,
                source = source,
                plan = plan,
                nextBillingAt = null,
                pendingPlan = null,
                pendingTransitionAt = null,
            )
    }
}
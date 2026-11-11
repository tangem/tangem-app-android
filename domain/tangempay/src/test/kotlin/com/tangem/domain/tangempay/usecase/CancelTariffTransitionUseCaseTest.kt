package com.tangem.domain.tangempay.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
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

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            cancelTangemPayOrderUseCase,
            getTariffTransitionUseCase,
            submitTariffTransitionUseCase,
            getCurrentTariffUseCase,
        )
    }

    @Test
    fun `GIVEN cancel order fails WHEN invoke THEN returns Left and skips further steps`() = runTest {
        // Arrange
        coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns TangemPayApiError.Unspecified.left()

        // Act
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
        coVerify(exactly = 0) { getCurrentTariffUseCase(any()) }
        coVerify(exactly = 0) { getTariffTransitionUseCase(any()) }
        coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN current tariff is null, fails or is not default WHEN invoke THEN returns Right and skips transition`(
        currentTariff: Either<TangemPayApiError, TangemPayCustomerTariffPlan?>,
    ) = runTest {
        // Arrange
        coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns currentTariff

        // Act
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 0) { getTariffTransitionUseCase(any()) }
        coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN default tariff AND getTransitions fails WHEN invoke THEN returns Left`() = runTest {
        // Arrange
        coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns DEFAULT_BASIC_TARIFF.right()
        coEvery { getTariffTransitionUseCase(USER_WALLET_ID) } returns TangemPayApiError.Unspecified.left()

        // Act
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
        coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN default tariff AND basic transition exists WHEN invoke THEN submits basic transition`() =
        runTest {
            // Arrange
            coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
            coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns DEFAULT_BASIC_TARIFF.right()
            coEvery { getTariffTransitionUseCase(USER_WALLET_ID) } returns
                listOf(PLUS_TRANSITION, BASIC_TRANSITION).right()
            coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, BASIC_TRANSITION) } returns Unit.right()

            // Act
            val result = useCase(USER_WALLET_ID, ORDER_ID)

            // Assert
            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 1) { submitTariffTransitionUseCase(USER_WALLET_ID, BASIC_TRANSITION) }
        }

    @Test
    fun `GIVEN default tariff AND no basic transition WHEN invoke THEN returns Right without submit`() =
        runTest {
            // Arrange
            coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
            coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns DEFAULT_BASIC_TARIFF.right()
            coEvery { getTariffTransitionUseCase(USER_WALLET_ID) } returns listOf(PLUS_TRANSITION).right()

            // Act
            val result = useCase(USER_WALLET_ID, ORDER_ID)

            // Assert
            assertThat(result.isRight()).isTrue()
            coVerify(exactly = 0) { submitTariffTransitionUseCase(any(), any()) }
        }

    @Test
    fun `GIVEN default tariff AND submit fails WHEN invoke THEN returns Left`() = runTest {
        // Arrange
        coEvery { cancelTangemPayOrderUseCase(USER_WALLET_ID, ORDER_ID) } returns Unit.right()
        coEvery { getCurrentTariffUseCase(USER_WALLET_ID) } returns DEFAULT_BASIC_TARIFF.right()
        coEvery { getTariffTransitionUseCase(USER_WALLET_ID) } returns listOf(BASIC_TRANSITION).right()
        coEvery { submitTariffTransitionUseCase(USER_WALLET_ID, BASIC_TRANSITION) } returns
            TangemPayApiError.Unspecified.left()

        // Act
        val result = useCase(USER_WALLET_ID, ORDER_ID)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
    }

    private fun provideTestModels(): List<Either<TangemPayApiError, TangemPayCustomerTariffPlan?>> = listOf(
        null.right(),
        TangemPayApiError.Unspecified.left(),
        CUSTOMER_PLUS_TARIFF.right(),
    )

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
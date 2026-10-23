package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountActivationError
import com.tangem.domain.moneyaccount.model.StartTangemPayOnboardingError
import com.tangem.domain.pay.usecase.CreateOnboardingOrderUseCase
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
internal class StartTangemPayOnboardingUseCaseTest {

    private val activateMoneyAccountUseCase: ActivateMoneyAccountUseCase = mockk()
    private val createOnboardingOrderUseCase: CreateOnboardingOrderUseCase = mockk()

    private val useCase = StartTangemPayOnboardingUseCase(
        activateMoneyAccountUseCase = activateMoneyAccountUseCase,
        createOnboardingOrderUseCase = createOnboardingOrderUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(activateMoneyAccountUseCase, createOnboardingOrderUseCase)
    }

    @Test
    fun `GIVEN banking account available WHEN invoke THEN activates money account and creates order with credentials`() =
        runTest {
            // Arrange
            coEvery { activateMoneyAccountUseCase(USER_WALLET_ID) } returns CREDENTIALS.right()
            coEvery { createOnboardingOrderUseCase(any(), any(), any()) } returns Unit.right()

            // Act
            val actual = useCase(
                userWalletId = USER_WALLET_ID,
                tariffPlanTransition = TRANSITION,
                availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT),
            )

            // Assert
            assertThat(actual).isEqualTo(Unit.right())
            coVerify(exactly = 1) {
                createOnboardingOrderUseCase(
                    userWalletId = USER_WALLET_ID,
                    tariffPlanTransition = TRANSITION,
                    depositCredentials = CREDENTIALS,
                )
            }
        }

    @Test
    fun `GIVEN banking account unavailable WHEN invoke THEN creates order without activation`() = runTest {
        // Arrange
        coEvery { createOnboardingOrderUseCase(any(), any(), any()) } returns Unit.right()

        // Act
        val actual = useCase(
            userWalletId = USER_WALLET_ID,
            tariffPlanTransition = TRANSITION,
            availableProducts = emptySet(),
        )

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) { activateMoneyAccountUseCase(any()) }
        coVerify(exactly = 1) {
            createOnboardingOrderUseCase(
                userWalletId = USER_WALLET_ID,
                tariffPlanTransition = TRANSITION,
                depositCredentials = null,
            )
        }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN activation fails WHEN invoke THEN returns mapped error and skips order`(model: ActivationErrorModel) =
        runTest {
            // Arrange
            coEvery { activateMoneyAccountUseCase(USER_WALLET_ID) } returns model.activationError.left()

            // Act
            val actual = useCase(
                userWalletId = USER_WALLET_ID,
                tariffPlanTransition = TRANSITION,
                availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT),
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
            coVerify(exactly = 0) { createOnboardingOrderUseCase(any(), any(), any()) }
        }

    @Test
    fun `GIVEN order creation fails WHEN invoke THEN returns failed`() = runTest {
        // Arrange
        coEvery {
            createOnboardingOrderUseCase(any(), any(), any())
        } returns VisaApiError.Unspecified.left()

        // Act
        val actual = useCase(
            userWalletId = USER_WALLET_ID,
            tariffPlanTransition = null,
            availableProducts = emptySet(),
        )

        // Assert
        assertThat(actual).isEqualTo(StartTangemPayOnboardingError.Failed.left())
    }

    private fun provideTestModels() = listOf(
        ActivationErrorModel(
            activationError = MoneyAccountActivationError.UserCancelled,
            expected = StartTangemPayOnboardingError.UserCancelled.left(),
        ),
        ActivationErrorModel(
            activationError = MoneyAccountActivationError.Unknown,
            expected = StartTangemPayOnboardingError.Failed.left(),
        ),
    )

    internal data class ActivationErrorModel(
        val activationError: MoneyAccountActivationError,
        val expected: Either<StartTangemPayOnboardingError, Unit>,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")

        val TRANSITION = TangemPayTariffPlanTransition(
            type = TangemPayTariffPlanTransition.Type.ACTIVATION,
            plan = TangemPayTariffPlan(
                id = "plan-plus",
                tierId = "PLUS",
                isBasicTier = false,
                name = "Plus",
                programName = "program-plus",
                descriptionItems = emptyList(),
            ),
        )

        val CREDENTIALS = MoneyAccountDepositCredentials(address = "0xdeposit", signature = "0xsig")
    }
}
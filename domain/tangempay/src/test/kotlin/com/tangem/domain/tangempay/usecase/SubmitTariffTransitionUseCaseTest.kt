package com.tangem.domain.tangempay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class SubmitTariffTransitionUseCaseTest {

    private val createTransitionOrder: CreateTariffPlanTransitionOrderUseCase = mockk()
    private val setPendingTransition: SetTariffPlanPendingTransitionUseCase = mockk()

    private val useCase = SubmitTariffTransitionUseCase(
        createTransitionOrder = createTransitionOrder,
        setPendingTransition = setPendingTransition,
    )

    @ParameterizedTest
    @EnumSource(TangemPayTariffPlanTransition.Type::class, names = ["ACTIVATION", "UPGRADE"])
    fun `GIVEN activation or upgrade transition WHEN invoke THEN creates transition order`(
        type: TangemPayTariffPlanTransition.Type,
    ) = runTest {
        // Arrange
        val transition = transition(type)
        coEvery { createTransitionOrder(USER_WALLET_ID, PLAN_ID, type) } returns Unit.right()

        // Act
        val result = useCase(USER_WALLET_ID, transition)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { createTransitionOrder(USER_WALLET_ID, PLAN_ID, type) }
        coVerify(exactly = 0) { setPendingTransition(any(), any()) }
    }

    @Test
    fun `GIVEN createTransitionOrder fails WHEN invoke THEN returns Left`() = runTest {
        // Arrange
        val transition = transition(TangemPayTariffPlanTransition.Type.UPGRADE)
        coEvery {
            createTransitionOrder(USER_WALLET_ID, PLAN_ID, TangemPayTariffPlanTransition.Type.UPGRADE)
        } returns TangemPayApiError.Unspecified.left()

        // Act
        val result = useCase(USER_WALLET_ID, transition)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
    }

    @Test
    fun `GIVEN downgrade transition WHEN invoke THEN sets pending transition`() = runTest {
        // Arrange
        val transition = transition(TangemPayTariffPlanTransition.Type.DOWNGRADE)
        coEvery { setPendingTransition(USER_WALLET_ID, PLAN_ID) } returns Unit.right()

        // Act
        val result = useCase(USER_WALLET_ID, transition)

        // Assert
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { setPendingTransition(USER_WALLET_ID, PLAN_ID) }
        coVerify(exactly = 0) { createTransitionOrder(any(), any(), any()) }
    }

    @Test
    fun `GIVEN setPendingTransition fails WHEN invoke THEN returns Left`() = runTest {
        // Arrange
        val transition = transition(TangemPayTariffPlanTransition.Type.DOWNGRADE)
        coEvery { setPendingTransition(USER_WALLET_ID, PLAN_ID) } returns TangemPayApiError.Unspecified.left()

        // Act
        val result = useCase(USER_WALLET_ID, transition)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
    }

    @ParameterizedTest
    @EnumSource(TangemPayTariffPlanTransition.Type::class, names = ["SYSTEM_DOWNGRADE", "UNKNOWN"])
    fun `GIVEN system downgrade or unknown transition WHEN invoke THEN returns Left without side effects`(
        type: TangemPayTariffPlanTransition.Type,
    ) = runTest {
        // Arrange
        val transition = transition(type)

        // Act
        val result = useCase(USER_WALLET_ID, transition)

        // Assert
        assertThat(result.leftOrNull()).isEqualTo(TangemPayApiError.Unspecified)
        coVerify(exactly = 0) { createTransitionOrder(any(), any(), any()) }
        coVerify(exactly = 0) { setPendingTransition(any(), any()) }
    }

    private fun transition(type: TangemPayTariffPlanTransition.Type) = TangemPayTariffPlanTransition(
        type = type,
        plan = PLAN,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val PLAN_ID = "plan-plus"

        val PLAN = TangemPayTariffPlan(
            id = PLAN_ID,
            tierId = "PLUS",
            isBasicTier = false,
            name = "Plus",
            programName = "program-plus",
            descriptionItems = emptyList(),
        )
    }
}
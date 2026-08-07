package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class SubmitTariffTransitionUseCaseTest {

    private val createTransitionOrder: CreateTariffPlanTransitionOrderUseCase = mockk()
    private val setPendingTransition: SetTariffPlanPendingTransitionUseCase = mockk()

    private val useCase = SubmitTariffTransitionUseCase(
        createTransitionOrder = createTransitionOrder,
        setPendingTransition = setPendingTransition,
    )

    @Test
    fun `GIVEN activation transition WHEN invoke THEN creates transition order`() = runTest {
        // GIVEN
        val transition = transition(TangemPayTariffPlanTransition.Type.ACTIVATION)
        coEvery {
            createTransitionOrder(USER_WALLET_ID, PLAN_ID, TangemPayTariffPlanTransition.Type.ACTIVATION)
        } returns Unit.right()

        // WHEN
        val result = useCase(USER_WALLET_ID, transition)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) {
            createTransitionOrder(USER_WALLET_ID, PLAN_ID, TangemPayTariffPlanTransition.Type.ACTIVATION)
        }
        coVerify(exactly = 0) { setPendingTransition(any(), any()) }
    }

    @Test
    fun `GIVEN upgrade transition WHEN invoke THEN creates transition order`() = runTest {
        // GIVEN
        val transition = transition(TangemPayTariffPlanTransition.Type.UPGRADE)
        coEvery {
            createTransitionOrder(USER_WALLET_ID, PLAN_ID, TangemPayTariffPlanTransition.Type.UPGRADE)
        } returns Unit.right()

        // WHEN
        val result = useCase(USER_WALLET_ID, transition)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) {
            createTransitionOrder(USER_WALLET_ID, PLAN_ID, TangemPayTariffPlanTransition.Type.UPGRADE)
        }
        coVerify(exactly = 0) { setPendingTransition(any(), any()) }
    }

    @Test
    fun `GIVEN createTransitionOrder fails WHEN invoke THEN returns Left`() = runTest {
        // GIVEN
        val transition = transition(TangemPayTariffPlanTransition.Type.UPGRADE)
        coEvery {
            createTransitionOrder(USER_WALLET_ID, PLAN_ID, TangemPayTariffPlanTransition.Type.UPGRADE)
        } returns VisaApiError.Unspecified.left()

        // WHEN
        val result = useCase(USER_WALLET_ID, transition)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
    }

    @Test
    fun `GIVEN downgrade transition WHEN invoke THEN sets pending transition`() = runTest {
        // GIVEN
        val transition = transition(TangemPayTariffPlanTransition.Type.DOWNGRADE)
        coEvery { setPendingTransition(USER_WALLET_ID, PLAN_ID) } returns Unit.right()

        // WHEN
        val result = useCase(USER_WALLET_ID, transition)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { setPendingTransition(USER_WALLET_ID, PLAN_ID) }
        coVerify(exactly = 0) { createTransitionOrder(any(), any(), any()) }
    }

    @Test
    fun `GIVEN setPendingTransition fails WHEN invoke THEN returns Left`() = runTest {
        // GIVEN
        val transition = transition(TangemPayTariffPlanTransition.Type.DOWNGRADE)
        coEvery { setPendingTransition(USER_WALLET_ID, PLAN_ID) } returns VisaApiError.Unspecified.left()

        // WHEN
        val result = useCase(USER_WALLET_ID, transition)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
    }

    @Test
    fun `GIVEN system downgrade transition WHEN invoke THEN returns Left without side effects`() = runTest {
        // GIVEN
        val transition = transition(TangemPayTariffPlanTransition.Type.SYSTEM_DOWNGRADE)

        // WHEN
        val result = useCase(USER_WALLET_ID, transition)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { createTransitionOrder(any(), any(), any()) }
        coVerify(exactly = 0) { setPendingTransition(any(), any()) }
    }

    @Test
    fun `GIVEN unknown transition WHEN invoke THEN returns Left without side effects`() = runTest {
        // GIVEN
        val transition = transition(TangemPayTariffPlanTransition.Type.UNKNOWN)

        // WHEN
        val result = useCase(USER_WALLET_ID, transition)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
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
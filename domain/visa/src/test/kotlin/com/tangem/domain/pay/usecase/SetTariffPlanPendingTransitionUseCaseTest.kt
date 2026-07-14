package com.tangem.domain.pay.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.repository.TangemPayTariffPlanTransitionsRepository
import com.tangem.domain.visa.error.VisaApiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class SetTariffPlanPendingTransitionUseCaseTest {

    private val repository: TangemPayTariffPlanTransitionsRepository = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk()

    private val useCase = SetTariffPlanPendingTransitionUseCase(
        repository = repository,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
    )

    @Test
    fun `GIVEN setPendingTransition fails WHEN invoke THEN returns Left and skips fetch`() = runTest {
        // GIVEN
        coEvery {
            repository.setPendingTransition(USER_WALLET_ID, PENDING_PLAN_ID)
        } returns VisaApiError.Unspecified.left()

        // WHEN
        val result = useCase(USER_WALLET_ID, PENDING_PLAN_ID)

        // THEN
        assertThat(result.leftOrNull()).isEqualTo(VisaApiError.Unspecified)
        coVerify(exactly = 0) { paymentAccountStatusFetcher.invoke(any<UserWalletId>()) }
    }

    @Test
    fun `GIVEN setPendingTransition succeeds WHEN invoke THEN refreshes account status`() = runTest {
        // GIVEN
        coEvery { repository.setPendingTransition(USER_WALLET_ID, PENDING_PLAN_ID) } returns Unit.right()
        coEvery { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) } returns Unit.right()

        // WHEN
        val result = useCase(USER_WALLET_ID, PENDING_PLAN_ID)

        // THEN
        assertThat(result.isRight()).isTrue()
        coVerify(exactly = 1) { paymentAccountStatusFetcher.invoke(USER_WALLET_ID) }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("aabbcc112233")
        const val PENDING_PLAN_ID = "plan-basic"
    }
}
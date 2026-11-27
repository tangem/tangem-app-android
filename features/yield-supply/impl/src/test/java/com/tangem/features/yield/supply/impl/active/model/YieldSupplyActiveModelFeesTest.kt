package com.tangem.features.yield.supply.impl.active.model

import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.tangem.common.routing.AppRoute.YieldSupplyEntry.AccountKind
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class YieldSupplyActiveModelFeesTest : YieldSupplyActiveModelTestBase() {

    @BeforeEach
    fun setUp() {
        val status = CryptoCurrencyStatus(currency = token, value = CryptoCurrencyStatus.Loading)
        every { currencyStatusResolver.observe(userWalletId, token, any()) } returns flowOf(status.some())
        coEvery { getYieldBoostStatusUseCase(any(), any()) } returns YieldBoostStatus.NotStarted.right()
        coEvery { yieldSupplyGetTokenStatusUseCase(any()) } returns Throwable("no apy").left()
        coEvery { minAmountUseCase(any(), any(), any()) } returns Throwable("no min amount").left()
        coEvery { getCurrentFeeUseCase(any(), any(), any()) } returns Throwable("no fee").left()
        coEvery { getMaxFeeUseCase(any(), any(), any()) } returns Throwable("no max fee").left()
    }

    @Test
    fun `GIVEN currency held outside the portfolio WHEN status loaded THEN min amount and fees use its holder`() =
        runTest {
            // Act
            val model = createModel(
                dispatchers = createTestingCoroutineDispatcherProvider(),
                account = AccountKind.Money,
            )
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { minAmountUseCase(userWalletId, any(), isHeldOutsidePortfolio = true) }
            coVerify(exactly = 1) { getCurrentFeeUseCase(userWalletId, any(), isHeldOutsidePortfolio = true) }
            coVerify(exactly = 1) { getMaxFeeUseCase(userWalletId, any(), isHeldOutsidePortfolio = true) }

            model.onDestroy()
        }

    @Test
    fun `GIVEN a portfolio currency WHEN status loaded THEN min amount and fees use the portfolio`() = runTest {
        // Act
        val model = createModel(dispatchers = createTestingCoroutineDispatcherProvider())
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { minAmountUseCase(userWalletId, any(), isHeldOutsidePortfolio = false) }
        coVerify(exactly = 1) { getCurrentFeeUseCase(userWalletId, any(), isHeldOutsidePortfolio = false) }
        coVerify(exactly = 1) { getMaxFeeUseCase(userWalletId, any(), isHeldOutsidePortfolio = false) }

        model.onDestroy()
    }
}
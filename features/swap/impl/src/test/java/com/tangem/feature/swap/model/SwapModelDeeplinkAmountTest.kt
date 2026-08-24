package com.tangem.feature.swap.model

import arrow.core.Either
import com.google.firebase.perf.FirebasePerformance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.SwapState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapModelDeeplinkAmountTest : SwapModelTestBase() {

    @BeforeEach
    fun setUp() {
        setUpBase()
        // SwapQuotePerformanceTracker.onLoadingStarted() calls FirebasePerformance.getInstance(),
        // which reaches into un-mocked Android framework APIs (android.os.Process) in this plain
        // JVM unit-test environment. Stub it out so the real quotes-loading path can run.
        mockkStatic(FirebasePerformance::class)
        every { FirebasePerformance.getInstance() } returns mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FirebasePerformance::class)
    }

    @Test
    fun `GIVEN deeplink fromAmount WHEN model created THEN quotes requested with that amount`() = runTest {
        // Arrange
        val fromCurrency = mockk<CryptoCurrency>(relaxed = true)
        val toCurrency = mockk<CryptoCurrency>(relaxed = true)
        val fromStatus: SwapCurrencyStatus = swapCurrencyStatus(currency = fromCurrency)
        val toStatus: SwapCurrencyStatus = swapCurrencyStatus(currency = toCurrency)
        val provider: SwapProvider = swapProvider(id = "changelly", type = ExchangeProviderType.CEX)
        val quoteState: SwapState.QuotesLoadedState = quotesLoadedState(provider)

        coEvery { initialCurrenciesResolver.invoke(any(), any(), any(), any(), any()) } returns
            (fromStatus to toStatus)
        // Relaxed-mock generics on Either<TokenListError, CryptoCurrencyStatus?> / Either<Throwable,
        // BigDecimal?> produce a value that throws a ClassCastException once unwrapped further down
        // the chain (updateFeePaidCryptoCurrencyFor / applyDeeplinkInitialAmount) unless explicitly
        // stubbed with a real Either instance.
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any())
        } returns Either.Right(fromStatus.status)
        coEvery {
            getMinimumTransactionAmountSyncUseCase(any(), any())
        } returns Either.Right(null)
        every { swapTransferInteractor.shouldTransferInsteadOfSwap(any(), any()) } returns false
        coEvery {
            swapInteractor.getPair(any(), any(), any())
        } returns Either.Right(emptyList<SwapPairLeast>())
        coEvery { swapInteractor.findProvidersForPairWithCheck(any(), any(), any()) } returns listOf(provider)
        coEvery {
            swapInteractor.findBestQuote(any(), any(), any(), any(), any())
        } returns mapOf(provider to quoteState)

        // Act
        val model = createModel(createParams(fromAmount = BigDecimal("234.678901")))
        advanceUntilIdle()

        // Assert
        coVerify {
            swapInteractor.findBestQuote(
                fromSwapCurrencyStatus = any(),
                toSwapCurrencyStatus = any(),
                providers = any(),
                amountToSwap = "234.678901",
                reduceBalanceBy = any(),
            )
        }
        model.onDestroy()
    }
}
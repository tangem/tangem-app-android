package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketBridgeError
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class PlacePredictionQuoteEngineTest : PlacePredictionModelTestBase() {

    private val engineJob = Job()

    @AfterEach
    fun cancelEngine() {
        engineJob.cancel()
    }

    @Test
    fun `GIVEN engine held WHEN resumed THEN no source is re-read`() = runTest {
        // Arrange
        stubFailedResumeSources()
        val (engine, _) = createEngine()
        advanceUntilIdle()
        engine.holdForSubmit()

        // Act
        engine.onResume()
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 2)

        // Assert
        coVerify(exactly = 0) { getPriceUseCase(assetId = any(), side = any()) }
        coVerify(exactly = 0) { getQuoteUseCase(request = any()) }
        coVerify(exactly = 1) { checkGeoblockUseCase() }
        coVerify(exactly = 1) { refreshRequisites(USER_WALLET_ID) }
        verify(exactly = 0) { balancePoller.subscribe(userWalletId = any()) }
    }

    @Test
    fun `GIVEN sell engine held WHEN resumed THEN position not reloaded`() = runTest {
        // Arrange
        val (engine, _) = createEngine(params = SELL_PARAMS)
        advanceUntilIdle()
        engine.holdForSubmit()

        // Act
        engine.onResume()
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 2)

        // Assert
        coVerify(exactly = 1) { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) }
    }

    @Test
    fun `GIVEN engine resumed while held WHEN released THEN sources re-read once AND loop restarts`() = runTest {
        // Arrange
        stubFailedResumeSources()
        val (engine, _) = createEngine()
        advanceUntilIdle()
        engine.holdForSubmit()
        engine.onResume()
        runCurrent()

        // Act
        engine.release()
        runCurrent()

        // Assert
        coVerify(exactly = 1) { getPriceUseCase(assetId = any(), side = any()) }
        coVerify(exactly = 2) { checkGeoblockUseCase() }
        coVerify(exactly = 2) { refreshRequisites(USER_WALLET_ID) }
        verify(exactly = 1) { balancePoller.subscribe(userWalletId = USER_WALLET_ID) }

        engine.onPause()
    }

    @Test
    fun `GIVEN sell engine resumed while held WHEN released THEN position reloaded`() = runTest {
        // Arrange
        val (engine, _) = createEngine(params = SELL_PARAMS)
        advanceUntilIdle()
        engine.holdForSubmit()
        engine.onResume()
        runCurrent()

        // Act
        engine.release()
        runCurrent()

        // Assert
        coVerify(exactly = 2) { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) }

        engine.onPause()
    }

    @Test
    fun `GIVEN paused engine held WHEN released THEN nothing requested`() = runTest {
        // Arrange
        stubFailedResumeSources()
        val (engine, _) = createEngine()
        advanceUntilIdle()
        engine.holdForSubmit()

        // Act
        engine.release()
        runCurrent()

        // Assert
        coVerify(exactly = 0) { getPriceUseCase(assetId = any(), side = any()) }
        coVerify(exactly = 1) { checkGeoblockUseCase() }
        coVerify(exactly = 1) { refreshRequisites(USER_WALLET_ID) }
        verify(exactly = 0) { balancePoller.subscribe(userWalletId = any()) }
    }

    @Test
    fun `GIVEN resumed engine not held WHEN released THEN nothing extra requested`() = runTest {
        // Arrange
        val (engine, _) = createEngine()
        advanceUntilIdle()
        engine.onResume()
        runCurrent()

        // Act
        engine.release()
        runCurrent()

        // Assert
        coVerify(exactly = 1) { getPriceUseCase(assetId = any(), side = any()) }
        verify(exactly = 1) { balancePoller.subscribe(userWalletId = USER_WALLET_ID) }

        engine.onPause()
    }

    @Test
    fun `GIVEN sell position lost and engine held WHEN quote retried THEN position not reloaded`() = runTest {
        // Arrange
        coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns null.right()
        val (engine, uiState) = createEngine(params = SELL_PARAMS)
        advanceUntilIdle()
        engine.holdForSubmit()

        // Act
        engine.retryQuote()
        advanceUntilIdle()

        // Assert
        assertThat((uiState.value.order as OrderSideUM.Sell).position).isEqualTo(SellPositionUM.NotHeld)
        coVerify(exactly = 1) { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) }
    }

    @Test
    fun `GIVEN placeable quote and engine held WHEN prepared THEN nothing returned`() = runTest {
        // Arrange
        val (engine, uiState) = createEngine()
        advanceUntilIdle()
        engine.onResume()
        engine.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        assertThat(uiState.value.isPrimaryButtonEnabled).isTrue()
        engine.holdForSubmit()

        // Act
        val prepared = engine.prepare()

        // Assert
        assertThat(prepared).isNull()

        engine.onPause()
    }

    @Test
    fun `GIVEN engine held WHEN balance changes THEN balance applied live`() = runTest {
        // Arrange
        val (engine, uiState) = createEngine()
        advanceUntilIdle()
        engine.holdForSubmit()

        // Act
        statusFlow.value = activeStatus(balance = BigDecimal("50"))
        advanceUntilIdle()

        // Assert
        assertThat((uiState.value.order as OrderSideUM.Buy).balance).isEqualTo(BigDecimal("50"))
    }

    @Test
    fun `GIVEN engine held WHEN funding window opens THEN awaiting funds applied live`() = runTest {
        // Arrange
        val (engine, uiState) = createEngine()
        advanceUntilIdle()
        engine.holdForSubmit()

        // Act
        isFundingOpen.value = true
        advanceUntilIdle()

        // Assert
        assertThat(uiState.value.isAwaitingFunds).isTrue()
    }

    private fun stubFailedResumeSources() {
        coEvery { checkGeoblockUseCase() } returns PolymarketOnboardingError.Network.left()
        coEvery { refreshRequisites(USER_WALLET_ID) } returns PolymarketBridgeError.Unavailable.left()
    }

    private fun TestScope.createEngine(
        params: PlacePredictionComponent.Params = BUY_PARAMS,
    ): Pair<PlacePredictionQuoteEngine, MutableStateFlow<PlacePredictionUM>> {
        val dispatchers = createTestingCoroutineDispatcherProvider()
        val uiState = MutableStateFlow(PlacePredictionUM.initial(side = params.side))
        val engine = createQuoteEngineFactory(dispatchers = dispatchers).create(
            params = params,
            uiState = uiState,
            scope = CoroutineScope(dispatchers.mainImmediate + engineJob),
        )
        return engine to uiState
    }

    private companion object {
        val BUY_PARAMS = PlacePredictionComponent.Params.Buy(
            userWalletId = USER_WALLET_ID,
            eventId = EVENT_ID,
            marketId = MARKET_ID,
            assetId = ASSET_ID,
        )
    }
}
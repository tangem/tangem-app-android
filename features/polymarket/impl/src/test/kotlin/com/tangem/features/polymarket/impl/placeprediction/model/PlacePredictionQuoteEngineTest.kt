package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.SellPositionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.TradingPermissionUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.CompletableDeferred
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
    fun `GIVEN engine held WHEN resumed THEN no price or quote requested`() = runTest {
        // Arrange
        val (engine, _) = createEngine()
        advanceUntilIdle()
        engine.holdForSubmit()

        // Act
        engine.onResume()
        advanceTimeBy(delayTimeMillis = QUOTE_POLL_INTERVAL_MILLIS * 2)

        // Assert
        coVerify(exactly = 0) { getPriceUseCase(assetId = any(), side = any()) }
        coVerify(exactly = 0) { getQuoteUseCase(request = any()) }
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
    fun `GIVEN balance emitted before hold WHEN delivered after hold THEN balance not applied`() = runTest {
        // Arrange
        val (engine, uiState) = createEngine()
        advanceUntilIdle()
        statusFlow.value = activeStatus(balance = BigDecimal("50"))

        // Act
        engine.holdForSubmit()
        advanceUntilIdle()

        // Assert
        assertThat((uiState.value.order as OrderSideUM.Buy).balance).isEqualTo(BigDecimal("100"))
    }

    @Test
    fun `GIVEN position read pending WHEN engine held before it answers THEN position not applied`() = runTest {
        // Arrange
        val position = CompletableDeferred<Unit>()
        coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } coAnswers {
            position.await()
            createPosition().right()
        }
        val (engine, uiState) = createEngine(params = SELL_PARAMS)
        runCurrent()

        // Act
        engine.holdForSubmit()
        position.complete(Unit)
        advanceUntilIdle()

        // Assert
        assertThat((uiState.value.order as OrderSideUM.Sell).position).isEqualTo(SellPositionUM.Loading)
    }

    @Test
    fun `GIVEN engine held WHEN balance changes THEN balance not applied`() = runTest {
        // Arrange
        val (engine, uiState) = createEngine()
        advanceUntilIdle()
        engine.holdForSubmit()

        // Act
        statusFlow.value = activeStatus(balance = BigDecimal("50"))
        advanceUntilIdle()

        // Assert
        assertThat((uiState.value.order as OrderSideUM.Buy).balance).isEqualTo(BigDecimal("100"))
    }

    @Test
    fun `GIVEN balance changed while held WHEN released THEN latest balance applied`() = runTest {
        // Arrange
        val (engine, uiState) = createEngine()
        advanceUntilIdle()
        engine.holdForSubmit()
        statusFlow.value = activeStatus(balance = BigDecimal("50"))
        advanceUntilIdle()

        // Act
        engine.release()
        advanceUntilIdle()

        // Assert
        assertThat((uiState.value.order as OrderSideUM.Buy).balance).isEqualTo(BigDecimal("50"))
    }

    @Test
    fun `GIVEN resumed engine held WHEN released THEN price loop restarts`() = runTest {
        // Arrange
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

        engine.onPause()
    }

    @Test
    fun `GIVEN paused engine held WHEN released THEN nothing requested`() = runTest {
        // Arrange
        val (engine, _) = createEngine()
        advanceUntilIdle()
        engine.holdForSubmit()

        // Act
        engine.release()
        runCurrent()

        // Assert
        coVerify(exactly = 0) { getPriceUseCase(assetId = any(), side = any()) }
    }

    @Test
    fun `GIVEN engine held WHEN funding window opens THEN awaiting funds not applied`() = runTest {
        // Arrange
        val (engine, uiState) = createEngine()
        advanceUntilIdle()
        engine.holdForSubmit()

        // Act
        isFundingOpen.value = true
        advanceUntilIdle()

        // Assert
        assertThat(uiState.value.isAwaitingFunds).isFalse()
    }

    @Test
    fun `GIVEN funding window opened while held WHEN released THEN awaiting funds applied`() = runTest {
        // Arrange
        val (engine, uiState) = createEngine()
        advanceUntilIdle()
        engine.holdForSubmit()
        isFundingOpen.value = true
        advanceUntilIdle()

        // Act
        engine.release()

        // Assert
        assertThat(uiState.value.isAwaitingFunds).isTrue()
    }

    @Test
    fun `GIVEN engine held WHEN trading permission answered on resume THEN permission not applied`() = runTest {
        // Arrange
        coEvery { checkGeoblockUseCase() } returns PolymarketOnboardingError.Network.left()
        val (engine, uiState) = createEngine()
        advanceUntilIdle()
        coEvery { checkGeoblockUseCase() } returns false.right()
        engine.holdForSubmit()

        // Act
        engine.onResume()
        runCurrent()

        // Assert
        assertThat(uiState.value.tradingPermission).isEqualTo(TradingPermissionUM.Unknown)

        engine.onPause()
    }

    @Test
    fun `GIVEN trading permission answered while held WHEN released THEN permission applied`() = runTest {
        // Arrange
        coEvery { checkGeoblockUseCase() } returns PolymarketOnboardingError.Network.left()
        val (engine, uiState) = createEngine()
        advanceUntilIdle()
        coEvery { checkGeoblockUseCase() } returns false.right()
        engine.holdForSubmit()
        engine.onResume()
        runCurrent()

        // Act
        engine.release()

        // Assert
        assertThat(uiState.value.tradingPermission).isEqualTo(TradingPermissionUM.Allowed)

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
    fun `GIVEN resumed engine not held WHEN released THEN no extra price request`() = runTest {
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

        engine.onPause()
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
package com.tangem.features.polymarket.impl.placeprediction.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.entity.ViewPositionUM
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PlacePredictionStatusModelTest : PlacePredictionModelTestBase() {

    @Test
    fun `GIVEN a buy fills on a market with a condition id WHEN the position is indexed THEN view position is available`() =
        runTest {
            // Arrange
            coEvery { getEventUseCase(eventId = EVENT_ID) } returns createEvent(conditionId = CONDITION_ID).right()
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            val model = placeAndCreateStatusModel(testScope = this, amountModel = amountModel)
            advanceUntilIdle()

            // Assert
            assertThat(model.viewPosition.value).isEqualTo(ViewPositionUM.Available)
            coVerify(exactly = 1) { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) }
        }

    @Test
    fun `GIVEN the position is not indexed yet WHEN a buy fills THEN the button waits AND the read is retried`() =
        runTest {
            // Arrange
            coEvery { getEventUseCase(eventId = EVENT_ID) } returns createEvent(conditionId = CONDITION_ID).right()
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returnsMany listOf(
                null.right(),
                createPosition().right(),
            )
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            val model = placeAndCreateStatusModel(testScope = this, amountModel = amountModel)
            runCurrent()
            val whileIndexing = model.viewPosition.value
            advanceTimeBy(delayTimeMillis = PolymarketConst.POSITION_INDEXING_RETRY_MILLIS + 1)

            // Assert
            assertThat(whileIndexing).isEqualTo(ViewPositionUM.Awaiting)
            assertThat(model.viewPosition.value).isEqualTo(ViewPositionUM.Available)
            coVerify(exactly = 2) { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) }
        }

    @Test
    fun `GIVEN the position never indexes WHEN the reads run out THEN view position is hidden`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = EVENT_ID) } returns createEvent(conditionId = CONDITION_ID).right()
        coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns null.right()
        val amountModel = createAmountModel(testScope = this)
        advanceUntilIdle()
        amountModel.onResume()
        amountModel.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        val model = placeAndCreateStatusModel(testScope = this, amountModel = amountModel)
        advanceUntilIdle()

        // Assert
        assertThat(model.viewPosition.value).isEqualTo(ViewPositionUM.Hidden)
        coVerify(exactly = PolymarketConst.CREATED_POSITION_READS) {
            positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID)
        }
    }

    @Test
    fun `GIVEN the position indexes on the twelfth read WHEN a buy fills THEN view position is still available`() =
        runTest {
            // Arrange
            coEvery { getEventUseCase(eventId = EVENT_ID) } returns createEvent(conditionId = CONDITION_ID).right()
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returnsMany
                List(size = 11) { null.right() } + createPosition().right()
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            val model = placeAndCreateStatusModel(testScope = this, amountModel = amountModel)
            advanceUntilIdle()

            // Assert
            assertThat(model.viewPosition.value).isEqualTo(ViewPositionUM.Available)
            coVerify(exactly = 12) { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) }
        }

    @Test
    fun `GIVEN the market resolved while indexing WHEN the position is read THEN view position is hidden`() =
        runTest {
            // Arrange
            coEvery { getEventUseCase(eventId = EVENT_ID) } returns createEvent(conditionId = CONDITION_ID).right()
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns
                createPosition().copy(resolutionStatus = PolymarketResolutionStatus.RESOLVED).right()
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            val model = placeAndCreateStatusModel(testScope = this, amountModel = amountModel)
            advanceUntilIdle()

            // Assert
            assertThat(model.viewPosition.value).isEqualTo(ViewPositionUM.Hidden)
            coVerify(exactly = 1) { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) }
        }

    @Test
    fun `GIVEN the position read fails WHEN a buy fills THEN the read is retried like a missing position`() =
        runTest {
            // Arrange
            coEvery { getEventUseCase(eventId = EVENT_ID) } returns createEvent(conditionId = CONDITION_ID).right()
            coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returnsMany listOf(
                PolymarketPositionsError.Unknown(httpCode = 502, detail = null).left(),
                createPosition().right(),
            )
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            val model = placeAndCreateStatusModel(testScope = this, amountModel = amountModel)
            advanceUntilIdle()

            // Assert
            assertThat(model.viewPosition.value).isEqualTo(ViewPositionUM.Available)
        }

    @Test
    fun `GIVEN the market has no condition id WHEN a buy fills THEN view position is hidden AND nothing is read`() =
        runTest {
            // Arrange
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

            // Act
            val model = placeAndCreateStatusModel(testScope = this, amountModel = amountModel)
            advanceUntilIdle()

            // Assert
            assertThat(model.viewPosition.value).isEqualTo(ViewPositionUM.Hidden)
            coVerify(exactly = 0) { positionsRepository.getPosition(addresses = any(), id = any()) }
        }

    @Test
    fun `GIVEN the exchange found no match WHEN place clicked THEN view position is hidden`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = EVENT_ID) } returns createEvent(conditionId = CONDITION_ID).right()
        coEvery { placeOrderUseCase(any(), any(), any()) } returns
            PredictionPlaceOrderError.NoLiquidity(detail = "no orders found to match with FAK order").left()
        val amountModel = createAmountModel(testScope = this)
        advanceUntilIdle()
        amountModel.onResume()
        amountModel.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        val model = placeAndCreateStatusModel(testScope = this, amountModel = amountModel)
        advanceUntilIdle()

        // Assert
        assertThat(model.viewPosition.value).isEqualTo(ViewPositionUM.Hidden)
        coVerify(exactly = 0) { positionsRepository.getPosition(addresses = any(), id = any()) }
    }

    @Test
    fun `GIVEN a sell fills WHEN place clicked THEN view position is hidden`() = runTest {
        // Arrange
        coEvery { getQuoteUseCase(request = any()) } returns createSellQuote().right()
        val amountModel = createAmountModel(testScope = this, params = SELL_PARAMS)
        advanceUntilIdle()
        amountModel.onResume()
        amountModel.onAmountChange(value = "12.80")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)

        // Act
        val model = placeAndCreateStatusModel(testScope = this, amountModel = amountModel)
        advanceUntilIdle()

        // Assert
        assertThat(model.viewPosition.value).isEqualTo(ViewPositionUM.Hidden)
    }

    @Test
    fun `GIVEN the position is indexed WHEN view position clicked THEN main and the position details replace the stack`() =
        runTest {
            // Arrange
            coEvery { getEventUseCase(eventId = EVENT_ID) } returns createEvent(conditionId = CONDITION_ID).right()
            val amountModel = createAmountModel(testScope = this)
            advanceUntilIdle()
            amountModel.onResume()
            amountModel.onAmountChange(value = "10")
            advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
            val model = placeAndCreateStatusModel(testScope = this, amountModel = amountModel)
            advanceUntilIdle()

            // Act
            model.onViewPositionClick()

            // Assert
            verify {
                router.replaceAll(
                    PolymarketRoute.Main(userWalletId = USER_WALLET_ID),
                    PolymarketRoute.PositionDetails(userWalletId = USER_WALLET_ID, positionId = POSITION_ID),
                    onComplete = any(),
                )
            }
        }

    @Test
    fun `GIVEN the position is still indexing WHEN view position clicked THEN nothing navigates`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = EVENT_ID) } returns createEvent(conditionId = CONDITION_ID).right()
        coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returns null.right()
        val amountModel = createAmountModel(testScope = this)
        advanceUntilIdle()
        amountModel.onResume()
        amountModel.onAmountChange(value = "10")
        advanceTimeBy(delayTimeMillis = QUOTE_DEBOUNCE_MILLIS + 1)
        val model = placeAndCreateStatusModel(testScope = this, amountModel = amountModel)
        runCurrent()

        // Act
        model.onViewPositionClick()

        // Assert
        verify(exactly = 0) { router.replaceAll(any<PolymarketRoute.Main>(), any(), onComplete = any()) }
    }

    private fun placeAndCreateStatusModel(
        testScope: TestScope,
        amountModel: PlacePredictionAmountModel,
    ): PlacePredictionStatusModel {
        val summary = createSummaryModel(testScope = testScope, amountModel = amountModel)
        val route = slot<PlacePredictionRoute.Status>()
        every { router.replaceAll(capture(route), onComplete = any()) } returns Unit
        summary.onPlaceClick()
        testScope.runCurrent()
        summary.onDestroy()
        amountModel.onDestroy()
        return createStatusModel(testScope = testScope, params = route.captured)
    }
}
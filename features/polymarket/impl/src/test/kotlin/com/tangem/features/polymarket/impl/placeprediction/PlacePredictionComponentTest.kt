package com.tangem.features.polymarket.impl.placeprediction

import arrow.core.left
import arrow.core.right
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.create
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.start
import com.arkivanov.essenty.lifecycle.stop
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.context.DefaultAppComponentContext
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ModelsEntryPoint
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionOrderFailure
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionAmountModel
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionModelTestBase
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionOrderModel
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionSlippageModel
import com.tangem.features.polymarket.impl.placeprediction.entity.ViewPositionUM
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionStatusModel
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionSummaryModel
import com.tangem.features.polymarket.impl.placeprediction.model.QUOTE_DEBOUNCE_MILLIS
import com.tangem.features.polymarket.impl.placeprediction.model.QUOTE_POLL_INTERVAL_MILLIS
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import dagger.hilt.EntryPoints
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import javax.inject.Provider

@OptIn(ExperimentalCoroutinesApi::class)
internal class PlacePredictionComponentTest : PlacePredictionModelTestBase() {

    private val lifecycle = LifecycleRegistry()
    private val backDispatcher = BackDispatcher()

    @AfterEach
    fun releaseContext() {
        lifecycle.destroy()
        unmockkStatic(EntryPoints::class)
    }

    @Test
    fun `GIVEN summary on the stack WHEN app resumes THEN polling waits until back to amount`() = runTest {
        // Arrange
        createComponent(this)
        runCurrent()
        val amount = models.filterIsInstance<PlacePredictionAmountModel>().single()
        amount.onAmountChange("10")
        advanceTimeBy(QUOTE_DEBOUNCE_MILLIS + 1)
        amount.onNextClick()
        val summary = models.filterIsInstance<PlacePredictionSummaryModel>().single()
        val accepted = summary.uiState.value

        // Act
        lifecycle.pause()
        lifecycle.stop()
        lifecycle.start()
        lifecycle.resume()
        advanceTimeBy(QUOTE_POLL_INTERVAL_MILLIS * 3)

        // Assert
        coVerify(exactly = 1) { getQuoteUseCase(any(), any()) }
        assertThat(summary.uiState.value).isEqualTo(accepted)
        backDispatcher.back()
        runCurrent()
        coVerify(exactly = 2) { getQuoteUseCase(any(), any()) }
        lifecycle.destroy()
    }

    @Test
    fun `GIVEN slippage screen WHEN confirmed THEN amount resumes with a quote for the new slippage`() = runTest {
        // Arrange
        createComponent(this)
        runCurrent()
        val amount = models.filterIsInstance<PlacePredictionAmountModel>().single()
        amount.onAmountChange("10")
        advanceTimeBy(QUOTE_DEBOUNCE_MILLIS + 1)
        amount.onSlippageClick()
        val slippage = models.filterIsInstance<PlacePredictionSlippageModel>().single()
        advanceTimeBy(QUOTE_POLL_INTERVAL_MILLIS * 3)
        coVerify(exactly = 1) { getQuoteUseCase(any(), any()) }

        // Act
        slippage.onSlippageValueChange("5")
        slippage.onSlippageConfirmClick()
        runCurrent()

        // Assert
        assertThat(amount.uiState.value.slippage.percent).isEqualTo(BigDecimal("5"))
        coVerify(exactly = 1) { getQuoteUseCase(any(), match { it.slippagePercent == BigDecimal("5") }) }
        lifecycle.destroy()
    }

    @Test
    fun `GIVEN a submitted order WHEN status replaces the stack THEN position indexing continues without quote polling`() = runTest {
        // Arrange
        coEvery { getEventUseCase(any(), eventId = EVENT_ID) } returns createEvent(conditionId = CONDITION_ID).right()
        coEvery { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) } returnsMany listOf(
            null.right(),
            createPosition().right(),
        )
        createComponent(this)
        runCurrent()
        val amount = models.filterIsInstance<PlacePredictionAmountModel>().single()
        amount.onAmountChange("10")
        advanceTimeBy(QUOTE_DEBOUNCE_MILLIS + 1)
        amount.onNextClick()
        val summary = models.filterIsInstance<PlacePredictionSummaryModel>().single()

        // Act
        summary.onPlaceClick()
        runCurrent()
        val status = models.filterIsInstance<PlacePredictionStatusModel>().single()
        assertThat(status.viewPosition.value).isEqualTo(ViewPositionUM.Awaiting)
        lifecycle.pause()
        lifecycle.resume()
        advanceTimeBy(QUOTE_POLL_INTERVAL_MILLIS * 3)
        backDispatcher.back()

        // Assert
        assertThat(status.viewPosition.value).isEqualTo(ViewPositionUM.Available)
        coVerify(exactly = 2) { positionsRepository.getPosition(addresses = ADDRESSES, id = POSITION_ID) }
        coVerify(exactly = 1) { placeOrderUseCase(any(), any(), any()) }
        coVerify(exactly = 1) { getQuoteUseCase(any(), any()) }
        verify(exactly = 1) { router.pop(any()) }
        lifecycle.destroy()
    }

    @Test
    fun `GIVEN signing on summary WHEN system back is pressed THEN amount stays inactive`() = runTest {
        // Arrange
        val signature = CompletableDeferred<Unit>()
        coEvery { signOrderInteractor(any()) } coAnswers {
            signature.await()
            PredictionOrderFailure.Signing(PolymarketSigningError.UserCancelled).left()
        }
        createComponent(this)
        runCurrent()
        val amount = models.filterIsInstance<PlacePredictionAmountModel>().single()
        amount.onAmountChange("10")
        advanceTimeBy(QUOTE_DEBOUNCE_MILLIS + 1)
        amount.onNextClick()
        val summary = models.filterIsInstance<PlacePredictionSummaryModel>().single()
        summary.onPlaceClick()
        runCurrent()

        // Act
        backDispatcher.back()
        advanceTimeBy(QUOTE_POLL_INTERVAL_MILLIS * 3)
        signature.complete(Unit)
        runCurrent()
        advanceTimeBy(QUOTE_POLL_INTERVAL_MILLIS * 3)

        // Assert
        assertThat(summary.isSubmitting()).isFalse()
        coVerify(exactly = 1) { getQuoteUseCase(any(), any()) }
        verify(exactly = 0) { router.pop(any()) }
        lifecycle.destroy()
    }

    @Test
    fun `GIVEN quick order disabled WHEN the flow opens THEN the amount step comes first`() = runTest {
        // Act
        createComponent(this, isQuickOrderEnabled = false)
        runCurrent()

        // Assert
        assertThat(models.filterIsInstance<PlacePredictionAmountModel>()).hasSize(1)
        assertThat(models.filterIsInstance<PlacePredictionOrderModel>()).isEmpty()
        lifecycle.destroy()
    }

    @Test
    fun `GIVEN quick order enabled WHEN the flow opens THEN the order screen polls the price`() = runTest {
        // Act
        createComponent(this, isQuickOrderEnabled = true)
        runCurrent()

        // Assert
        assertThat(models.filterIsInstance<PlacePredictionOrderModel>()).hasSize(1)
        assertThat(models.filterIsInstance<PlacePredictionAmountModel>()).isEmpty()
        coVerify { getPriceUseCase(assetId = any(), side = any()) }
        lifecycle.destroy()
    }

    @Test
    fun `GIVEN signing on the order screen WHEN system back is pressed THEN the flow stays`() = runTest {
        // Arrange
        val signature = CompletableDeferred<Unit>()
        coEvery { signOrderInteractor(any()) } coAnswers {
            signature.await()
            PredictionOrderFailure.Signing(PolymarketSigningError.UserCancelled).left()
        }
        createComponent(this, isQuickOrderEnabled = true)
        runCurrent()
        val order = models.filterIsInstance<PlacePredictionOrderModel>().single()
        order.onAmountChange("10")
        advanceTimeBy(QUOTE_DEBOUNCE_MILLIS + 1)
        order.onPlaceClick()
        runCurrent()

        // Act
        backDispatcher.back()

        // Assert
        assertThat(order.isSubmitting()).isTrue()
        verify(exactly = 0) { router.pop(any()) }
        signature.complete(Unit)
        runCurrent()
        backDispatcher.back()
        verify(exactly = 1) { router.pop(any()) }
        lifecycle.destroy()
    }

    @Test
    fun `GIVEN signing on the order screen WHEN the app pauses and resumes THEN nothing is requested until it ends`() =
        runTest {
            // Arrange
            val signature = CompletableDeferred<Unit>()
            coEvery { signOrderInteractor(any()) } coAnswers {
                signature.await()
                PredictionOrderFailure.Signing(PolymarketSigningError.UserCancelled).left()
            }
            createComponent(this, isQuickOrderEnabled = true)
            runCurrent()
            val order = models.filterIsInstance<PlacePredictionOrderModel>().single()
            order.onAmountChange("10")
            advanceTimeBy(QUOTE_DEBOUNCE_MILLIS + 1)
            order.onPlaceClick()
            runCurrent()
            clearMocks(getQuoteUseCase, getPriceUseCase, answers = false)

            // Act
            lifecycle.pause()
            lifecycle.resume()
            advanceTimeBy(QUOTE_POLL_INTERVAL_MILLIS * 3)

            // Assert
            coVerify(exactly = 0) { getQuoteUseCase(any()) }
            coVerify(exactly = 0) { getPriceUseCase(assetId = any(), side = any()) }
            signature.complete(Unit)
            runCurrent()
            coVerify(exactly = 1) { getQuoteUseCase(any()) }
            lifecycle.destroy()
        }

    private fun createComponent(scope: TestScope, isQuickOrderEnabled: Boolean = false) {
        val params = slot<ParamsContainer>()
        val childRouter = slot<Router>()
        val builder: ModelComponent.Builder = mockk()
        every { builder.router(capture(childRouter)) } returns builder
        every { builder.uiMessageSender(any()) } returns builder
        every { builder.paramsContainer(capture(params)) } returns builder
        every { builder.build() } returns mockk()
        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), ModelsEntryPoint::class.java) } answers {
            val modelParams = params.captured
            val modelRouter = childRouter.captured
            val entryPoint: ModelsEntryPoint = mockk()
            every { entryPoint.models() } returns mapOf(
                PlacePredictionAmountModel::class.java to Provider<Model> {
                    createAmountModel(scope, modelParams.require(), modelRouter)
                },
                PlacePredictionOrderModel::class.java to Provider<Model> {
                    createOrderModel(scope, modelParams.require(), modelRouter)
                },
                PlacePredictionSummaryModel::class.java to Provider<Model> {
                    createSummaryModel(scope, modelParams.require(), modelRouter)
                },
                PlacePredictionStatusModel::class.java to Provider<Model> {
                    createStatusModel(scope, modelParams.require(), modelRouter)
                },
                PlacePredictionSlippageModel::class.java to Provider<Model> {
                    PlacePredictionSlippageModel(
                        paramsContainer = modelParams,
                        router = modelRouter,
                        dispatchers = scope.createTestingCoroutineDispatcherProvider(),
                    ).also(models::add)
                },
            )
            entryPoint
        }
        val context = DefaultAppComponentContext(
            componentContext = DefaultComponentContext(lifecycle = lifecycle, backHandler = backDispatcher),
            dispatchers = scope.createTestingCoroutineDispatcherProvider(),
            hiltComponentBuilder = builder,
            messageSender = messageSender,
            activity = mockk(relaxed = true),
            replaceRouter = router,
        )
        PlacePredictionComponent(
            appComponentContext = context,
            tokenReceiveComponentFactory = mockk(),
            isQuickOrderEnabled = isQuickOrderEnabled,
            params = PlacePredictionComponent.Params.Buy(USER_WALLET_ID, EVENT_ID, MARKET_ID, ASSET_ID),
        )
        lifecycle.create()
        lifecycle.start()
        lifecycle.resume()
    }
}
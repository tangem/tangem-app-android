package com.tangem.features.polymarket.impl.details.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.EventMessage
import com.tangem.core.ui.message.ModalMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.features.polymarket.impl.R
import com.tangem.features.polymarket.impl.common.region.PredictionRegionAccess
import com.tangem.features.polymarket.impl.common.region.createPredictionRegionAccess
import com.tangem.features.polymarket.impl.common.region.regionRestrictionsMessage
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.details.PolymarketEventDetailsComponent
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketEventDetailsModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getEventUseCase: GetPolymarketEventUseCase = mockk()
    private val shareManager: ShareManager = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)

    private var model: PolymarketEventDetailsModel? = null

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN event loads WHEN model created THEN content shown`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returns createEvent().right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(PolymarketEventDetailsUM.Content::class.java)
    }

    @Test
    fun `GIVEN load fails WHEN model created THEN error AND retry reloads to content`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returnsMany listOf(
            PolymarketEventError.Network.left(),
            createEvent().right(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        val error = model.uiState.value as PolymarketEventDetailsUM.Error
        error.onRetryClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value).isInstanceOf(PolymarketEventDetailsUM.Content::class.java)
    }

    @Test
    fun `WHEN back clicked THEN router pops`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returns createEvent().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onBackClick()

        // Assert
        verify { router.pop(any()) }
    }

    @Test
    fun `GIVEN content WHEN share clicked THEN event link shared`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returns createEvent().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.uiState.value as PolymarketEventDetailsUM.Content).onShareClick()

        // Assert
        verify { shareManager.shareText(text = "https://polymarket.com/event/event-slug") }
    }

    @Test
    fun `GIVEN content WHEN outcome clicked THEN place prediction flow pushed`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returns createEvent().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.uiState.value as PolymarketEventDetailsUM.Content)
            .activeMarkets.single()
            .outcomes.single()
            .onClick()
        advanceUntilIdle()

        // Assert
        verify {
            router.push(
                route = PolymarketRoute.PlacePrediction(
                    userWalletId = UserWalletId("011"),
                    eventId = "event-1",
                    marketId = "market-1",
                    assetId = "asset-1",
                ),
                onComplete = any(),
            )
        }
    }

    @Test
    fun `GIVEN a blocked region WHEN outcome clicked THEN the restriction is explained AND no order opens`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returns createEvent().right()
        val model = createModel(testScope = this, regionAccess = createPredictionRegionAccess(isBlocked = true))
        advanceUntilIdle()
        val message = slot<EventMessage>()
        every { messageSender.send(capture(message)) } returns Unit

        // Act
        (model.uiState.value as PolymarketEventDetailsUM.Content)
            .activeMarkets.single()
            .outcomes.single()
            .onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.push(route = any<PolymarketRoute.PlacePrediction>(), onComplete = any()) }
        assertThat((message.captured as ModalMessage).messageModalUM.content)
            .isEqualTo(regionRestrictionsMessage().messageModalUM.content)
    }

    @Test
    fun `GIVEN content WHEN closed markets clicked THEN section toggles`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returns createEvent().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.uiState.value as PolymarketEventDetailsUM.Content).onClosedMarketsClick()

        // Assert
        assertThat((model.uiState.value as PolymarketEventDetailsUM.Content).isClosedMarketsExpanded).isTrue()
    }

    @Test
    fun `GIVEN content WHEN read more clicked THEN description expands`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returns createEvent().right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.uiState.value as PolymarketEventDetailsUM.Content).onReadMoreClick()

        // Assert
        assertThat((model.uiState.value as PolymarketEventDetailsUM.Content).isDescriptionExpanded).isTrue()
    }

    @Test
    fun `GIVEN content shown WHEN screen resumed THEN the event is refreshed at once`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returnsMany listOf(
            createEvent().right(),
            createEvent(title = "Fresh title").right(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onResume()
        runCurrent()

        // Assert
        coVerify(exactly = 2) { getEventUseCase(eventId = "event-1") }
        assertThat((model.uiState.value as PolymarketEventDetailsUM.Content).title)
            .isEqualTo(stringReference("Fresh title"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN screen resumed WHEN the poll interval passes THEN the event is requested again`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returns createEvent().right()
        val model = createModel(testScope = this)
        model.onResume()
        runCurrent()

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()

        // Assert
        coVerify(exactly = 3) { getEventUseCase(eventId = "event-1") }
        model.onDestroy()
    }

    @Test
    fun `GIVEN screen paused WHEN the poll interval passes THEN the event is not requested`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returns createEvent().right()
        val model = createModel(testScope = this)
        model.onResume()
        runCurrent()

        // Act
        model.onPause()
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { getEventUseCase(eventId = "event-1") }
        model.onDestroy()
    }

    @Test
    fun `GIVEN load failed WHEN screen resumed THEN the event is not polled`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returns PolymarketEventError.Network.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onResume()
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { getEventUseCase(eventId = "event-1") }
        assertThat(model.uiState.value).isInstanceOf(PolymarketEventDetailsUM.Error::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN a refresh fails once WHEN the retry delay passes THEN it is retried silently`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returnsMany listOf(
            createEvent().right(),
            PolymarketEventError.Network.left(),
            createEvent(title = "Fresh title").right(),
        )
        val model = createModel(testScope = this)
        model.onResume()
        runCurrent()

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS + RETRY_DELAY_MILLIS)
        runCurrent()

        // Assert
        coVerify(exactly = 3) { getEventUseCase(eventId = "event-1") }
        assertThat((model.uiState.value as PolymarketEventDetailsUM.Content).title)
            .isEqualTo(stringReference("Fresh title"))
        verify(exactly = 0) { messageSender.send(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN refreshes keep failing WHEN polled THEN the last event stays and outdated is reported once`() =
        runTest {
            // Arrange
            coEvery { getEventUseCase(eventId = "event-1") } returnsMany listOf(
                createEvent().right(),
                PolymarketEventError.Network.left(),
            )
            val model = createModel(testScope = this)
            model.onResume()
            runCurrent()

            // Act
            repeat(times = 2) {
                advanceTimeBy(POLL_INTERVAL_MILLIS + RETRY_DELAY_MILLIS)
                runCurrent()
            }

            // Assert
            coVerify(exactly = 5) { getEventUseCase(eventId = "event-1") }
            assertThat((model.uiState.value as PolymarketEventDetailsUM.Content).title)
                .isEqualTo(stringReference("Event title"))
            verify(exactly = 1) {
                messageSender.send(SnackbarMessage(message = resourceReference(R.string.prediction_main_data_outdated)))
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN a refresh succeeded after failures WHEN it fails again THEN outdated is reported again`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returnsMany listOf(
            createEvent().right(),
            PolymarketEventError.Network.left(),
            PolymarketEventError.Network.left(),
            createEvent().right(),
            PolymarketEventError.Network.left(),
        )
        val model = createModel(testScope = this)
        model.onResume()
        runCurrent()

        // Act
        repeat(times = 3) {
            advanceTimeBy(POLL_INTERVAL_MILLIS + RETRY_DELAY_MILLIS)
            runCurrent()
        }

        // Assert
        verify(exactly = 2) {
            messageSender.send(SnackbarMessage(message = resourceReference(R.string.prediction_main_data_outdated)))
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN closed markets unfolded WHEN the event is refreshed THEN the fold stays open`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returns createEvent().right()
        val model = createModel(testScope = this)
        model.onResume()
        runCurrent()
        (model.uiState.value as PolymarketEventDetailsUM.Content).onClosedMarketsClick()

        // Act
        advanceTimeBy(POLL_INTERVAL_MILLIS)
        runCurrent()

        // Assert
        coVerify(exactly = 2) { getEventUseCase(eventId = "event-1") }
        assertThat((model.uiState.value as PolymarketEventDetailsUM.Content).isClosedMarketsExpanded).isTrue()
        model.onDestroy()
    }

    private fun createModel(
        testScope: TestScope,
        regionAccess: PredictionRegionAccess = createPredictionRegionAccess(),
    ): PolymarketEventDetailsModel {
        return PolymarketEventDetailsModel(
            paramsContainer = MutableParamsContainer(
                value = PolymarketEventDetailsComponent.Params(
                    eventId = "event-1",
                    userWalletId = UserWalletId("011"),
                ),
            ),
            router = router,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getPolymarketEventUseCase = getEventUseCase,
            shareManager = shareManager,
            messageSender = messageSender,
            regionAccess = regionAccess,
        ).also { model = it }
    }

    private fun createEvent(title: String = "Event title"): PolymarketEvent = createPolymarketEvent(title = title)

    private companion object {
        const val POLL_INTERVAL_MILLIS = 10_000L
        const val RETRY_DELAY_MILLIS = 2_000L
    }
}
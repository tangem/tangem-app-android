package com.tangem.features.polymarket.impl.details.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.share.ShareManager
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.features.polymarket.impl.createPolymarketEvent
import com.tangem.features.polymarket.impl.details.PolymarketEventDetailsComponent
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketEventDetailsModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getEventUseCase: GetPolymarketEventUseCase = mockk()
    private val shareManager: ShareManager = mockk(relaxed = true)

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

    private fun createModel(testScope: TestScope): PolymarketEventDetailsModel {
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
        ).also { model = it }
    }

    private fun createEvent(): PolymarketEvent = createPolymarketEvent()
}
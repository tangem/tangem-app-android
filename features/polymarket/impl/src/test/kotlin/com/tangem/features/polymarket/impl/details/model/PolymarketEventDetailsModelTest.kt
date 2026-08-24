package com.tangem.features.polymarket.impl.details.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.share.ShareManager
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.features.polymarket.impl.details.PolymarketEventDetailsComponent
import com.tangem.features.polymarket.impl.details.ui.state.PolymarketEventDetailsUM
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionConfig
import com.tangem.test.core.getEmittedValues
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
    fun `GIVEN content WHEN outcome clicked THEN place prediction sheet requested`() = runTest {
        // Arrange
        coEvery { getEventUseCase(eventId = "event-1") } returns createEvent().right()
        val model = createModel(testScope = this)
        val requests = getEmittedValues(model.sheetRequests)
        advanceUntilIdle()

        // Act
        (model.uiState.value as PolymarketEventDetailsUM.Content)
            .activeMarkets.single()
            .outcomes.single()
            .onClick()
        advanceUntilIdle()

        // Assert
        assertThat(requests).containsExactly(
            PlacePredictionConfig(eventId = "event-1", marketId = "market-1", side = "asset-1"),
        )
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

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private fun createEvent(): PolymarketEvent = PolymarketEvent(
        id = "event-1",
        slug = "event-slug",
        title = "Event title",
        description = "Event description",
        rulesUrl = "https://polymarket.com/rules",
        iconUrl = null,
        imageUrl = null,
        status = PolymarketStatus.ACTIVE,
        startDate = null,
        endDate = null,
        volume = null,
        volume24h = null,
        liquidity = null,
        totalMarketsCount = 1,
        isNegRisk = false,
        displayMode = PolymarketDisplayMode.PLAIN_MARKETS,
        markets = listOf(
            PolymarketMarket(
                id = "market-1",
                eventId = "event-1",
                title = "Market question",
                slug = "market-slug",
                description = "Market description",
                groupItemTitle = null,
                iconUrl = null,
                imageUrl = null,
                status = PolymarketStatus.ACTIVE,
                isNegRisk = false,
                startDate = null,
                endDate = null,
                startDateIso = null,
                endDateIso = null,
                volume = null,
                volume24h = null,
                liquidity = null,
                orderIndex = 0,
                outcomes = listOf(
                    PolymarketOutcome(assetId = "asset-1", title = "Yes", probability = null),
                ),
            ),
        ),
    )
}
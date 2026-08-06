package com.tangem.features.polymarket.impl.main.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.model.PolymarketAccessMode
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsUseCase
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
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
internal class PolymarketMainModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getEventsUseCase: GetPolymarketEventsUseCase = mockk()
    private val getCategoriesUseCase: GetPolymarketCategoriesUseCase = mockk()

    private var model: PolymarketMainModel? = null

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN categories and events load WHEN model created THEN content shows tabs and events`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        coEvery { getEventsUseCase(category = 1) } returns listOf(createEvent()).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value.content as PolymarketMainUM.ContentUM.Content
        assertThat(content.categories.map { it.id to it.isSelected })
            .containsExactly(1 to true, 2 to false)
            .inOrder()
        assertThat(content.events.map { it.id }).containsExactly("event-1")
    }

    @Test
    fun `GIVEN params WHEN model created THEN access mode carried into the state`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        coEvery { getEventsUseCase(category = 1) } returns listOf(createEvent()).right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.accessMode).isEqualTo(PolymarketAccessMode.TRADING)
    }

    @Test
    fun `GIVEN categories fail WHEN model created THEN error state shown`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns DataError.NetworkError.NoInternetConnection.left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.content).isInstanceOf(PolymarketMainUM.ContentUM.Error::class.java)
    }

    @Test
    fun `GIVEN events fail WHEN model created THEN error state shown`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        coEvery { getEventsUseCase(category = 1) } returns DataError.NetworkError.NoInternetConnection.left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.content).isInstanceOf(PolymarketMainUM.ContentUM.Error::class.java)
    }

    @Test
    fun `GIVEN error WHEN retry clicked THEN feed reloads to content`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returnsMany listOf(
            DataError.NetworkError.NoInternetConnection.left(),
            categories().right(),
        )
        coEvery { getEventsUseCase(category = 1) } returns listOf(createEvent()).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.uiState.value.content as PolymarketMainUM.ContentUM.Error).onRetryClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.content).isInstanceOf(PolymarketMainUM.ContentUM.Content::class.java)
    }

    @Test
    fun `GIVEN no events WHEN model created THEN empty state shown`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        coEvery { getEventsUseCase(category = 1) } returns emptyList<PolymarketEvent>().right()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.content).isEqualTo(PolymarketMainUM.ContentUM.Empty)
    }

    @Test
    fun `GIVEN content WHEN another category clicked THEN feed reloads for that category`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        coEvery { getEventsUseCase(category = 1) } returns listOf(createEvent()).right()
        coEvery { getEventsUseCase(category = 2) } returns listOf(createEvent(id = "event-2")).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.uiState.value.content as PolymarketMainUM.ContentUM.Content).categories.single { it.id == 2 }.onClick()
        advanceUntilIdle()

        // Assert
        val content = model.uiState.value.content as PolymarketMainUM.ContentUM.Content
        assertThat(content.categories.map { it.id to it.isSelected })
            .containsExactly(1 to false, 2 to true)
            .inOrder()
        assertThat(content.events.map { it.id }).containsExactly("event-2")
        coVerify(exactly = 1) { getEventsUseCase(category = 2) }
    }

    @Test
    fun `GIVEN content WHEN selected category clicked again THEN feed is not reloaded`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        coEvery { getEventsUseCase(category = 1) } returns listOf(createEvent()).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.uiState.value.content as PolymarketMainUM.ContentUM.Content).categories.single { it.id == 1 }.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { getEventsUseCase(category = 1) }
    }

    @Test
    fun `GIVEN content WHEN event card clicked THEN details route pushed`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        coEvery { getEventsUseCase(category = 1) } returns listOf(createEvent()).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.uiState.value.content as PolymarketMainUM.ContentUM.Content).events.single().onClick()

        // Assert
        verify { router.push(PolymarketRoute.EventDetails(eventId = "event-1"), any()) }
    }

    @Test
    fun `GIVEN content WHEN outcome clicked THEN details route pushed with preselection`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        coEvery { getEventsUseCase(category = 1) } returns listOf(createEvent()).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        (model.uiState.value.content as PolymarketMainUM.ContentUM.Content)
            .events.single()
            .rows.single()
            .outcomes.single()
            .onClick()

        // Assert
        verify {
            router.push(
                PolymarketRoute.EventDetails(eventId = "event-1", marketId = "market-1", assetId = "asset-1"),
                any(),
            )
        }
    }

    @Test
    fun `WHEN back clicked THEN router pops`() = runTest {
        // Arrange
        coEvery { getCategoriesUseCase() } returns categories().right()
        coEvery { getEventsUseCase(category = 1) } returns listOf(createEvent()).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onBackClick()

        // Assert
        verify { router.pop(any()) }
    }

    private fun createModel(testScope: TestScope): PolymarketMainModel {
        return PolymarketMainModel(
            paramsContainer = MutableParamsContainer(
                value = PolymarketMainParams(
                    userWalletId = UserWalletId("011"),
                    accessMode = PolymarketAccessMode.TRADING,
                ),
            ),
            router = router,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getPolymarketEventsUseCase = getEventsUseCase,
            getPolymarketCategoriesUseCase = getCategoriesUseCase,
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

    private fun categories(): List<PolymarketCategory> = listOf(
        PolymarketCategory(id = 1, label = "Trending", iconUrl = null),
        PolymarketCategory(id = 2, label = "Sport", iconUrl = null),
    )

    private fun createEvent(id: String = "event-1"): PolymarketEvent = PolymarketEvent(
        id = id,
        slug = "$id-slug",
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
                eventId = id,
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
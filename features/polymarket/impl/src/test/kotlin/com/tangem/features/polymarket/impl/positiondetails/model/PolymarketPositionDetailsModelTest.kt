package com.tangem.features.polymarket.impl.positiondetails.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketDisplayMode
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.model.PolymarketMarket
import com.tangem.domain.polymarket.model.PolymarketOutcome
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketResolutionStatus
import com.tangem.domain.polymarket.model.PolymarketStatus
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventUseCase
import com.tangem.features.polymarket.impl.common.PolymarketConst
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.features.polymarket.impl.common.formatPolymarketVolume
import com.tangem.features.polymarket.impl.positiondetails.PolymarketPositionDetailsComponent
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Content
import com.tangem.features.polymarket.impl.positions.model.createPolymarketPosition
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketPositionDetailsModelTest {
    private val repository: PolymarketPositionsRepository = mockk()
    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val getEvent: GetPolymarketEventUseCase = mockk()
    private val router: Router = mockk(relaxed = true)
    private val addresses: PolymarketAddresses = mockk()
    private val wallet = UserWalletId("022")
    private val position = createPolymarketPosition()
    private val models = mutableListOf<PolymarketPositionDetailsModel>()

    @BeforeEach
    fun setUp() {
        // Compact volume formatting uses Android ICU, unavailable in JVM model tests.
        mockkStatic(BigDecimal?::formatPolymarketVolume)
        every { BigDecimal("6300000").formatPolymarketVolume() } returns "$6.3M"
        coEvery { deriveAddresses.stored(wallet) } returns addresses
        coEvery { repository.getPosition(addresses, position.id) } returns position.right()
        coEvery { getEvent(any()) } returns PolymarketEventError.Network.left()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(BigDecimal?::formatPolymarketVolume)
    }

    @Test
    fun `GIVEN selected outcome WHEN resumed twice THEN reads only stored wallet addresses and that outcome`() = runModelTest {
        // Arrange
        val model = createModel()
        coVerify(exactly = 0) { repository.getPosition(any(), any()) }

        // Act
        model.onResume()
        model.onResume()
        runCurrent()
        advanceTimeBy(120_000)
        runCurrent()

        // Assert
        assertThat(model.uiState.value.content).isInstanceOf(Content.Data::class.java)
        coVerify(exactly = 1) { deriveAddresses.stored(wallet) }
        coVerify(exactly = 1) { repository.getPosition(addresses, position.id) }
        coVerify(exactly = 1) { getEvent("event") }
        coVerify(exactly = 0) { deriveAddresses.invoke(any()) }
    }

    @Test
    fun `GIVEN failed read WHEN manually retried THEN loads details without automatic retries`() = runModelTest {
        // Arrange
        coEvery { repository.getPosition(addresses, position.id) } returnsMany listOf(
            PolymarketPositionsError.Network.left(), position.right(),
        )
        val model = createModel()
        model.onResume()
        runCurrent()
        val error = model.uiState.value.content as Content.Error

        // Act
        advanceTimeBy(120_000)
        runCurrent()
        assertThat(model.uiState.value.content).isSameInstanceAs(error)
        coVerify(exactly = 1) { repository.getPosition(addresses, position.id) }
        error.onRetry()
        runCurrent()

        // Assert
        assertThat(model.uiState.value.content).isInstanceOf(Content.Data::class.java)
        coVerify(exactly = 2) { repository.getPosition(addresses, position.id) }
    }

    @Test
    fun `GIVEN missing stored addresses WHEN retry succeeds THEN initial error becomes details`() = runModelTest {
        // Arrange
        coEvery { deriveAddresses.stored(wallet) } returnsMany listOf(null, addresses)
        val model = createModel()
        model.onResume()
        runCurrent()

        // Act
        (model.uiState.value.content as Content.Error).onRetry()
        runCurrent()

        // Assert
        assertThat(model.uiState.value.content).isInstanceOf(Content.Data::class.java)
        coVerify(exactly = 1) { repository.getPosition(addresses, position.id) }
        coVerify(exactly = 0) { deriveAddresses.invoke(any()) }
    }

    @Test
    fun `GIVEN absent outcome WHEN opened THEN shows unavailable without loading event`() = runModelTest {
        // Arrange
        coEvery { repository.getPosition(addresses, position.id) } returns null.right()
        val model = createModel()

        // Act
        model.onResume()
        runCurrent()

        // Assert
        assertThat(model.uiState.value.content).isEqualTo(Content.Unavailable)
        coVerify(exactly = 1) { repository.getPosition(addresses, position.id) }
        coVerify(exactly = 0) { getEvent(any()) }
    }

    @Test
    fun `GIVEN resolved holding WHEN opened THEN does not enter claim flow`() = runModelTest {
        // Arrange
        coEvery { repository.getPosition(addresses, position.id) } returns
            position.copy(resolutionStatus = PolymarketResolutionStatus.RESOLVED).right()
        val model = createModel()

        // Act
        model.onResume()
        runCurrent()

        // Assert
        assertThat(model.uiState.value.content).isEqualTo(Content.Unavailable)
        coVerify(exactly = 0) { getEvent(any()) }
        verify(exactly = 0) { router.push(any(), any()) }
    }

    @Test
    fun `GIVEN failed request WHEN paused and old retry tapped THEN no reads until resume`() = runModelTest {
        // Arrange
        coEvery { repository.getPosition(addresses, position.id) } returns PolymarketPositionsError.Network.left()
        val model = createModel()
        model.onResume()
        runCurrent()
        val retry = (model.uiState.value.content as Content.Error).onRetry

        // Act
        model.onPause()
        retry()
        advanceTimeBy(60_000)
        runCurrent()

        // Assert
        coVerify(exactly = 1) { repository.getPosition(any(), any()) }

        // Act
        model.onResume()
        runCurrent()

        // Assert
        coVerify(exactly = 2) { repository.getPosition(addresses, position.id) }
    }

    @Test
    fun `GIVEN cancelled slow read WHEN old response arrives after resume THEN keeps newer value`() = runModelTest {
        // Arrange
        val release = CompletableDeferred<Unit>()
        coEvery { repository.getPosition(addresses, position.id) } coAnswers {
            withContext(NonCancellable) { release.await() }
            position.copy(currentValue = BigDecimal("100")).right()
        }
        val model = createModel()
        model.onResume()
        runCurrent()
        model.onPause()
        coEvery { repository.getPosition(addresses, position.id) } returns position.right()
        model.onResume()
        runCurrent()

        // Act
        release.complete(Unit)
        runCurrent()

        // Assert
        assertThat(model.data().summary.currentValue).isEqualTo(stringReference("$9.97"))
    }

    @Test
    fun `GIVEN slow event WHEN metadata arrives THEN enriches already displayed position without another read`() = runModelTest {
        // Arrange
        val event = CompletableDeferred<PolymarketEvent>()
        coEvery { getEvent("event") } coAnswers { event.await().right() }
        val model = createModel()
        model.onResume()
        runCurrent()
        assertThat(model.data().event?.volume).isNull()
        advanceTimeBy(120_000)
        runCurrent()

        // Act
        event.complete(createEvent())
        runCurrent()

        // Assert
        assertThat(model.data().event?.title).isEqualTo(stringReference("Loaded event"))
        assertThat(model.data().event?.iconUrl).isEqualTo("https://example.com/event.png")
        assertThat(model.data().event?.volume).isEqualTo(stringReference("$6.3M"))
        coVerify(exactly = 1) { repository.getPosition(addresses, position.id) }
        coVerify(exactly = 1) { getEvent("event") }
    }

    @Test
    fun `GIVEN optional event fails WHEN event and close are tapped THEN routes with the selected wallet and outcome`() = runModelTest {
        // Arrange
        val model = createModel()
        model.onResume()
        runCurrent()

        // Act
        requireNotNull(model.data().event?.onClick).invoke()
        model.uiState.value.onCloseClick()

        // Assert
        verify(exactly = 1) {
            router.push(PolymarketRoute.EventDetails("event", wallet, "market", position.id.assetId), any())
        }
        verify(exactly = 1) { router.pop() }
    }

    @Test
    fun `GIVEN a sellable position WHEN close tapped THEN the close-position route carries wallet, position and market`() =
        runModelTest {
            // Arrange
            val model = createModel()
            model.onResume()
            runCurrent()

            // Act
            requireNotNull(model.data().onClosePositionClick).invoke()

            // Assert
            verify(exactly = 1) {
                router.push(PolymarketRoute.ClosePosition(wallet, position.id, "market"), any())
            }
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a position that cannot be sold WHEN opened THEN close is not offered`(model: UnsellableModel) =
        runModelTest {
            // Arrange
            coEvery { repository.getPosition(addresses, position.id) } returns model.position.right()
            val details = createModel()

            // Act
            details.onResume()
            runCurrent()

            // Assert
            assertThat(details.data().onClosePositionClick).isNull()
        }

    @Test
    fun `GIVEN a shown position WHEN it is briefly missing after a sell THEN the read is repeated before giving up`() =
        runModelTest {
            // Arrange
            val model = createModel()
            model.onResume()
            runCurrent()
            coEvery { repository.getPosition(addresses, position.id) } returnsMany listOf(
                null.right(),
                null.right(),
                position.copy(size = BigDecimal("10")).right(),
            )
            model.onPause()

            // Act
            model.onResume()
            runCurrent()
            val whileIndexing = model.uiState.value.content
            advanceTimeBy(PolymarketConst.POSITION_INDEXING_RETRY_MILLIS * 2 + 1)

            // Assert
            assertThat(whileIndexing).isEqualTo(Content.Loading)
            assertThat(model.data().metrics.quantity).isEqualTo(stringReference("10"))
            coVerify(exactly = 4) { repository.getPosition(addresses, position.id) }
        }

    @Test
    fun `GIVEN a shown position WHEN it comes back unchanged THEN it is shown at once AND re-read until it changes`() =
        runModelTest {
            // Arrange
            val model = createModel()
            model.onResume()
            runCurrent()
            coEvery { repository.getPosition(addresses, position.id) } returnsMany listOf(
                position.right(),
                position.right(),
                position.copy(size = BigDecimal("10")).right(),
            )
            model.onPause()

            // Act
            model.onResume()
            runCurrent()
            val atOnce = model.data().metrics.quantity
            advanceTimeBy(PolymarketConst.POSITION_INDEXING_RETRY_MILLIS * 2 + 1)

            // Assert
            assertThat(atOnce).isEqualTo(stringReference("28.09"))
            assertThat(model.data().metrics.quantity).isEqualTo(stringReference("10"))
            coVerify(exactly = 4) { repository.getPosition(addresses, position.id) }
        }

    @Test
    fun `GIVEN a shown position WHEN it comes back changed THEN no further reads are made`() = runModelTest {
        // Arrange
        val model = createModel()
        model.onResume()
        runCurrent()
        coEvery { repository.getPosition(addresses, position.id) } returns position.copy(size = BigDecimal("10")).right()
        model.onPause()

        // Act
        model.onResume()
        advanceTimeBy(PolymarketConst.POSITION_INDEXING_RETRY_MILLIS * PolymarketConst.POSITION_INDEXING_READS + 1)

        // Assert
        assertThat(model.data().metrics.quantity).isEqualTo(stringReference("10"))
        coVerify(exactly = 2) { repository.getPosition(addresses, position.id) }
    }

    @Test
    fun `GIVEN a shown position WHEN it stays missing THEN the reads stop AND the screen closes`() =
        runModelTest {
            // Arrange
            val model = createModel()
            model.onResume()
            runCurrent()
            coEvery { repository.getPosition(addresses, position.id) } returns null.right()
            model.onPause()

            // Act
            model.onResume()
            advanceTimeBy(PolymarketConst.POSITION_INDEXING_RETRY_MILLIS * PolymarketConst.POSITION_INDEXING_READS + 1)

            // Assert
            verify(exactly = 1) { router.pop() }
            assertThat(model.uiState.value.content).isEqualTo(Content.Loading)
            coVerify(exactly = 1 + PolymarketConst.POSITION_INDEXING_READS) {
                repository.getPosition(addresses, position.id)
            }
        }

    @Test
    fun `GIVEN two wallets WHEN details load THEN requests and values stay isolated`() = runModelTest {
        // Arrange
        val otherWallet = UserWalletId("033")
        val otherAddresses = mockk<PolymarketAddresses>()
        coEvery { deriveAddresses.stored(otherWallet) } returns otherAddresses
        coEvery { repository.getPosition(otherAddresses, position.id) } returns
            position.copy(currentValue = BigDecimal("42")).right()
        val first = createModel()
        val second = createModel(otherWallet)

        // Act
        first.onResume()
        second.onResume()
        runCurrent()

        // Assert
        assertThat(first.data().summary.currentValue).isEqualTo(stringReference("$9.97"))
        assertThat(second.data().summary.currentValue).isEqualTo(stringReference("$42.00"))
    }

    @Test
    fun `GIVEN a tradable market WHEN buy tapped THEN the buy route carries wallet, event, market and outcome`() =
        runModelTest {
            // Arrange
            coEvery { getEvent("event") } returns createEvent(markets = listOf(createMarket())).right()
            val model = createModel()
            model.onResume()
            runCurrent()

            // Act
            requireNotNull(model.data().onBuyMoreClick).invoke()

            // Assert
            verify(exactly = 1) {
                router.push(
                    PolymarketRoute.PlacePrediction(wallet, "event", "market-from-event", position.id.assetId),
                    any(),
                )
            }
        }

    @Test
    fun `GIVEN a market that refuses orders WHEN opened THEN buy is not offered`() = runModelTest {
        // Arrange
        coEvery { getEvent("event") } returns
            createEvent(markets = listOf(createMarket(isAcceptingOrders = false))).right()
        val model = createModel()

        // Act
        model.onResume()
        runCurrent()

        // Assert
        assertThat(model.data().onBuyMoreClick).isNull()
    }

    @Test
    fun `GIVEN a position awaiting resolution WHEN opened THEN buy is not offered`() = runModelTest {
        // Arrange
        coEvery { repository.getPosition(addresses, position.id) } returns
            position.copy(resolutionStatus = PolymarketResolutionStatus.DISPUTED).right()
        coEvery { getEvent("event") } returns createEvent(markets = listOf(createMarket())).right()
        val model = createModel()

        // Act
        model.onResume()
        runCurrent()

        // Assert
        assertThat(model.data().onBuyMoreClick).isNull()
    }

    @Test
    fun `GIVEN no market of the event holds the outcome WHEN opened THEN buy is not offered`() = runModelTest {
        // Arrange
        coEvery { getEvent("event") } returns createEvent(markets = listOf(createMarket(assetId = "other"))).right()
        val model = createModel()

        // Act
        model.onResume()
        runCurrent()

        // Assert
        assertThat(model.data().onBuyMoreClick).isNull()
    }

    @Test
    fun `GIVEN the event could not be read WHEN opened THEN buy is not offered`() = runModelTest {
        // Arrange
        val model = createModel()

        // Act
        model.onResume()
        runCurrent()

        // Assert
        assertThat(model.data().onBuyMoreClick).isNull()
    }

    private fun runModelTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            models.forEach { it.onDestroy() }
        }
    }

    private fun PolymarketPositionDetailsModel.data() = uiState.value.content as Content.Data

    private fun TestScope.createModel(userWalletId: UserWalletId = wallet): PolymarketPositionDetailsModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return PolymarketPositionDetailsModel(
            paramsContainer = MutableParamsContainer(PolymarketPositionDetailsComponent.Params(userWalletId, position.id)),
            router = router,
            dispatchers = TestingCoroutineDispatcherProvider(dispatcher, dispatcher, dispatcher, dispatcher, dispatcher),
            repository = repository,
            deriveAddresses = deriveAddresses,
            getEvent = getEvent,
        ).also(models::add)
    }

    private fun createEvent(markets: List<PolymarketMarket> = emptyList()) = PolymarketEvent(
        id = "event",
        slug = "event",
        title = "Loaded event",
        description = "",
        rulesUrl = "",
        iconUrl = "https://example.com/event.png",
        imageUrl = null,
        status = PolymarketStatus.ACTIVE,
        startDate = null,
        endDate = null,
        volume = BigDecimal("6300000"),
        volume24h = null,
        liquidity = null,
        totalMarketsCount = 0,
        isNegRisk = false,
        displayMode = PolymarketDisplayMode.PLAIN_MARKETS,
        markets = markets,
    )

    private fun createMarket(
        assetId: String = position.id.assetId,
        isAcceptingOrders: Boolean = true,
    ) = PolymarketMarket(
        id = "market-from-event",
        conditionId = null,
        eventId = "event",
        title = "Will the event happen?",
        slug = "market",
        description = "",
        groupItemTitle = null,
        iconUrl = null,
        imageUrl = null,
        status = PolymarketStatus.ACTIVE,
        isNegRisk = false,
        isAcceptingOrders = isAcceptingOrders,
        startDate = null,
        endDate = null,
        startDateIso = null,
        endDateIso = null,
        volume = null,
        volume24h = null,
        liquidity = null,
        orderIndex = 0,
        outcomes = listOf(PolymarketOutcome(assetId = assetId, title = "Yes", probability = null)),
    )

    internal data class UnsellableModel(val position: PolymarketPosition.Open)

    private companion object {

        @JvmStatic
        fun provideTestModels() = listOf(
            UnsellableModel(position = createPolymarketPosition().copy(marketId = null)),
            UnsellableModel(position = createPolymarketPosition().copy(isAcceptingOrders = false)),
            UnsellableModel(position = createPolymarketPosition().copy(size = BigDecimal.ZERO)),
        )
    }
}
package com.tangem.features.earn.impl.picker.model

import arrow.core.Either
import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.media.LoopingVideoController
import com.tangem.core.media.LoopingVideoState
import com.tangem.core.media.RemoteClipCache
import com.tangem.core.media.VideoFrameSize
import com.tangem.core.media.VideoSurfaceState
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.api.overview.EarnOverviewFetcher
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.api.ui.EarnRowInfoUM
import com.tangem.features.earn.impl.R
import com.tangem.features.earn.impl.analytics.EarnAddFundsSource
import com.tangem.features.earn.impl.analytics.EarnPickerAnalyticsEvent
import com.tangem.features.earn.impl.fake.FakeEarnPosition
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.createFakeEarnProduct
import com.tangem.features.earn.impl.fake.fakeApyPeriods
import com.tangem.features.earn.impl.fake.fakeReadyState
import com.tangem.features.earn.impl.getfunds.EarnGetTokenSheetRequest
import com.tangem.features.earn.impl.incentive.EarnIncentiveSheetRequest
import com.tangem.features.earn.impl.overview.EarnHeadlineSelector
import com.tangem.features.earn.impl.overview.EarnProductSummaries
import com.tangem.features.earn.impl.overview.EarnProductSummarySupplier
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.overview.EarnSummaryOrder
import com.tangem.features.earn.impl.overview.USER_WALLET_ID
import com.tangem.features.earn.impl.overview.createRate
import com.tangem.features.earn.impl.overview.createSummary
import com.tangem.features.earn.impl.picker.EarnPickerHeroVideo
import com.tangem.features.earn.impl.picker.EarnYieldPickerComponent
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerTab
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerUM
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerUM.ContentUM
import com.tangem.features.earn.impl.picker.ui.state.EarnProductCardUM
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorComponent
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.navigation.EarnNavigator
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.features.earn.product.api.product.EarnActionHandler
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnFreshness
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnProjection
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnRowInfo
import com.tangem.features.earn.product.api.product.EarnRowKind
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.rate.EarnRateBadgeConverter
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.verifySequence
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnPickerModelTest {

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val videoFactory: LoopingVideoController.Factory = mockk()
    private val heroVideo: LoopingVideoController = mockk(relaxed = true)
    private val heroClipCache: RemoteClipCache = mockk()
    private val overviewFetcher: EarnOverviewFetcher = mockk()
    private val summarySupplier: EarnProductSummarySupplier = mockk()
    private val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val accountStatusList: AccountStatusList = mockk()
    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    private lateinit var lending: PickerProduct
    private lateinit var staking: PickerProduct
    private lateinit var vault: PickerProduct

    @BeforeEach
    fun setUp() {
        clearMocks(router, uiMessageSender, urlOpener, analyticsEventHandler, overviewFetcher, summarySupplier, accountStatusListSupplier, accountStatusList)
        clearMocks(videoFactory, heroVideo)
        mockkObject(CryptoCurrencyStatusOperations)
        mockkObject(TangemLogger)
        lending = PickerProduct(createFakeEarnProduct(FakeEarnProductPreset.Lending), readyState(FakeEarnProductPreset.Lending))
        staking = PickerProduct(createFakeEarnProduct(FakeEarnProductPreset.Staking), readyState(FakeEarnProductPreset.Staking))
        vault = PickerProduct(createFakeEarnProduct(FakeEarnProductPreset.Vault), readyState(FakeEarnProductPreset.Vault))
        every { accountStatusListSupplier(USER_WALLET_ID) } returns flowOf(accountStatusList)
        every { videoFactory.createRemote(any(), any(), any(), any(), any(), any()) } returns heroVideo
        stubBalance(BigDecimal.ONE)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
        unmockkObject(TangemLogger)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Selection {

        @Test
        fun `GIVEN no frame emitted WHEN model created THEN uiState is the initial Loading`() = runTest {
            // Arrange
            val model = createModel(frames = MutableSharedFlow())

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.content).isEqualTo(ContentUM.Loading)
            model.onDestroy()
        }

        @Test
        fun `GIVEN preselected product WHEN model created THEN it is selected and uiState shows its title`() = runTest {
            // Arrange
            val frames = MutableStateFlow(frame(summary(lending), summary(vault)))
            val model = createModel(frames = frames, preselected = vault.id)

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.selectedChipIds()).containsExactly("vault")
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN preselected missing from the first frame WHEN model created THEN the first chip is selected`(
            model: PreselectedModel,
        ) = runTest {
            // Arrange
            val frames = MutableStateFlow(frame(summary(lending), summary(vault)))
            val picker = createModel(frames = frames, products = listOf(lending, staking, vault), preselected = model.preselected)

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(picker.uiState.value.selectedChipIds()).containsExactly("lending")
            picker.onDestroy()
        }

        private fun provideTestModels() = listOf(
            PreselectedModel(preselected = null),
            PreselectedModel(preselected = EarnProductId("unknown")),
            PreselectedModel(preselected = EarnProductId("staking")),
        )

        @Test
        fun `GIVEN registered preselected absent from a CACHE frame WHEN a later frame brings it THEN it is kept`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(frame(summary(lending), source = StatusSource.CACHE))
                val model = createModel(
                    frames = frames,
                    products = listOf(lending, staking, vault),
                    preselected = staking.id,
                )
                advanceUntilIdle()
                val whileWaiting = model.uiState.value.content

                // Act
                frames.value = frame(summary(lending), summary(staking), source = StatusSource.ACTUAL)
                advanceUntilIdle()

                // Assert
                assertThat(whileWaiting).isEqualTo(ContentUM.Loading)
                assertThat(model.uiState.value.selectedChipIds()).containsExactly("staking")
                model.onDestroy()
            }

        @Test
        fun `GIVEN registered preselected still loading and another feed failed WHEN its summary arrives THEN it is kept`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(
                    frame(summary(lending), source = StatusSource.ONLY_CACHE, isAwaitingProducts = true),
                )
                val model = createModel(
                    frames = frames,
                    products = listOf(lending, staking, vault),
                    preselected = staking.id,
                )
                advanceUntilIdle()
                val whileWaiting = model.uiState.value.content

                // Act
                frames.value = frame(summary(lending), summary(staking), source = StatusSource.ONLY_CACHE)
                advanceUntilIdle()

                // Assert
                assertThat(whileWaiting).isEqualTo(ContentUM.Loading)
                assertThat(model.uiState.value.selectedChipIds()).containsExactly("staking")
                model.onDestroy()
            }

        @Test
        fun `GIVEN no preselected WHEN a partial CACHE frame comes first THEN the first chip of the complete frame is selected`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(frame(summary(vault), source = StatusSource.CACHE))
                val model = createModel(frames = frames)
                advanceUntilIdle()
                val whileWaiting = model.uiState.value.content

                // Act
                frames.value = frame(summary(lending), summary(vault), source = StatusSource.ACTUAL)
                advanceUntilIdle()

                // Assert
                assertThat(whileWaiting).isEqualTo(ContentUM.Loading)
                assertThat(model.uiState.value.selectedChipIds()).containsExactly("lending")
                model.onDestroy()
            }

        @Test
        fun `GIVEN registered preselected missing from the first complete frame WHEN settled THEN no state shows cards with none selected`() =
            runTest {
                // Arrange
                val published = mutableListOf<EarnPickerUM>()
                val frames = MutableStateFlow(frame(summary(lending), summary(vault)))
                val model = createModel(
                    frames = frames,
                    products = listOf(lending, staking, vault),
                    preselected = staking.id,
                )
                backgroundScope.launch { model.uiState.collect { published += it } }

                // Act
                advanceUntilIdle()

                // Assert
                val withCards = published.filter { it.products.isNotEmpty() }
                assertThat(withCards).isNotEmpty()
                assertThat(withCards.all { state -> state.products.count { it.isSelected } == 1 }).isTrue()
                model.onDestroy()
            }

        @Test
        fun `GIVEN resolved selection WHEN a settled frame drops its product THEN the first remaining product is selected`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(frame(summary(lending), summary(staking), summary(vault)))
                val model = createModel(
                    frames = frames,
                    products = listOf(lending, staking, vault),
                    preselected = lending.id,
                )
                advanceUntilIdle()

                // Act
                frames.value = frame(summary(staking), summary(vault))
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.selectedChipIds()).containsExactly("staking")
                model.onDestroy()
            }

        @Test
        fun `GIVEN resolved selection WHEN a CACHE frame drops its product THEN cards are hidden until a settled frame`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(frame(summary(lending), summary(staking), summary(vault)))
                val model = createModel(
                    frames = frames,
                    products = listOf(lending, staking, vault),
                    preselected = lending.id,
                )
                advanceUntilIdle()

                // Act
                frames.value = frame(summary(staking), summary(vault), source = StatusSource.CACHE)
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.products).isEmpty()
                model.onDestroy()
            }

        @Test
        fun `GIVEN a later frame with another order WHEN emitted THEN selection is unchanged`() = runTest {
            // Arrange
            val frames = MutableStateFlow(frame(summary(lending), summary(vault)))
            val model = createModel(frames = frames)
            advanceUntilIdle()

            // Act
            frames.value = frame(summary(vault), summary(lending), source = StatusSource.ONLY_CACHE)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.selectedChipIds()).containsExactly("lending")
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Clicks {

        @Test
        fun `GIVEN ready state WHEN cta clicked twice THEN the handler got the action twice and was created once`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(frame(summary(lending), summary(vault)))
                val model = createModel(frames = frames, preselected = lending.id)
                advanceUntilIdle()

                // Act
                model.uiState.value.cta.onClick()
                model.uiState.value.cta.onClick()

                // Assert
                val action = lending.readyState().primary.action
                assertThat(lending.handler.actions).containsExactly(action, action)
                assertThat(lending.actionHandlerCalls).isEqualTo(1)
                model.onDestroy()
            }

        @Test
        fun `GIVEN two chips WHEN the other chip clicked THEN the selection and the projection follow it`() = runTest {
            // Arrange
            val frames = MutableStateFlow(frame(summary(lending), summary(staking)))
            val model = createModel(frames = frames, products = listOf(lending, staking), preselected = lending.id)
            val projections = mutableListOf<EarnProjection?>()
            backgroundScope.launch { model.projection.collect { projections += it } }
            advanceUntilIdle()
            val beforeClick = projections.last()

            // Act
            model.uiState.value.card("staking").onClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.selectedChipIds()).containsExactly("staking")
            assertThat(beforeClick).isEqualTo(lending.readyState().projection)
            assertThat(projections.last()).isNull()
            assertThat(lending.actionHandlerCalls).isEqualTo(0)
            assertThat(staking.actionHandlerCalls).isEqualTo(0)
            model.onDestroy()
        }

        @Test
        fun `GIVEN two cards WHEN the other card is clicked before its state arrives THEN it is selected and the content is Loading`() =
            runTest {
                // Arrange
                val pending = PickerProduct(
                    delegate = createFakeEarnProduct(FakeEarnProductPreset.Staking),
                    initialState = readyState(FakeEarnProductPreset.Staking),
                    customState = MutableSharedFlow(),
                )
                val frames = MutableStateFlow(frame(summary(lending), summary(pending)))
                val model = createModel(frames = frames, products = listOf(lending, pending), preselected = lending.id)
                advanceUntilIdle()

                // Act
                model.uiState.value.card("staking").onClick()
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.selectedChipIds()).containsExactly("staking")
                assertThat(model.uiState.value.content).isEqualTo(ContentUM.Loading)
                model.onDestroy()
            }

        @Test
        fun `GIVEN a chip round trip A to B to A WHEN cta clicked each time THEN each product got one handler`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(frame(summary(lending), summary(staking)))
                val model = createModel(frames = frames, products = listOf(lending, staking), preselected = lending.id)
                advanceUntilIdle()

                // Act
                model.uiState.value.cta.onClick()
                model.uiState.value.card("staking").onClick()
                advanceUntilIdle()
                model.uiState.value.cta.onClick()
                model.uiState.value.card("lending").onClick()
                advanceUntilIdle()
                model.uiState.value.cta.onClick()

                // Assert
                assertThat(lending.actionHandlerCalls).isEqualTo(1)
                assertThat(staking.actionHandlerCalls).isEqualTo(1)
                assertThat(lending.handler.actions).hasSize(2)
                assertThat(staking.handler.actions).hasSize(1)
                model.onDestroy()
            }

        @Test
        fun `GIVEN no ETH status WHEN model created THEN the button is disabled`() = runTest {
            // Arrange
            stubBalance(null)
            val model = createModel(frames = MutableStateFlow(frame(summary(lending), summary(vault))), preselected = lending.id)

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.cta.isEnabled).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN ETH balance below the minimum WHEN model created THEN Add funds`() = runTest {
            // Arrange
            stubBalance(BigDecimal.ZERO)
            val model = createModel(frames = MutableStateFlow(frame(summary(lending), summary(vault))), preselected = lending.id)

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.cta.text).isEqualTo(resourceReference(R.string.common_add_funds))
            model.onDestroy()
        }

        @Test
        fun `GIVEN Add funds WHEN cta clicked THEN one Get token request is emitted and neither the router nor a handler is touched`() =
            runTest {
                // Arrange
                stubBalance(BigDecimal.ZERO)
                val requests = mutableListOf<EarnGetTokenSheetRequest>()
                val model = createModel(
                    frames = MutableStateFlow(frame(summary(lending), summary(vault))),
                    preselected = lending.id,
                )
                backgroundScope.launch { model.getTokenSheetRequests.collect { requests += it } }
                runCurrent()
                advanceUntilIdle()

                // Act
                model.uiState.value.cta.onClick()
                runCurrent()

                // Assert
                assertThat(requests).containsExactly(
                    EarnGetTokenSheetRequest(USER_WALLET_ID, currency, EarnAddFundsSource.YieldPicker),
                )
                verify { router wasNot Called }
                assertThat(lending.actionHandlerCalls).isEqualTo(0)
                model.onDestroy()
            }

        @Test
        fun `GIVEN Add funds WHEN clicked THEN the Get token request carries the Yield Picker source`() = runTest {
            // Arrange
            stubBalance(BigDecimal.ZERO)
            val requests = mutableListOf<EarnGetTokenSheetRequest>()
            val model = createModel(
                frames = MutableStateFlow(frame(summary(lending), summary(vault))),
                preselected = lending.id,
            )
            backgroundScope.launch { model.getTokenSheetRequests.collect { requests += it } }
            runCurrent()
            advanceUntilIdle()

            // Act
            model.uiState.value.cta.onClick()
            runCurrent()

            // Assert
            assertThat(requests.map { it.source }).containsExactly(EarnAddFundsSource.YieldPicker)
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Blocking {

        @Test
        fun `GIVEN primary block WHEN cta clicked THEN a dialog with the reason is sent and the handler is not called`() =
            runTest {
                // Arrange
                val block = EarnBlock(reason = stringReference("blocked"))
                lending.stateFlow.value = lending.readyState().let { it.copy(primary = it.primary.copy(block = block)) }
                val sent = slot<DialogMessage>()
                every { uiMessageSender.send(capture(sent)) } returns Unit
                val model = createModel(frames = MutableStateFlow(frame(summary(lending), summary(vault))), preselected = lending.id)
                advanceUntilIdle()

                // Act
                model.uiState.value.cta.onClick()

                // Assert
                assertThat(sent.captured.message).isEqualTo(block.reason)
                assertThat(lending.actionHandlerCalls).isEqualTo(0)
                model.onDestroy()
            }

        @Test
        fun `GIVEN stale data and no product block WHEN cta clicked THEN the out-of-date reason is sent`() = runTest {
            // Arrange
            lending.stateFlow.value = lending.readyState().copy(
                freshness = EarnFreshness(asOf = Instant.fromEpochSeconds(0), isStale = true),
            )
            val sent = slot<DialogMessage>()
            every { uiMessageSender.send(capture(sent)) } returns Unit
            val model = createModel(frames = MutableStateFlow(frame(summary(lending), summary(vault))), preselected = lending.id)
            advanceUntilIdle()

            // Act
            model.uiState.value.cta.onClick()

            // Assert
            val outOfDate = resourceReference(R.string.token_button_unavailability_reason_out_of_date_balance)
            assertThat(sent.captured.message).isEqualTo(outOfDate)
            assertThat(lending.actionHandlerCalls).isEqualTo(0)
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AutoRedirect {

        @Test
        fun `GIVEN two active products and two frames WHEN Auto THEN one replace with the best active product`() = runTest {
            // Arrange
            val active = EarnPositionSummary(cryptoAmount = BigDecimal.ONE)
            val lendingSummary = summary(lending, position = active, rate = "0.05")
            val stakingSummary = summary(staking, position = active, rate = "0.06")
            val frames = MutableStateFlow(frame(lendingSummary, stakingSummary))
            val transformer = slot<(List<Route>) -> List<Route>>()
            val model = createModel(
                frames = frames,
                products = listOf(lending, staking),
                shouldRedirectToActive = true,
            )

            // Act
            advanceUntilIdle()
            frames.value = frame(lendingSummary, stakingSummary, source = StatusSource.ONLY_CACHE)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.navigate(capture(transformer), any()) }
            val stack = listOf(EarnPickerRoute(preselected = null, shouldRedirectToActive = true), FlowOnTop)
            assertThat(transformer.captured(stack)).containsExactly(EarnInfoRoute(staking.id), FlowOnTop).inOrder()
            model.onDestroy()
        }

        @Test
        fun `GIVEN Auto already redirected WHEN the state changes later THEN the content stays Loading`() = runTest {
            // Arrange
            val active = EarnPositionSummary(cryptoAmount = BigDecimal.ONE)
            val frames = MutableStateFlow(frame(summary(lending, position = active), summary(vault)))
            val model = createModel(frames = frames, shouldRedirectToActive = true)
            advanceUntilIdle()

            // Act
            lending.stateFlow.value = lending.readyState().copy(description = stringReference("changed"))
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.navigate(any(), any()) }
            assertThat(model.uiState.value.content).isEqualTo(ContentUM.Loading)
            model.onDestroy()
        }

        @Test
        fun `GIVEN Auto and an active product WHEN frames arrive THEN no published state shows the picker content`() =
            runTest {
                // Arrange
                val published = mutableListOf<EarnPickerUM>()
                val active = EarnPositionSummary(cryptoAmount = BigDecimal.ONE)
                val frames = MutableStateFlow(
                    frame(summary(lending, position = active), summary(vault), source = StatusSource.CACHE),
                )
                val model = createModel(frames = frames, shouldRedirectToActive = true)
                backgroundScope.launch { model.uiState.collect { published += it } }

                // Act
                advanceUntilIdle()
                frames.value = frame(summary(lending, position = active), summary(vault), source = StatusSource.ACTUAL)
                advanceUntilIdle()

                // Assert
                assertThat(published.none { it.content is ContentUM.Ready }).isTrue()
                model.onDestroy()
            }

        @Test
        fun `GIVEN Auto, a failed feed and a pending one WHEN the pending feed brings an active product THEN redirect without the picker`() =
            runTest {
                // Arrange
                val published = mutableListOf<EarnPickerUM>()
                val active = EarnPositionSummary(cryptoAmount = BigDecimal.ONE)
                val frames = MutableStateFlow(
                    frame(summary(vault), source = StatusSource.ONLY_CACHE, isAwaitingProducts = true),
                )
                val model = createModel(frames = frames, shouldRedirectToActive = true)
                backgroundScope.launch { model.uiState.collect { published += it } }

                // Act
                advanceUntilIdle()
                frames.value = frame(
                    summary(lending, position = active),
                    summary(vault),
                    source = StatusSource.ONLY_CACHE,
                )
                advanceUntilIdle()

                // Assert
                assertThat(published.none { it.content is ContentUM.Ready }).isTrue()
                verify(exactly = 1) { router.navigate(any(), any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN Auto, no active product and only another currency pending WHEN model created THEN the picker shows its content`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(
                    frame(summary(lending), summary(vault), pendingCurrencies = setOf(OTHER_CURRENCY_ID)),
                )
                val model = createModel(frames = frames, shouldRedirectToActive = true)

                // Act
                advanceUntilIdle()

                // Assert
                verify(exactly = 0) { router.navigate(any(), any()) }
                assertThat(model.uiState.value.content).isInstanceOf(ContentUM.Ready::class.java)
                model.onDestroy()
            }

        @Test
        fun `GIVEN no active product and ACTUAL WHEN Auto THEN the picker stays with its content`() = runTest {
            // Arrange
            val frames = MutableStateFlow(frame(summary(lending), summary(vault)))
            val model = createModel(frames = frames, shouldRedirectToActive = true)

            // Act
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.navigate(any(), any()) }
            assertThat(model.uiState.value.content).isInstanceOf(ContentUM.Ready::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN no active product and CACHE WHEN Auto THEN Loading until an ACTUAL frame brings the content`() = runTest {
            // Arrange
            val frames = MutableStateFlow(frame(summary(lending), summary(vault), source = StatusSource.CACHE))
            val model = createModel(frames = frames, shouldRedirectToActive = true)
            advanceUntilIdle()
            val whileWaiting = model.uiState.value.content

            // Act
            frames.value = frame(summary(lending), summary(vault), source = StatusSource.ACTUAL)
            advanceUntilIdle()

            // Assert
            assertThat(whileWaiting).isEqualTo(ContentUM.Loading)
            assertThat(model.uiState.value.content).isInstanceOf(ContentUM.Ready::class.java)
            verify(exactly = 0) { router.navigate(any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN Auto showed the picker WHEN a later frame is pending THEN the content stays`() = runTest {
            // Arrange
            val frames = MutableStateFlow(frame(summary(lending), summary(vault)))
            val model = createModel(frames = frames, shouldRedirectToActive = true)
            advanceUntilIdle()

            // Act
            frames.value = frame(summary(lending), summary(vault), source = StatusSource.CACHE)
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.content).isInstanceOf(ContentUM.Ready::class.java)
            verify(exactly = 0) { router.navigate(any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN Auto showed the picker WHEN an active product appears later THEN redirect`() = runTest {
            // Arrange
            val frames = MutableStateFlow(frame(summary(lending), summary(vault)))
            val model = createModel(frames = frames, shouldRedirectToActive = true)
            advanceUntilIdle()

            // Act
            val active = EarnPositionSummary(cryptoAmount = BigDecimal.ONE)
            frames.value = frame(summary(lending, position = active), summary(vault), source = StatusSource.CACHE)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.navigate(any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN no active product and ONLY_CACHE WHEN Auto THEN no replace and the picker stays`() = runTest {
            // Arrange
            val frames = MutableStateFlow(frame(summary(lending), summary(vault), source = StatusSource.ONLY_CACHE))
            val model = createModel(frames = frames, shouldRedirectToActive = true)

            // Act
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.navigate(any(), any()) }
            assertThat(model.uiState.value.content).isInstanceOf(ContentUM.Ready::class.java)
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Unavailable {

        @Test
        fun `GIVEN empty summaries and ACTUAL WHEN model created THEN content is Unavailable`() = runTest {
            // Arrange
            val model = createModel(frames = MutableStateFlow(frame()))

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.content).isInstanceOf(ContentUM.Unavailable::class.java)
            model.onDestroy()
        }

        @Test
        fun `GIVEN empty summaries, a failed feed and a pending one WHEN model created THEN content is Loading`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(frame(source = StatusSource.ONLY_CACHE, isAwaitingProducts = true))
                val model = createModel(frames = frames)

                // Act
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.content).isEqualTo(ContentUM.Loading)
                model.onDestroy()
            }

        @Test
        fun `GIVEN empty summaries and the currency pending WHEN model created THEN content is Loading`() = runTest {
            // Arrange
            val frames = MutableStateFlow(frame(source = StatusSource.ONLY_CACHE, pendingCurrencies = setOf(currency.id)))
            val model = createModel(frames = frames)

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.content).isEqualTo(ContentUM.Loading)
            model.onDestroy()
        }

        @Test
        fun `GIVEN empty summaries and only another currency pending WHEN model created THEN content is Unavailable`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(
                    frame(source = StatusSource.ONLY_CACHE, pendingCurrencies = setOf(OTHER_CURRENCY_ID)),
                )
                val model = createModel(frames = frames)

                // Act
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.content).isInstanceOf(ContentUM.Unavailable::class.java)
                model.onDestroy()
            }

        @Test
        fun `GIVEN Unavailable content WHEN Retry THEN the fetcher is called for the currency`() = runTest {
            // Arrange
            coEvery { overviewFetcher(any()) } returns Unit.right()
            val model = createModel(frames = MutableStateFlow(frame()))
            advanceUntilIdle()

            // Act
            (model.uiState.value.content as ContentUM.Unavailable).onRetryClick()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { overviewFetcher(EarnOverviewFetcher.Params(USER_WALLET_ID, currency)) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the fetcher fails WHEN Retry THEN nothing throws and the content stays Unavailable`() = runTest {
            // Arrange
            coEvery { overviewFetcher(any()) } returns IllegalStateException("offline").left()
            val model = createModel(frames = MutableStateFlow(frame()))
            advanceUntilIdle()

            // Act
            (model.uiState.value.content as ContentUM.Unavailable).onRetryClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.content).isInstanceOf(ContentUM.Unavailable::class.java)
            verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN a product whose state throws WHEN model created THEN the content is Unavailable and the error is logged`() =
            runTest {
                // Arrange
                val failing = PickerProduct(
                    delegate = createFakeEarnProduct(FakeEarnProductPreset.Lending),
                    initialState = readyState(FakeEarnProductPreset.Lending),
                    customState = flow { throw IllegalStateException("offline") },
                )
                val frames = MutableStateFlow(frame(summary(failing), summary(vault)))
                val model = createModel(frames = frames, products = listOf(failing, vault), preselected = failing.id)

                // Act
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value.content).isInstanceOf(ContentUM.Unavailable::class.java)
                verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN a product whose state threw WHEN Retry THEN the state is collected again and the content recovers`() =
            runTest {
                // Arrange
                var stateCalls = 0
                val recovering = PickerProduct(
                    delegate = createFakeEarnProduct(FakeEarnProductPreset.Lending),
                    initialState = readyState(FakeEarnProductPreset.Lending),
                    stateFactory = {
                        if (stateCalls++ == 0) {
                            flow { throw IllegalStateException("offline") }
                        } else {
                            flowOf(readyState(FakeEarnProductPreset.Lending))
                        }
                    },
                )
                val frames = MutableStateFlow(frame(summary(recovering), summary(vault)))
                val model = createModel(frames = frames, products = listOf(recovering, vault), preselected = recovering.id)
                advanceUntilIdle()
                val before = model.uiState.value.content

                // Act
                (before as ContentUM.Unavailable).onRetryClick()
                advanceUntilIdle()

                // Assert
                assertThat(recovering.refreshParams).containsExactly(EarnProduct.Params(USER_WALLET_ID, currency))
                assertThat(model.uiState.value.content).isInstanceOf(ContentUM.Ready::class.java)
                model.onDestroy()
            }

        @Test
        fun `GIVEN a product Unavailable and its refresh fails WHEN Retry THEN it is refreshed and stays Unavailable`() =
            runTest {
                // Arrange
                val failing = PickerProduct(
                    delegate = createFakeEarnProduct(FakeEarnProductPreset.Lending),
                    initialState = EarnProductState.Unavailable,
                    refreshResult = IllegalStateException("offline").left(),
                )
                val frames = MutableStateFlow(frame(summary(failing), summary(vault)))
                val model = createModel(frames = frames, products = listOf(failing, vault), preselected = failing.id)
                advanceUntilIdle()

                // Act
                (model.uiState.value.content as ContentUM.Unavailable).onRetryClick()
                advanceUntilIdle()

                // Assert
                assertThat(failing.refreshParams).containsExactly(EarnProduct.Params(USER_WALLET_ID, currency))
                assertThat(model.uiState.value.content).isInstanceOf(ContentUM.Unavailable::class.java)
                verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN a product refresh in flight WHEN the screen closes THEN the refresh still finishes`() = runTest {
            // Arrange
            val unavailable = PickerProduct(
                delegate = createFakeEarnProduct(FakeEarnProductPreset.Lending),
                initialState = EarnProductState.Unavailable,
            )
            unavailable.refreshGate = CompletableDeferred()
            val frames = MutableStateFlow(frame(summary(unavailable), summary(vault)))
            val model = createModel(frames = frames, products = listOf(unavailable, vault), preselected = unavailable.id)
            advanceUntilIdle()
            (model.uiState.value.content as ContentUM.Unavailable).onRetryClick()
            runCurrent()

            // Act
            model.onDestroy()
            unavailable.refreshGate?.complete(Unit)
            advanceUntilIdle()

            // Assert
            assertThat(unavailable.refreshesFinished).isEqualTo(1)
        }

        @Test
        fun `GIVEN an overview fetch in flight WHEN the screen closes THEN the fetch still finishes`() = runTest {
            // Arrange
            val gate = CompletableDeferred<Unit>()
            var finished = 0
            coEvery { overviewFetcher(any()) } coAnswers {
                gate.await()
                finished++
                Unit.right()
            }
            val model = createModel(frames = MutableStateFlow(frame()))
            advanceUntilIdle()
            (model.uiState.value.content as ContentUM.Unavailable).onRetryClick()
            runCurrent()

            // Act
            model.onDestroy()
            gate.complete(Unit)
            advanceUntilIdle()

            // Assert
            assertThat(finished).isEqualTo(1)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Misc {

        @Test
        fun `GIVEN a row with info WHEN its onInfoClick THEN rowInfo holds its sheet`() = runTest {
            // Arrange
            val info = EarnRowInfo.Text(title = stringReference("title"), text = stringReference("description"))
            val row = EarnRow(stringReference("label"), EarnValue.Text(stringReference("value")), info)
            lending.stateFlow.value = lending.readyState().copy(details = listOf(row))
            val model = createModel(frames = MutableStateFlow(frame(summary(lending), summary(vault))), preselected = lending.id)
            advanceUntilIdle()

            // Act
            model.uiState.value.ready().details.single().onInfoClick?.invoke()

            // Assert
            val expected = EarnRowInfoUM.Text(title = stringReference("title"), text = stringReference("description"))
            assertThat(model.rowInfo.value).isEqualTo(expected)
            verify(exactly = 0) { uiMessageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN an open row info WHEN onRowInfoDismiss THEN rowInfo is cleared`() = runTest {
            // Arrange
            val info = EarnRowInfo.Text(title = stringReference("title"), text = stringReference("description"))
            val row = EarnRow(stringReference("label"), EarnValue.Text(stringReference("value")), info)
            lending.stateFlow.value = lending.readyState().copy(details = listOf(row))
            val model = createModel(
                frames = MutableStateFlow(frame(summary(lending), summary(vault))),
                preselected = lending.id,
            )
            advanceUntilIdle()
            model.uiState.value.ready().details.single().onInfoClick?.invoke()

            // Act
            model.onRowInfoDismiss()

            // Assert
            assertThat(model.rowInfo.value).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN a link row WHEN its onValueClick THEN the url is opened once`() = runTest {
            // Arrange
            val link = EarnRow(stringReference("Protocol"), EarnValue.Link(stringReference("Aave"), "https://aave.com"))
            lending.stateFlow.value = lending.readyState().copy(details = listOf(link))
            val model = createModel(frames = MutableStateFlow(frame(summary(lending), summary(vault))), preselected = lending.id)
            advanceUntilIdle()

            // Act
            model.uiState.value.ready().details.single().onValueClick?.invoke()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl("https://aave.com") }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the picker WHEN onClose THEN the router pops`() = runTest {
            // Arrange
            val model = createModel(frames = MutableStateFlow(frame(summary(lending), summary(vault))), preselected = lending.id)
            advanceUntilIdle()

            // Act
            model.uiState.value.onClose()

            // Assert
            verify(exactly = 1) { router.pop(any()) }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IncentiveSheet {

        @Test
        fun `GIVEN Lending with incentive WHEN the badge is clicked THEN a request carries its rate, periods and history`() =
            runTest {
                // Arrange
                val requests = mutableListOf<EarnIncentiveSheetRequest>()
                val model = createModel(
                    frames = MutableStateFlow(frame(summary(lending), summary(vault))),
                    preselected = lending.id,
                )
                backgroundScope.launch { model.incentiveSheetRequests.collect { requests += it } }
                runCurrent()
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value.ready().incentive).onClick()
                runCurrent()

                // Assert
                val actual = requests.single()
                assertThat(actual.rate).isEqualTo(lending.readyState().rate)
                assertThat(actual.periods).isEqualTo(fakeApyPeriods)
                assertThat(actual.history(actual.periods.first()).getOrNull()?.average).isEqualTo(BigDecimal("0.052"))
                model.onDestroy()
            }

        @Test
        fun `GIVEN Ready with empty APY periods WHEN the badge is clicked THEN no request and the error is logged`() =
            runTest {
                // Arrange
                val requests = mutableListOf<EarnIncentiveSheetRequest>()
                val withoutPeriods = PickerProduct(
                    delegate = createFakeEarnProduct(FakeEarnProductPreset.Lending),
                    initialState = readyState(FakeEarnProductPreset.Lending).copy(apyPeriods = emptyList()),
                )
                val model = createModel(
                    frames = MutableStateFlow(frame(summary(withoutPeriods), summary(vault))),
                    products = listOf(withoutPeriods, vault),
                    preselected = withoutPeriods.id,
                )
                backgroundScope.launch { model.incentiveSheetRequests.collect { requests += it } }
                runCurrent()
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value.ready().incentive).onClick()
                runCurrent()

                // Assert
                assertThat(requests).isEmpty()
                verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN Loading selection WHEN onIncentiveClick THEN no request`() = runTest {
            // Arrange
            val requests = mutableListOf<EarnIncentiveSheetRequest>()
            val pending = PickerProduct(
                delegate = createFakeEarnProduct(FakeEarnProductPreset.Lending),
                initialState = readyState(FakeEarnProductPreset.Lending),
                customState = MutableSharedFlow(),
            )
            val model = createModel(
                frames = MutableStateFlow(frame(summary(pending), summary(vault))),
                products = listOf(pending, vault),
                preselected = pending.id,
            )
            backgroundScope.launch { model.incentiveSheetRequests.collect { requests += it } }
            runCurrent()
            advanceUntilIdle()

            // Act
            model.onIncentiveClick()
            runCurrent()

            // Assert
            assertThat(model.uiState.value.content).isEqualTo(ContentUM.Loading)
            assertThat(requests).isEmpty()
            model.onDestroy()
        }

        @Test
        fun `GIVEN two products with incentives WHEN the second chip is selected and the badge clicked THEN the request is the second one's`() =
            runTest {
                // Arrange
                val requests = mutableListOf<EarnIncentiveSheetRequest>()
                val second = PickerProduct(
                    delegate = createFakeEarnProduct(FakeEarnProductPreset.Vault),
                    initialState = readyState(FakeEarnProductPreset.Vault).copy(
                        rate = EarnProductRate(BigDecimal("0.07"), EarnRateType.APY),
                        incentiveRate = EarnProductRate(BigDecimal("0.02"), EarnRateType.APY),
                    ),
                )
                val model = createModel(
                    frames = MutableStateFlow(frame(summary(lending), summary(second))),
                    products = listOf(lending, second),
                    preselected = lending.id,
                )
                backgroundScope.launch { model.incentiveSheetRequests.collect { requests += it } }
                runCurrent()
                advanceUntilIdle()
                model.uiState.value.card("vault").onClick()
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value.ready().incentive).onClick()
                runCurrent()

                // Assert
                assertThat(requests.single().rate).isEqualTo(second.readyState().rate)
                assertThat(requests.single().rate).isNotEqualTo(lending.readyState().rate)
                assertThat(requests.single().history(fakeApyPeriods.first()).isLeft()).isTrue()
                model.onDestroy()
            }

        @Test
        fun `GIVEN a product whose apyHistory fails WHEN the request history is called THEN it returns Left`() =
            runTest {
                // Arrange
                val requests = mutableListOf<EarnIncentiveSheetRequest>()
                val failing = PickerProduct(
                    delegate = createFakeEarnProduct(FakeEarnProductPreset.Vault),
                    initialState = readyState(FakeEarnProductPreset.Vault).copy(
                        incentiveRate = EarnProductRate(BigDecimal("0.02"), EarnRateType.APY),
                    ),
                )
                val model = createModel(
                    frames = MutableStateFlow(frame(summary(lending), summary(failing))),
                    products = listOf(lending, failing),
                    preselected = failing.id,
                )
                backgroundScope.launch { model.incentiveSheetRequests.collect { requests += it } }
                runCurrent()
                advanceUntilIdle()
                requireNotNull(model.uiState.value.ready().incentive).onClick()
                runCurrent()

                // Act
                val actual = requests.single().history(fakeApyPeriods.first())

                // Assert
                assertThat(actual.isLeft()).isTrue()
                model.onDestroy()
            }

        @Test
        fun `GIVEN a request emitted without collectors WHEN a collector subscribes later THEN it receives nothing`() =
            runTest {
                // Arrange
                val requests = mutableListOf<EarnIncentiveSheetRequest>()
                val model = createModel(
                    frames = MutableStateFlow(frame(summary(lending), summary(vault))),
                    preselected = lending.id,
                )
                advanceUntilIdle()
                requireNotNull(model.uiState.value.ready().incentive).onClick()
                runCurrent()

                // Act
                backgroundScope.launch { model.incentiveSheetRequests.collect { requests += it } }
                runCurrent()

                // Assert
                assertThat(requests).isEmpty()
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Analytics {

        @Test
        fun `GIVEN Auto with an active product WHEN frames THEN no Yield Picker Screen Opened`() = runTest {
            // Arrange
            val active = EarnPositionSummary(cryptoAmount = BigDecimal.ONE)
            val frames = MutableStateFlow(
                frame(summary(lending, position = active), summary(vault), source = StatusSource.CACHE),
            )
            val model = createModel(frames = frames, shouldRedirectToActive = true)

            // Act
            advanceUntilIdle()
            frames.value = frame(summary(lending, position = active), summary(vault), source = StatusSource.ACTUAL)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.navigate(any(), any()) }
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnPickerAnalyticsEvent.ScreenOpened>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN two products WHEN first visible frame and two more frames THEN one Yield Picker Screen Opened`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(frame(summary(lending), summary(vault)))
                val model = createModel(frames = frames)
                advanceUntilIdle()

                // Act
                frames.value = frame(summary(lending), summary(vault), source = StatusSource.ONLY_CACHE)
                advanceUntilIdle()
                frames.value = frame(summary(lending, rate = "0.06"), summary(vault), source = StatusSource.ACTUAL)
                advanceUntilIdle()

                // Assert
                val expected = EarnPickerAnalyticsEvent.ScreenOpened(
                    source = EarnEntrySource.TokenPage,
                    token = currency.symbol,
                    product = "Lending",
                    currentApy = "5.00",
                    products = 2,
                )
                verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnPickerAnalyticsEvent.ScreenOpened>()) }
                verify(exactly = 1) { analyticsEventHandler.send(expected) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN picker opened from the yield sheet WHEN first visible frame arrives THEN Screen Opened has that source`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(frame(summary(lending), summary(vault)))
                val model = createModel(frames = frames, source = EarnEntrySource.YieldSheet)

                // Act
                advanceUntilIdle()

                // Assert
                val expected = EarnPickerAnalyticsEvent.ScreenOpened(
                    source = EarnEntrySource.YieldSheet,
                    token = currency.symbol,
                    product = "Lending",
                    currentApy = "5.00",
                    products = 2,
                )
                verify(exactly = 1) { analyticsEventHandler.send(expected) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN preselected and a CACHE frame with one summary WHEN the ACTUAL frame adds a second THEN one event with Products 1`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(frame(summary(lending), source = StatusSource.CACHE))
                val model = createModel(frames = frames, preselected = lending.id)
                advanceUntilIdle()

                // Act
                frames.value = frame(summary(lending), summary(vault), source = StatusSource.ACTUAL)
                advanceUntilIdle()

                // Assert
                val expected = EarnPickerAnalyticsEvent.ScreenOpened(
                    source = EarnEntrySource.TokenPage,
                    token = currency.symbol,
                    product = "Lending",
                    currentApy = "5.00",
                    products = 1,
                )
                verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnPickerAnalyticsEvent.ScreenOpened>()) }
                verify(exactly = 1) { analyticsEventHandler.send(expected) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN chosen product rate null WHEN shown THEN no Current APY`() = runTest {
            // Arrange
            val lendingWithoutRate = createSummary(productId = lending.id, rate = null)
            val frames = MutableStateFlow(frame(lendingWithoutRate, summary(vault)))
            val model = createModel(frames = frames, preselected = lending.id)

            // Act
            advanceUntilIdle()

            // Assert
            val expected = EarnPickerAnalyticsEvent.ScreenOpened(
                source = EarnEntrySource.TokenPage,
                token = currency.symbol,
                product = "Lending",
                currentApy = null,
                products = 2,
            )
            verify(exactly = 1) { analyticsEventHandler.send(expected) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN Lending chosen WHEN clicking Vault then Lending THEN ProductChip Position 2 then 1 and no second screen event`() =
            runTest {
                // Arrange
                val frames = MutableStateFlow(frame(summary(lending), summary(vault, rate = "0.07")))
                val model = createModel(frames = frames, preselected = lending.id)
                advanceUntilIdle()

                // Act
                model.uiState.value.card("vault").onClick()
                advanceUntilIdle()
                model.uiState.value.card("lending").onClick()
                advanceUntilIdle()

                // Assert
                verifyOrder {
                    analyticsEventHandler.send(EarnPickerAnalyticsEvent.ProductChip("Vault", 2, "7.00"))
                    analyticsEventHandler.send(EarnPickerAnalyticsEvent.ProductChip("Lending", 1, "5.00"))
                }
                verify(exactly = 2) { analyticsEventHandler.send(ofType<EarnPickerAnalyticsEvent.ProductChip>()) }
                verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnPickerAnalyticsEvent.ScreenOpened>()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN Lending chosen WHEN clicking Lending THEN no ProductChip`() = runTest {
            // Arrange
            val frames = MutableStateFlow(frame(summary(lending), summary(vault)))
            val model = createModel(frames = frames, preselected = lending.id)
            advanceUntilIdle()

            // Act
            model.uiState.value.card("lending").onClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.selectedChipIds()).containsExactly("lending")
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnPickerAnalyticsEvent.ProductChip>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN shown WHEN onTabClick Details THEN Tab Lending Details`() = runTest {
            // Arrange
            val model = createModel(
                frames = MutableStateFlow(frame(summary(lending), summary(vault))),
                preselected = lending.id,
            )
            advanceUntilIdle()

            // Act
            model.uiState.value.ready().onTabClick(EarnPickerTab.Details)

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(EarnPickerAnalyticsEvent.Tab("Lending", EarnPickerTab.Details))
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN shown WHEN onHorizonChange Year5 and onInputChange MonthlyTopUp THEN CalculatorHorizon and CalculatorInput with Lending`() =
            runTest {
                // Arrange
                val model = createModel(
                    frames = MutableStateFlow(frame(summary(lending), summary(vault))),
                    preselected = lending.id,
                )
                advanceUntilIdle()

                // Act
                model.onHorizonChange(EarnHorizon.Year5)
                model.onInputChange(EarnCalculatorComponent.Input.MonthlyTopUp)

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(EarnPickerAnalyticsEvent.CalculatorHorizon("Lending", EarnHorizon.Year5))
                }
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        EarnPickerAnalyticsEvent.CalculatorInput("Lending", EarnCalculatorComponent.Input.MonthlyTopUp),
                    )
                }
                model.onDestroy()
            }

        @Test
        fun `GIVEN shown WHEN onInfoClick Risk and onLinkClick url null THEN DetailRow Lending Risk and DetailRow Lending null`() =
            runTest {
                // Arrange
                val info = EarnRowInfo.Text(title = stringReference("title"), text = stringReference("description"))
                val model = createModel(
                    frames = MutableStateFlow(frame(summary(lending), summary(vault))),
                    preselected = lending.id,
                )
                advanceUntilIdle()

                // Act
                model.onInfoClick(info, EarnRowKind.Risk)
                model.onLinkClick("https://aave.com", null)

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(EarnPickerAnalyticsEvent.DetailRow("Lending", EarnRowKind.Risk))
                }
                verify(exactly = 1) { analyticsEventHandler.send(EarnPickerAnalyticsEvent.DetailRow("Lending", null)) }
                verify(exactly = 1) { urlOpener.openUrl("https://aave.com") }
                assertThat(model.rowInfo.value).isEqualTo(
                    EarnRowInfoUM.Text(title = stringReference("title"), text = stringReference("description")),
                )
                model.onDestroy()
            }

        @Test
        fun `GIVEN no position and balance below min deposit WHEN shown THEN NoFundsNotice and after onAddFundsClick AddFunds symbol YieldPicker, no StartEarning`() =
            runTest {
                // Arrange
                stubBalance(BigDecimal.ZERO)
                val model = createModel(
                    frames = MutableStateFlow(frame(summary(lending), summary(vault))),
                    preselected = lending.id,
                )
                advanceUntilIdle()

                // Act
                model.uiState.value.cta.onClick()

                // Assert
                verifyOrder {
                    analyticsEventHandler.send(EarnPickerAnalyticsEvent.NoFundsNotice(currency.symbol, "Lending"))
                    analyticsEventHandler.send(
                        EarnPickerAnalyticsEvent.AddFunds(currency.symbol, EarnAddFundsSource.YieldPicker),
                    )
                }
                verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnPickerAnalyticsEvent.StartEarning>()) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN the no funds notice shown WHEN the state is re-emitted THEN still one NoFundsNotice`() = runTest {
            // Arrange
            stubBalance(BigDecimal.ZERO)
            val model = createModel(
                frames = MutableStateFlow(frame(summary(lending), summary(vault))),
                preselected = lending.id,
            )
            advanceUntilIdle()

            // Act
            lending.stateFlow.value = lending.readyState().copy(description = stringReference("changed"))
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnPickerAnalyticsEvent.NoFundsNotice>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN Primary CTA WHEN onActionClick lending deposit THEN StartEarning Lending with the rate`() = runTest {
            // Arrange
            val model = createModel(
                frames = MutableStateFlow(frame(summary(lending), summary(vault))),
                preselected = lending.id,
            )
            advanceUntilIdle()
            val action = lending.readyState().primary.action

            // Act
            model.onActionClick(lending, action)

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(EarnPickerAnalyticsEvent.StartEarning("Lending", "5.00")) }
            assertThat(lending.handler.actions).containsExactly(action)
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class HeroVideo {

        @Test
        fun `GIVEN a picker WHEN the model is created THEN the hero clip is requested from the earn cache at its own size`() =
            runTest {
                // Act
                val model = createModel(frames = MutableStateFlow(frame()))

                // Assert
                verify(exactly = 1) {
                    videoFactory.createRemote(
                        scope = any(),
                        url = EarnPickerHeroVideo.URL,
                        cache = heroClipCache,
                        frameSize = VideoFrameSize(width = 720, height = 1280),
                        diagnosticsSubject = "Earn picker hero video",
                        onFailed = any(),
                    )
                }
                model.onDestroy()
            }

        @Test
        fun `GIVEN the hero clip WHEN the state is read THEN it is the controller state`() = runTest {
            // Arrange
            val state = MutableStateFlow(LOOPING_STATE)
            every { heroVideo.state } returns state
            val model = createModel(frames = MutableStateFlow(frame()))

            // Act
            val actual = model.heroVideoState

            // Assert
            assertThat(actual).isSameInstanceAs(state)
            model.onDestroy()
        }

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `GIVEN a model with the hero in view WHEN the screen resumed state changes THEN the clip follows it`(
            isResumed: Boolean,
        ) = runTest {
            // Arrange
            val model = createModel(frames = MutableStateFlow(frame()))

            // Act
            model.setScreenResumed(isResumed = isResumed)

            // Assert
            verify(exactly = 1) { heroVideo.setRunning(isRunning = isResumed) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the screen resumed WHEN the hero scrolls out of view THEN the clip pauses`() = runTest {
            // Arrange
            val model = createModel(frames = MutableStateFlow(frame()))
            model.setScreenResumed(isResumed = true)

            // Act
            model.setHeroVisible(isVisible = false)

            // Assert
            verifySequence {
                heroVideo.setRunning(isRunning = true)
                heroVideo.setRunning(isRunning = false)
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the hero out of view WHEN the screen resumes THEN the clip stays paused`() = runTest {
            // Arrange
            val model = createModel(frames = MutableStateFlow(frame()))
            model.setHeroVisible(isVisible = false)

            // Act
            model.setScreenResumed(isResumed = true)

            // Assert
            verify(exactly = 0) { heroVideo.setRunning(isRunning = true) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN the clip paused by the scroll WHEN the hero comes back into view THEN the clip runs again`() =
            runTest {
                // Arrange
                val model = createModel(frames = MutableStateFlow(frame()))
                model.setScreenResumed(isResumed = true)
                model.setHeroVisible(isVisible = false)

                // Act
                model.setHeroVisible(isVisible = true)

                // Assert
                verifySequence {
                    heroVideo.setRunning(isRunning = true)
                    heroVideo.setRunning(isRunning = false)
                    heroVideo.setRunning(isRunning = true)
                }
                model.onDestroy()
            }

        @Test
        fun `GIVEN the hero in view WHEN the screen pauses THEN the clip pauses`() = runTest {
            // Arrange
            val model = createModel(frames = MutableStateFlow(frame()))
            model.setScreenResumed(isResumed = true)

            // Act
            model.setScreenResumed(isResumed = false)

            // Assert
            verifySequence {
                heroVideo.setRunning(isRunning = true)
                heroVideo.setRunning(isRunning = false)
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN a model destroyed in the frame it was created WHEN destroyed THEN the clip is released once`() =
            runTest {
                // Arrange
                val model = createModel(frames = MutableStateFlow(frame()))

                // Act
                model.onDestroy()

                // Assert
                verify(exactly = 1) { heroVideo.release() }
            }
    }

    private fun TestScope.createModel(
        frames: Flow<EarnProductSummaries>,
        products: List<EarnProduct> = listOf(lending, vault),
        preselected: EarnProductId? = null,
        shouldRedirectToActive: Boolean = false,
        source: EarnEntrySource = EarnEntrySource.TokenPage,
    ): EarnPickerModel {
        every { summarySupplier(USER_WALLET_ID) } returns frames
        val registry = EarnProductsRegistry(products = products.toSet(), rateSources = emptySet())
        val cta = EarnPickerCta()

        return EarnPickerModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                EarnYieldPickerComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    cryptoCurrency = currency,
                    source = source,
                    preselected = preselected,
                    shouldRedirectToActive = shouldRedirectToActive,
                ),
            ),
            registry = registry,
            summarySupplier = summarySupplier,
            headlineSelector = EarnHeadlineSelector(
                order = EarnSummaryOrder(registry),
                rateBadgeConverter = EarnRateBadgeConverter(),
                registry = registry,
            ),
            converter = EarnPickerConverter(registry = registry, cta = cta),
            singleAccountStatusListSupplier = accountStatusListSupplier,
            overviewFetcher = overviewFetcher,
            appScope = TestAppCoroutineScope(StandardTestDispatcher(testScheduler)),
            router = router,
            uiMessageSender = uiMessageSender,
            urlOpener = urlOpener,
            videoFactory = videoFactory,
            heroClipCache = heroClipCache,
            cta = cta,
            analyticsEventHandler = analyticsEventHandler,
        )
    }

    private fun frame(
        vararg summaries: EarnProductSummary,
        source: StatusSource = StatusSource.ACTUAL,
        isAwaitingProducts: Boolean = source == StatusSource.CACHE,
        pendingCurrencies: Set<CryptoCurrency.ID> = emptySet(),
    ): EarnProductSummaries = EarnProductSummaries(
        products = mapOf(currency.id to summaries.toList()),
        rateSources = emptyMap(),
        source = source,
        isAwaitingProducts = isAwaitingProducts,
        pendingCurrencies = pendingCurrencies,
    )

    private fun summary(
        product: EarnProduct,
        position: EarnPositionSummary? = null,
        rate: String = "0.05",
    ): EarnProductSummary = createSummary(productId = product.id, rate = createRate(rate), position = position)

    private fun readyState(preset: FakeEarnProductPreset): EarnProductState.Ready =
        fakeReadyState(preset, FakeEarnPosition.None)

    private fun stubBalance(balance: BigDecimal?) {
        val status = balance?.let {
            val value: CryptoCurrencyStatus.Value = mockk()
            every { value.amount } returns it
            val mockedStatus: CryptoCurrencyStatus = mockk()
            every { mockedStatus.value } returns value
            mockedStatus
        }
        every {
            with(CryptoCurrencyStatusOperations) {
                accountStatusList.getCryptoCurrencyStatus(any<CryptoCurrency>())
            }
        } returns (status?.some() ?: none())
    }

    private fun EarnPickerUM.card(id: String): EarnProductCardUM = products.first { it.id == id }

    private fun EarnPickerUM.selectedChipIds(): List<String> = products.filter { it.isSelected }.map { it.id }

    private fun EarnPickerUM.ready(): ContentUM.Ready = content as ContentUM.Ready

    internal data class PreselectedModel(val preselected: EarnProductId?)

    /** A fake product with its state under the test's control, counting how often its action handler is created. */
    internal class PickerProduct(
        private val delegate: EarnProduct,
        initialState: EarnProductState,
        private val refreshResult: Either<Throwable, Unit> = Unit.right(),
        private val customState: Flow<EarnProductState>? = null,
        private val stateFactory: (() -> Flow<EarnProductState>)? = null,
    ) : EarnProduct by delegate {

        val stateFlow = MutableStateFlow(initialState)

        val handler = RecordingHandler()

        var actionHandlerCalls: Int = 0
            private set

        val refreshParams = mutableListOf<EarnProduct.Params>()

        var refreshGate: CompletableDeferred<Unit>? = null

        var refreshesFinished: Int = 0

        fun readyState(): EarnProductState.Ready = stateFlow.value as EarnProductState.Ready

        override fun state(params: EarnProduct.Params): Flow<EarnProductState> = stateFactory?.invoke() ?: customState ?: stateFlow

        override suspend fun refresh(params: EarnProduct.Params): Either<Throwable, Unit> {
            refreshParams += params
            refreshGate?.await()
            refreshesFinished++
            return refreshResult
        }

        override fun actionHandler(params: EarnProduct.Params, navigator: EarnNavigator): EarnActionHandler {
            actionHandlerCalls++
            return handler
        }
    }

    internal class RecordingHandler : EarnActionHandler {

        val actions = mutableListOf<EarnAction>()

        override fun onActionClick(action: EarnAction) {
            actions += action
        }
    }

    private companion object {

        val OTHER_CURRENCY_ID = CryptoCurrency.ID.fromValue("coin⟨BNB⟩binancecoin")

        val LOOPING_STATE = LoopingVideoState(
            surface = VideoSurfaceState(
                frameSize = VideoFrameSize(width = 720, height = 1280),
                key = 0,
                isFirstFrameShown = false,
                onAvailable = {},
                onRelease = {},
            ),
            isRunning = false,
            isMotionEnabled = true,
            isFailed = false,
        )
    }

    private object FlowOnTop : Route
}
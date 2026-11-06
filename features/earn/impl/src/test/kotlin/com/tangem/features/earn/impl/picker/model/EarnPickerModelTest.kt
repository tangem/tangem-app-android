package com.tangem.features.earn.impl.picker.model

import arrow.core.Either
import arrow.core.none
import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.ModalMessage
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.overview.EarnOverviewFetcher
import com.tangem.features.earn.impl.R
import com.tangem.features.earn.impl.fake.FakeEarnPosition
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.createFakeEarnProduct
import com.tangem.features.earn.impl.fake.fakeReadyState
import com.tangem.features.earn.impl.overview.EarnHeadlineSelector
import com.tangem.features.earn.impl.overview.EarnProductSummaries
import com.tangem.features.earn.impl.overview.EarnProductSummarySupplier
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.overview.EarnRateBadgeConverter
import com.tangem.features.earn.impl.overview.EarnSummaryOrder
import com.tangem.features.earn.impl.overview.USER_WALLET_ID
import com.tangem.features.earn.impl.overview.createRate
import com.tangem.features.earn.impl.overview.createSummary
import com.tangem.features.earn.impl.picker.EarnPickerTexts
import com.tangem.features.earn.impl.picker.EarnYieldPickerComponent
import com.tangem.features.earn.impl.picker.ui.state.EarnProductCardUM
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerUM
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerUM.ContentUM
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.navigation.EarnNavigator
import com.tangem.features.earn.product.api.product.EarnActionHandler
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnFreshness
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnProjection
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.test.core.ProvideTestModels
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnPickerModelTest {

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
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
        clearMocks(router, uiMessageSender, urlOpener, overviewFetcher, summarySupplier, accountStatusListSupplier, accountStatusList)
        mockkObject(CryptoCurrencyStatusOperations)
        mockkObject(TangemLogger)
        lending = PickerProduct(createFakeEarnProduct(FakeEarnProductPreset.Lending), readyState(FakeEarnProductPreset.Lending))
        staking = PickerProduct(createFakeEarnProduct(FakeEarnProductPreset.Staking), readyState(FakeEarnProductPreset.Staking))
        vault = PickerProduct(createFakeEarnProduct(FakeEarnProductPreset.Vault), readyState(FakeEarnProductPreset.Vault))
        every { accountStatusListSupplier(USER_WALLET_ID) } returns flowOf(accountStatusList)
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
        fun `GIVEN ETH balance below the minimum WHEN model created THEN Add funds with the hint`() = runTest {
            // Arrange
            stubBalance(BigDecimal.ZERO)
            val model = createModel(frames = MutableStateFlow(frame(summary(lending), summary(vault))), preselected = lending.id)

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.cta.text).isEqualTo(EarnPickerTexts.addFunds)
            assertThat(model.uiState.value.ctaHint).isNotNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN Add funds WHEN cta clicked THEN neither the router nor a handler is touched`() = runTest {
            // Arrange
            stubBalance(BigDecimal.ZERO)
            val model = createModel(frames = MutableStateFlow(frame(summary(lending), summary(vault))), preselected = lending.id)
            advanceUntilIdle()

            // Act
            model.uiState.value.cta.onClick()

            // Assert
            verify { router wasNot Called }
            assertThat(lending.actionHandlerCalls).isEqualTo(0)
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
            val route = slot<Route>()
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
            verify(exactly = 1) { router.replaceCurrent(capture(route), any()) }
            assertThat(route.captured).isEqualTo(EarnInfoRoute(staking.id))
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
            verify(exactly = 1) { router.replaceCurrent(any(), any()) }
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
        fun `GIVEN no active product and ACTUAL WHEN Auto THEN the picker stays with its content`() = runTest {
            // Arrange
            val frames = MutableStateFlow(frame(summary(lending), summary(vault)))
            val model = createModel(frames = frames, shouldRedirectToActive = true)

            // Act
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { router.replaceCurrent(any(), any()) }
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
            verify(exactly = 0) { router.replaceCurrent(any(), any()) }
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
            verify(exactly = 0) { router.replaceCurrent(any(), any()) }
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
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Misc {

        @Test
        fun `GIVEN a row with info WHEN its onInfoClick THEN a modal message is sent`() = runTest {
            // Arrange
            val info = EarnTextBlock(stringReference("title"), stringReference("description"))
            val row = EarnRow(stringReference("label"), EarnValue.Text(stringReference("value")), info)
            lending.stateFlow.value = lending.readyState().copy(details = listOf(row))
            val sent = slot<ModalMessage>()
            every { uiMessageSender.send(capture(sent)) } returns Unit
            val model = createModel(frames = MutableStateFlow(frame(summary(lending), summary(vault))), preselected = lending.id)
            advanceUntilIdle()

            // Act
            model.uiState.value.ready().details.single().onInfoClick?.invoke()

            // Assert
            verify(exactly = 1) { uiMessageSender.send(any()) }
            assertThat(sent.isCaptured).isTrue()
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

    private fun TestScope.createModel(
        frames: Flow<EarnProductSummaries>,
        products: List<EarnProduct> = listOf(lending, vault),
        preselected: EarnProductId? = null,
        shouldRedirectToActive: Boolean = false,
    ): EarnPickerModel {
        every { summarySupplier(USER_WALLET_ID) } returns frames
        val registry = EarnProductsRegistry(products = products.toSet(), rateSources = emptySet())

        return EarnPickerModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                EarnYieldPickerComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    cryptoCurrency = currency,
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
            converter = EarnPickerConverter(registry = registry, cta = EarnPickerCta()),
            singleAccountStatusListSupplier = accountStatusListSupplier,
            overviewFetcher = overviewFetcher,
            router = router,
            uiMessageSender = uiMessageSender,
            urlOpener = urlOpener,
        )
    }

    private fun frame(
        vararg summaries: EarnProductSummary,
        source: StatusSource = StatusSource.ACTUAL,
    ): EarnProductSummaries = EarnProductSummaries(
        products = mapOf(currency.id to summaries.toList()),
        rateSources = emptyMap(),
        source = source,
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
    ) : EarnProduct by delegate {

        val stateFlow = MutableStateFlow(initialState)

        val handler = RecordingHandler()

        var actionHandlerCalls: Int = 0
            private set

        val refreshParams = mutableListOf<EarnProduct.Params>()

        fun readyState(): EarnProductState.Ready = stateFlow.value as EarnProductState.Ready

        override fun state(params: EarnProduct.Params): Flow<EarnProductState> = customState ?: stateFlow

        override suspend fun refresh(params: EarnProduct.Params): Either<Throwable, Unit> {
            refreshParams += params
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
}
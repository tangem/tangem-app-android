package com.tangem.features.earn.impl.info.model

import arrow.core.Either
import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.ui.EarnHowItWorksUM
import com.tangem.features.earn.api.ui.EarnRowInfoUM
import com.tangem.features.earn.impl.R
import com.tangem.features.earn.impl.analytics.EarnAddFundsSource
import com.tangem.features.earn.impl.analytics.EarnInfoAnalyticsEvent
import com.tangem.features.earn.impl.analytics.EarnPickerAnalyticsEvent
import com.tangem.features.earn.impl.analytics.EarnUsdValuation
import com.tangem.features.earn.impl.analytics.InfoAction
import com.tangem.features.earn.impl.fake.FakeEarnPosition
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.createFakeEarnProduct
import com.tangem.features.earn.impl.fake.fakeReadyState
import com.tangem.features.earn.impl.info.EarnInfoActions
import com.tangem.features.earn.impl.info.EarnInfoChartConfig
import com.tangem.features.earn.impl.info.EarnInfoComponent
import com.tangem.features.earn.impl.info.ui.state.EarnInfoUM
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.overview.USER_WALLET_ID
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorComponent
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorSheetConfig
import com.tangem.features.earn.product.api.navigation.EarnNavigator
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.features.earn.product.api.product.EarnActionHandler
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnFreshness
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnRowInfo
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.core.getEmittedValues
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnInfoModelTest {

    private val router: Router = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val blogUrlBuilder: TangemBlogUrlBuilder = mockk()
    private val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val accountStatusList: AccountStatusList = mockk()
    private val getSelectedAppCurrency: GetSelectedAppCurrencyUseCase = mockk()
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val isBalanceHidden = MutableStateFlow(false)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val usdValuation: EarnUsdValuation = mockk()
    private val ethereum: CryptoCurrency = MockCryptoCurrencyFactory().ethereum
    private val productParams = EarnProduct.Params(USER_WALLET_ID, ethereum)
    private val converter = EarnInfoConverter(actions = EarnInfoActions())

    private lateinit var product: InfoProduct

    @BeforeEach
    fun setUp() {
        clearMocks(
            router,
            uiMessageSender,
            urlOpener,
            blogUrlBuilder,
            accountStatusListSupplier,
            accountStatusList,
            getSelectedAppCurrency,
            singleQuoteStatusFetcher,
            getBalanceHidingSettingsUseCase,
            analyticsEventHandler,
            usdValuation,
        )
        mockkObject(CryptoCurrencyStatusOperations)
        mockkObject(TangemLogger)
        product = InfoProduct(createFakeEarnProduct(FakeEarnProductPreset.Lending), knownState())
        isBalanceHidden.value = false
        every { accountStatusListSupplier(USER_WALLET_ID) } returns flowOf(accountStatusList)
        every { getSelectedAppCurrency.invokeOrDefault() } returns flowOf(AppCurrency.Default)
        coEvery { singleQuoteStatusFetcher(any()) } returns Unit.right()
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns isBalanceHidden
        stubStatus(status())
        coEvery { usdValuation.bucket(any(), any()) } returns null
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
        unmockkObject(TangemLogger)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UiState {

        @Test
        fun `GIVEN Ready with Known position WHEN model created THEN uiState holds the converted UM`() = runTest {
            // Arrange
            val model = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            val expected = converter.convert(
                product = product,
                status = status(),
                appCurrency = AppCurrency.Default,
                isBalanceHidden = false,
                ready = product.readyState(),
                callbacks = NoOpCallbacks,
            )
            assertThat(expected).isNotNull()
            assertThat(model.uiState.value?.withoutCallbacks()).isEqualTo(expected?.withoutCallbacks())
            model.onDestroy()
        }

        @Test
        fun `GIVEN hidden balance WHEN model created THEN uiState masks the balance`() = runTest {
            // Arrange
            isBalanceHidden.value = true
            val model = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            val actual = requireNotNull(model.uiState.value)
            assertThat(listOf(actual.balanceFiat, actual.balanceCrypto))
                .containsExactly(stringReference("***"), stringReference("***"))
            model.onDestroy()
        }

        @Test
        fun `GIVEN shown position WHEN balance hiding is switched THEN the balance is masked and shown again`() =
            runTest {
                // Arrange
                val model = createModel()
                advanceUntilIdle()
                val shown = requireNotNull(model.uiState.value).balanceFiat

                // Act
                isBalanceHidden.value = true
                advanceUntilIdle()
                val masked = requireNotNull(model.uiState.value).balanceFiat
                isBalanceHidden.value = false
                advanceUntilIdle()
                val shownAgain = requireNotNull(model.uiState.value).balanceFiat

                // Assert
                assertThat(shown).isNotEqualTo(stringReference("***"))
                assertThat(masked).isEqualTo(stringReference("***"))
                assertThat(shownAgain).isEqualTo(shown)
                model.onDestroy()
            }

        @Test
        fun `GIVEN shown position WHEN the product turns Loading and balance hiding is switched THEN the balance follows`() =
            runTest {
                // Arrange
                val model = createModel()
                advanceUntilIdle()
                val shown = requireNotNull(model.uiState.value).balanceFiat
                product.stateFlow.value = EarnProductState.Loading
                advanceUntilIdle()

                // Act
                isBalanceHidden.value = true
                advanceUntilIdle()
                val masked = requireNotNull(model.uiState.value).balanceFiat
                isBalanceHidden.value = false
                advanceUntilIdle()
                val shownAgain = requireNotNull(model.uiState.value).balanceFiat

                // Assert
                assertThat(masked).isEqualTo(stringReference("***"))
                assertThat(shownAgain).isEqualTo(shown)
                model.onDestroy()
            }

        @Test
        fun `GIVEN shown position WHEN the currency status disappears and balance hiding is switched THEN the balance follows`() =
            runTest {
                // Arrange
                val accountStatusLists = MutableSharedFlow<AccountStatusList>(replay = 1)
                every { accountStatusListSupplier(USER_WALLET_ID) } returns accountStatusLists
                accountStatusLists.emit(accountStatusList)
                val model = createModel()
                advanceUntilIdle()
                val shown = requireNotNull(model.uiState.value).balanceFiat
                stubStatus(null)
                accountStatusLists.emit(accountStatusList)
                advanceUntilIdle()

                // Act
                isBalanceHidden.value = true
                advanceUntilIdle()
                val masked = requireNotNull(model.uiState.value).balanceFiat
                isBalanceHidden.value = false
                advanceUntilIdle()
                val shownAgain = requireNotNull(model.uiState.value).balanceFiat

                // Assert
                assertThat(masked).isEqualTo(stringReference("***"))
                assertThat(shownAgain).isEqualTo(shown)
                model.onDestroy()
            }

        @Test
        fun `GIVEN shown position WHEN the product turns Loading and hiding is unchanged THEN uiState is not rebuilt`() =
            runTest {
                // Arrange
                val model = createModel()
                advanceUntilIdle()
                val shown = model.uiState.value

                // Act
                product.stateFlow.value = EarnProductState.Loading
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value).isSameInstanceAs(shown)
                model.onDestroy()
            }

        @Test
        fun `GIVEN Loading and no status WHEN model created THEN uiState is null and router is untouched`() = runTest {
            // Arrange
            product.stateFlow.value = EarnProductState.Loading
            stubStatus(null)
            val model = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isNull()
            verify { router wasNot Called }
            model.onDestroy()
        }

        @Test
        fun `GIVEN Loading and a status WHEN model created THEN uiState is null and router is untouched`() = runTest {
            // Arrange
            product.stateFlow.value = EarnProductState.Loading
            val model = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isNull()
            verify { router wasNot Called }
            model.onDestroy()
        }

        @Test
        fun `GIVEN Loading then Ready WHEN state arrives THEN uiState appears and the router was never touched`() =
            runTest {
                // Arrange
                product.stateFlow.value = EarnProductState.Loading
                val model = createModel()
                advanceUntilIdle()
                val whileLoading = model.uiState.value

                // Act
                product.stateFlow.value = knownState()
                advanceUntilIdle()

                // Assert
                assertThat(whileLoading).isNull()
                assertThat(model.uiState.value).isNotNull()
                verify { router wasNot Called }
                model.onDestroy()
            }

        @Test
        fun `GIVEN Ready but no status for the currency WHEN model created THEN uiState is null and no redirect`() =
            runTest {
                // Arrange
                stubStatus(null)
                val model = createModel()

                // Act
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value).isNull()
                verify { router wasNot Called }
                model.onDestroy()
            }

        @Test
        fun `GIVEN close click WHEN invoked THEN router pops once`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            requireNotNull(model.uiState.value).onClose()

            // Assert
            verify(exactly = 1) { router.pop(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN Ready with projection and incentive rate WHEN flows collected THEN they emit the values of the state`() =
            runTest {
                // Arrange
                val model = createModel()
                val projections = getEmittedValues(model.projection)
                val incentiveRates = getEmittedValues(model.incentiveRate)

                // Act
                advanceUntilIdle()

                // Assert
                assertThat(projections.last()).isEqualTo(product.readyState().projection)
                assertThat(incentiveRates.last()).isEqualTo(product.readyState().incentiveRate)
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Redirect {

        @Test
        fun `GIVEN Unavailable WHEN model created THEN the router replaces the screen with the picker once`() =
            runTest {
                // Arrange
                product.stateFlow.value = EarnProductState.Unavailable
                val model = createModel()

                // Act
                advanceUntilIdle()

                // Assert
                verifyRedirectedOnce()
                assertThat(model.uiState.value).isNull()
                model.onDestroy()
            }

        @Test
        fun `GIVEN Unavailable and no status for the currency WHEN model created THEN the router replaces the screen with the picker once`() =
            runTest {
                // Arrange
                product.stateFlow.value = EarnProductState.Unavailable
                stubStatus(null)
                val model = createModel()

                // Act
                advanceUntilIdle()

                // Assert
                verifyRedirectedOnce()
                assertThat(model.uiState.value).isNull()
                model.onDestroy()
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN Ready with position None or Unknown WHEN model created THEN the router replaces the screen with the picker once`(
            model: EarnPositionState,
        ) = runTest {
            // Arrange
            product.stateFlow.value = product.readyState().copy(position = model)
            val infoModel = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            verifyRedirectedOnce()
            assertThat(infoModel.uiState.value).isNull()
            infoModel.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN Ready with position None or Unknown and no status for the currency WHEN model created THEN the router replaces the screen with the picker once`(
            model: EarnPositionState,
        ) = runTest {
            // Arrange
            product.stateFlow.value = product.readyState().copy(position = model)
            stubStatus(null)
            val infoModel = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            verifyRedirectedOnce()
            assertThat(infoModel.uiState.value).isNull()
            infoModel.onDestroy()
        }

        private fun provideTestModels() = listOf(EarnPositionState.None, EarnPositionState.Unknown)

        @Test
        fun `GIVEN Known then Unknown WHEN state changes THEN exactly one redirect`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()
            verify(exactly = 0) { router.navigate(any(), any()) }

            // Act
            product.stateFlow.value = product.readyState().copy(position = EarnPositionState.Unknown)
            advanceUntilIdle()
            product.stateFlow.value = product.readyState().copy(description = stringReference("changed"))
            advanceUntilIdle()

            // Assert
            verifyRedirectedOnce()
            model.onDestroy()
        }

        @Test
        fun `GIVEN a state that throws WHEN model created THEN the error is logged and the screen is replaced once`() =
            runTest {
                // Arrange
                val failing = InfoProduct(
                    delegate = createFakeEarnProduct(FakeEarnProductPreset.Lending),
                    initialState = product.readyState(),
                    customState = flow { throw IllegalStateException("offline") },
                )
                val model = createModel(products = listOf(failing))

                // Act
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
                verifyRedirectedOnce()
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class UnknownProduct {

        @Test
        fun `GIVEN unknown productId WHEN model created THEN TangemLogger e is called once and router pop once`() =
            runTest {
                // Arrange
                val model = createModel(products = emptyList(), productId = EarnProductId("unknown"))

                // Act
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
                verify(exactly = 1) { router.pop(any()) }
                verify(exactly = 0) { router.navigate(any(), any()) }
                assertThat(model.uiState.value).isNull()
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Actions {

        @Test
        fun `GIVEN clicks on primary, secondary and Claim WHEN invoked THEN the handler gets each action once and is created once`() =
            runTest {
                // Arrange
                val model = createModel()
                advanceUntilIdle()
                val ready = product.readyState()
                val claim = requireNotNull(ready.knownPosition().rewards).claimAction
                val state = requireNotNull(model.uiState.value)

                // Act
                state.primary.onClick()
                state.secondary.first().onClick()
                requireNotNull(state.rewards).claim.onClick()

                // Assert
                assertThat(product.handled)
                    .containsExactly(ready.primary.action, ready.secondary.first().action, claim)
                    .inOrder()
                assertThat(product.actionHandlerCalls).isEqualTo(1)
                model.onDestroy()
            }

        @Test
        fun `GIVEN stale Ready WHEN primary clicked THEN the out-of-date reason is sent and the handler is not called`() =
            runTest {
                // Arrange
                product.stateFlow.value = product.readyState().copy(
                    freshness = EarnFreshness(asOf = Instant.fromEpochSeconds(0), isStale = true),
                )
                val sent = slot<DialogMessage>()
                every { uiMessageSender.send(capture(sent)) } returns Unit
                val model = createModel()
                advanceUntilIdle()

                // Act
                requireNotNull(model.uiState.value).primary.onClick()

                // Assert
                val outOfDate = resourceReference(R.string.token_button_unavailability_reason_out_of_date_balance)
                assertThat(sent.captured.message).isEqualTo(outOfDate)
                assertThat(product.handled).isEmpty()
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Chart {

        @Test
        fun `GIVEN non-empty apyPeriods WHEN Ready arrives THEN chart slot is activated once even after further emissions`() =
            runTest {
                // Arrange
                val model = createModel()
                val chart = model.chartNavigation.track()

                // Act
                advanceUntilIdle()
                product.stateFlow.value = product.readyState().copy(description = stringReference("changed"))
                advanceUntilIdle()

                // Assert
                assertThat(model.uiState.value).isNotNull()
                assertThat(chart).containsExactly(EarnInfoChartConfig)
                model.onDestroy()
            }

        @Test
        fun `GIVEN empty apyPeriods WHEN Ready arrives THEN chart slot is never activated`() = runTest {
            // Arrange
            product.stateFlow.value = product.readyState().copy(apyPeriods = emptyList())
            val model = createModel()
            val chart = model.chartNavigation.track()

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isNotNull()
            assertThat(chart).isEmpty()
            model.onDestroy()
        }

        @Test
        fun `GIVEN Loading WHEN model created THEN isChartActivated is false`() = runTest {
            // Arrange
            product.stateFlow.value = EarnProductState.Loading
            val model = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.isChartActivated).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN Loading then Ready with non-empty apyPeriods WHEN state arrives THEN isChartActivated becomes true`() =
            runTest {
                // Arrange
                product.stateFlow.value = EarnProductState.Loading
                val model = createModel()
                advanceUntilIdle()
                val beforeReady = model.isChartActivated

                // Act
                product.stateFlow.value = knownState()
                advanceUntilIdle()

                // Assert
                assertThat(beforeReady).isFalse()
                assertThat(model.isChartActivated).isTrue()
                model.onDestroy()
            }

        @Test
        fun `GIVEN empty apyPeriods WHEN Ready arrives THEN isChartActivated stays false`() = runTest {
            // Arrange
            product.stateFlow.value = product.readyState().copy(apyPeriods = emptyList())
            val model = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value).isNotNull()
            assertThat(model.isChartActivated).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN chartParams WHEN history called THEN product apyHistory is invoked with productParams and the period`() =
            runTest {
                // Arrange
                val model = createModel()
                advanceUntilIdle()
                val ready = product.readyState()

                // Act
                val params = model.chartParams()
                val history = params.history(ready.apyPeriods.first())

                // Assert
                assertThat(product.apyHistoryCalls).containsExactly(productParams to ready.apyPeriods.first())
                assertThat(history).isInstanceOf(Either.Right::class.java)
                model.onDestroy()
            }

        @Test
        fun `GIVEN chartParams WHEN created THEN periods and rate come from Ready, defaultPeriod is the last, the calculator is shown and callback is the model`() =
            runTest {
                // Arrange
                val model = createModel()
                advanceUntilIdle()
                val ready = product.readyState()

                // Act
                val params = model.chartParams()

                // Assert
                assertThat(params.periods).isEqualTo(ready.apyPeriods)
                assertThat(params.defaultPeriod).isEqualTo(ready.apyPeriods.last())
                assertThat(params.currentRate.value).isEqualTo(ready.rate)
                assertThat(params.hasCalculator).isTrue()
                assertThat(params.callback).isSameInstanceAs(model)
                model.onDestroy()
            }

        @Test
        fun `GIVEN chartParams WHEN the product rate changes THEN the chart rate follows it`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()
            val params = model.chartParams()
            val changed = EarnProductRate(fraction = BigDecimal("0.0612"), type = product.readyState().rate.type)

            // Act
            product.stateFlow.value = product.readyState().copy(rate = changed)
            advanceUntilIdle()

            // Assert
            assertThat(params.currentRate.value).isEqualTo(changed)
            model.onDestroy()
        }

        @Test
        fun `GIVEN chartParams called twice WHEN created THEN both share one rate flow`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            val first = model.chartParams()
            val second = model.chartParams()

            // Assert
            assertThat(first.currentRate).isSameInstanceAs(second.currentRate)
            model.onDestroy()
        }

        @Test
        fun `GIVEN Ready without projection WHEN chartParams THEN no calculator but the model still gets period changes`() =
            runTest {
                // Arrange
                product.stateFlow.value = knownState().copy(projection = null)
                val model = createModel()
                advanceUntilIdle()

                // Act
                val params = model.chartParams()

                // Assert
                assertThat(params.hasCalculator).isFalse()
                assertThat(params.callback).isSameInstanceAs(model)
                model.onDestroy()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Calculator {

        @Test
        fun `GIVEN onCalculatorClick WHEN called THEN sheet slot is activated`() = runTest {
            // Arrange
            val model = createModel()
            val sheet = model.calculatorSheetNavigation.track()

            // Act
            model.onCalculatorClick()

            // Assert
            assertThat(sheet).containsExactly(EarnCalculatorSheetConfig)
            model.onDestroy()
        }

        @Test
        fun `GIVEN open sheet WHEN onDismiss called twice THEN the slot is dismissed`() = runTest {
            // Arrange
            val model = createModel()
            val sheet = model.calculatorSheetNavigation.track()
            model.onCalculatorClick()

            // Act
            model.onDismiss()
            model.onDismiss()

            // Assert
            assertThat(sheet.first()).isEqualTo(EarnCalculatorSheetConfig)
            assertThat(sheet.last()).isNull()
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Analytics {

        @Test
        fun `GIVEN Ready with a valued position and earned WHEN shown THEN Info Screen Opened with both buckets`() =
            runTest {
                // Arrange
                product.stateFlow.value = product.readyState().withPosition(
                    cryptoAmount = BigDecimal("1"),
                    earned = BigDecimal("0.01"),
                )
                coEvery { usdValuation.bucket(ethereum, BigDecimal("1")) } returns "1K-5K"
                coEvery { usdValuation.bucket(ethereum, BigDecimal("0.01")) } returns "<50"
                val model = createModel(source = EarnEntrySource.TokenPage)

                // Act
                advanceUntilIdle()

                // Assert
                val expected = EarnInfoAnalyticsEvent.InfoScreenOpened(
                    category = "ETH Lending",
                    source = EarnEntrySource.TokenPage,
                    positionBucket = "1K-5K",
                    earnedBucket = "<50",
                    currentApy = "5.50",
                )
                verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnInfoAnalyticsEvent.InfoScreenOpened>()) }
                verify(exactly = 1) { analyticsEventHandler.send(expected) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN another entry source WHEN shown THEN Info Screen Opened carries that source`() = runTest {
            // Arrange
            val model = createModel(source = EarnEntrySource.EarnBanner)

            // Act
            advanceUntilIdle()

            // Assert
            val expected = EarnInfoAnalyticsEvent.InfoScreenOpened(
                category = "ETH Lending",
                source = EarnEntrySource.EarnBanner,
                positionBucket = null,
                earnedBucket = null,
                currentApy = "5.50",
            )
            verify(exactly = 1) { analyticsEventHandler.send(expected) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN earned null WHEN shown THEN no Earned Bucket and earned is not valued`() = runTest {
            // Arrange
            product.stateFlow.value = product.readyState().withPosition(
                cryptoAmount = BigDecimal("1"),
                earned = null,
            )
            coEvery { usdValuation.bucket(ethereum, BigDecimal("1")) } returns "1K-5K"
            val model = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            val expected = EarnInfoAnalyticsEvent.InfoScreenOpened(
                category = "ETH Lending",
                source = EarnEntrySource.TokenPage,
                positionBucket = "1K-5K",
                earnedBucket = null,
                currentApy = "5.50",
            )
            verify(exactly = 1) { analyticsEventHandler.send(expected) }
            coVerify(exactly = 1) { usdValuation.bucket(any(), any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN two Ready frames WHEN collected THEN one Info Screen Opened`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            product.stateFlow.value = product.readyState().copy(description = stringReference("changed"))
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(ofType<EarnInfoAnalyticsEvent.InfoScreenOpened>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN no quote WHEN shown THEN Info Screen Opened without buckets`() = runTest {
            // Arrange
            coEvery { usdValuation.bucket(any(), any()) } returns null
            val model = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            val expected = EarnInfoAnalyticsEvent.InfoScreenOpened(
                category = "ETH Lending",
                source = EarnEntrySource.TokenPage,
                positionBucket = null,
                earnedBucket = null,
                currentApy = "5.50",
            )
            verify(exactly = 1) { analyticsEventHandler.send(expected) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN Unavailable WHEN frame THEN redirect and no Info Screen Opened`() = runTest {
            // Arrange
            product.stateFlow.value = EarnProductState.Unavailable
            val model = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            verifyRedirectedOnce()
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN position Unknown WHEN frame THEN redirect and no Info Screen Opened`() = runTest {
            // Arrange
            product.stateFlow.value = product.readyState().copy(position = EarnPositionState.Unknown)
            val model = createModel()

            // Act
            advanceUntilIdle()

            // Assert
            verifyRedirectedOnce()
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN shown WHEN onPeriodChange 3m THEN Chart Period 3M`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onPeriodChange(EarnPeriod(id = "3m", label = stringReference("3M")))

            // Assert
            verify(exactly = 1) { analyticsEventHandler.send(EarnInfoAnalyticsEvent.ChartPeriod("ETH Lending", "3M")) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN shown WHEN onPeriodChange with an unknown period THEN no Chart Period`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onPeriodChange(EarnPeriod(id = "1y", label = stringReference("1Y")))

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnInfoAnalyticsEvent.ChartPeriod>()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN shown WHEN Withdraw clicked THEN Info Screen Action Withdraw and the handler gets the action`() =
            runTest {
                // Arrange
                val model = createModel()
                advanceUntilIdle()
                val withdraw = product.readyState().secondary.first().action

                // Act
                requireNotNull(model.uiState.value).secondary.first().onClick()

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(EarnInfoAnalyticsEvent.InfoScreenAction("ETH Lending", InfoAction.Withdraw))
                }
                verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnPickerAnalyticsEvent.AddFunds>()) }
                assertThat(product.handled).containsExactly(withdraw)
                model.onDestroy()
            }

        @Test
        fun `GIVEN shown WHEN Deposit clicked THEN Info Screen Action Add Funds, Add Funds and the handler gets the action`() =
            runTest {
                // Arrange
                val model = createModel()
                advanceUntilIdle()
                val deposit = product.readyState().primary.action

                // Act
                requireNotNull(model.uiState.value).primary.onClick()

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(EarnInfoAnalyticsEvent.InfoScreenAction("ETH Lending", InfoAction.AddFunds))
                }
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        EarnPickerAnalyticsEvent.AddFunds(token = ethereum.symbol, source = EarnAddFundsSource.InfoScreen),
                    )
                }
                assertThat(product.handled).containsExactly(deposit)
                model.onDestroy()
            }

        @Test
        fun `GIVEN shown WHEN Claim clicked THEN no action event and the handler gets the action`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()
            val claim = requireNotNull(product.readyState().knownPosition().rewards).claimAction

            // Act
            requireNotNull(requireNotNull(model.uiState.value).rewards).claim.onClick()

            // Assert
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnInfoAnalyticsEvent.InfoScreenAction>()) }
            verify(exactly = 0) { analyticsEventHandler.send(ofType<EarnPickerAnalyticsEvent.AddFunds>()) }
            assertThat(product.handled).containsExactly(claim)
            model.onDestroy()
        }

        @Test
        fun `GIVEN shown WHEN onHorizonChange Year10 THEN Calculator Horizon with the product name`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onHorizonChange(EarnHorizon.Year10)

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(EarnPickerAnalyticsEvent.CalculatorHorizon("Lending", EarnHorizon.Year10))
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN shown WHEN onInputChange MonthlyTopUp THEN Calculator Input with the product name`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            model.onInputChange(EarnCalculatorComponent.Input.MonthlyTopUp)

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    EarnPickerAnalyticsEvent.CalculatorInput("Lending", EarnCalculatorComponent.Input.MonthlyTopUp),
                )
            }
            model.onDestroy()
        }

        @Test
        fun `GIVEN unknown product WHEN chart and calculator callbacks fire THEN nothing is sent and nothing throws`() =
            runTest {
                // Arrange
                val model = createModel(products = emptyList(), productId = EarnProductId("unknown"))
                advanceUntilIdle()

                // Act
                model.onPeriodChange(EarnPeriod(id = "3m", label = stringReference("3M")))
                model.onHorizonChange(EarnHorizon.Year10)
                model.onInputChange(EarnCalculatorComponent.Input.InitialBalance)

                // Assert
                verify(exactly = 0) { analyticsEventHandler.send(any()) }
                model.onDestroy()
            }

        private fun EarnProductState.Ready.withPosition(cryptoAmount: BigDecimal, earned: BigDecimal?) =
            copy(
                position = EarnPositionState.Known(
                    knownPosition().copy(cryptoAmount = cryptoAmount, earned = earned),
                ),
            )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Modals {

        @Test
        fun `GIVEN how it works WHEN onHowItWorksClick THEN the sheet shows its steps, footnote and learn more`() =
            runTest {
                // Arrange
                product.howItWorksContent = HOW_IT_WORKS
                val model = createModel()
                advanceUntilIdle()

                // Act
                requireNotNull(requireNotNull(model.uiState.value).myFunds.onHowItWorksClick).invoke()

                // Assert
                val actual = requireNotNull(model.howItWorks.value)
                val expected = EarnHowItWorksUM(
                    title = stringReference("How it works"),
                    steps = persistentListOf(
                        EarnHowItWorksUM.Step(stringReference("Supply"), stringReference("Your ETH is supplied")),
                    ),
                    footnote = stringReference("Audited"),
                    onLearnMoreClick = actual.onLearnMoreClick,
                )
                assertThat(actual).isEqualTo(expected)
                assertThat(actual.onLearnMoreClick).isNotNull()
                model.onDestroy()
            }

        @Test
        fun `GIVEN how it works sheet WHEN learn more clicked THEN the link of its post is built and opened`() =
            runTest {
                // Arrange
                product.howItWorksContent = HOW_IT_WORKS
                coEvery { blogUrlBuilder.build(TangemBlogUrlBuilder.Post.HowYieldModeWorks) } returns BLOG_URL
                val model = createModel()
                advanceUntilIdle()
                requireNotNull(requireNotNull(model.uiState.value).myFunds.onHowItWorksClick).invoke()

                // Act
                requireNotNull(model.howItWorks.value?.onLearnMoreClick).invoke()
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { urlOpener.openUrl(BLOG_URL) }
                model.onDestroy()
            }

        @Test
        fun `GIVEN how it works sheet WHEN dismissed THEN the sheet is hidden`() = runTest {
            // Arrange
            product.howItWorksContent = HOW_IT_WORKS
            val model = createModel()
            advanceUntilIdle()
            requireNotNull(requireNotNull(model.uiState.value).myFunds.onHowItWorksClick).invoke()

            // Act
            model.onHowItWorksDismiss()

            // Assert
            assertThat(model.howItWorks.value).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN a row with info WHEN onInfoClick THEN rowInfo holds its sheet`() = runTest {
            // Arrange
            val info = EarnRowInfo.Text(title = stringReference("Risk"), text = stringReference("How risky"))
            val row = EarnRow(stringReference("Risk"), EarnValue.Text(stringReference("Low")), info)
            product.stateFlow.value = product.readyState().copy(details = listOf(row))
            val model = createModel()
            advanceUntilIdle()

            // Act
            requireNotNull(requireNotNull(model.uiState.value).details.single().onInfoClick).invoke()

            // Assert
            val expected = EarnRowInfoUM.Text(title = stringReference("Risk"), text = stringReference("How risky"))
            assertThat(model.rowInfo.value).isEqualTo(expected)
            verify(exactly = 0) { uiMessageSender.send(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN an open row info WHEN onRowInfoDismiss THEN rowInfo is cleared`() = runTest {
            // Arrange
            val info = EarnRowInfo.Text(title = stringReference("Risk"), text = stringReference("How risky"))
            val row = EarnRow(stringReference("Risk"), EarnValue.Text(stringReference("Low")), info)
            product.stateFlow.value = product.readyState().copy(details = listOf(row))
            val model = createModel()
            advanceUntilIdle()
            requireNotNull(requireNotNull(model.uiState.value).details.single().onInfoClick).invoke()

            // Act
            model.onRowInfoDismiss()

            // Assert
            assertThat(model.rowInfo.value).isNull()
            model.onDestroy()
        }

        @Test
        fun `GIVEN a link row WHEN onLinkClick THEN urlOpener opens the url once`() = runTest {
            // Arrange
            val link = EarnRow(stringReference("Protocol"), EarnValue.Link(stringReference("Aave"), "https://aave.com"))
            product.stateFlow.value = product.readyState().copy(details = listOf(link))
            val model = createModel()
            advanceUntilIdle()

            // Act
            requireNotNull(requireNotNull(model.uiState.value).details.single().onValueClick).invoke()

            // Assert
            verify(exactly = 1) { urlOpener.openUrl("https://aave.com") }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PullToRefresh {

        @Test
        fun `GIVEN Ready WHEN pulled THEN the product refreshes once with the screen params and the flag is true until the call returns`() =
            runTest {
                // Arrange
                val gate = CompletableDeferred<Unit>()
                product.refreshGate = gate
                val model = createModel()
                advanceUntilIdle()

                // Act
                model.pull()
                runCurrent()
                val whileRefreshing = model.pullToRefresh.value.isRefreshing
                val callsWhileRefreshing = product.refreshCalls.toList()
                gate.complete(Unit)
                advanceUntilIdle()

                // Assert
                assertThat(whileRefreshing).isTrue()
                assertThat(callsWhileRefreshing).containsExactly(productParams)
                assertThat(model.pullToRefresh.value.isRefreshing).isFalse()
                assertThat(product.refreshCalls).containsExactly(productParams)
                model.onDestroy()
            }

        @Test
        fun `GIVEN a refresh in flight WHEN the screen closes THEN the refresh still finishes`() = runTest {
            // Arrange
            val gate = CompletableDeferred<Unit>()
            product.refreshGate = gate
            val model = createModel()
            advanceUntilIdle()
            model.pull()
            runCurrent()

            // Act
            model.onDestroy()
            gate.complete(Unit)
            advanceUntilIdle()

            // Assert
            assertThat(product.refreshesFinished).isEqualTo(1)
        }

        @Test
        fun `GIVEN a refresh in flight WHEN pulled again THEN refresh is not called twice`() = runTest {
            // Arrange
            val gate = CompletableDeferred<Unit>()
            product.refreshGate = gate
            val model = createModel()
            advanceUntilIdle()
            model.pull()
            runCurrent()

            // Act
            model.pull()
            runCurrent()

            // Assert
            assertThat(product.refreshCalls).hasSize(1)
            gate.complete(Unit)
            advanceUntilIdle()
            model.onDestroy()
        }

        @Test
        fun `GIVEN a finished refresh WHEN pulled again THEN refresh is called twice`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()
            model.pull()
            advanceUntilIdle()

            // Act
            model.pull()
            advanceUntilIdle()

            // Assert
            assertThat(product.refreshCalls).hasSize(2)
            model.onDestroy()
        }

        @Test
        fun `GIVEN Ready WHEN pulled THEN the quote is fetched in parallel for the raw currency id of the screen currency`() =
            runTest {
                // Arrange
                val gate = CompletableDeferred<Unit>()
                product.refreshGate = gate
                val model = createModel()
                advanceUntilIdle()
                val expected = SingleQuoteStatusFetcher.Params(
                    rawCurrencyId = checkNotNull(ethereum.id.rawCurrencyId),
                    appCurrencyId = null,
                )

                // Act
                model.pull()
                runCurrent()

                // Assert
                coVerify(exactly = 1) { singleQuoteStatusFetcher(expected) }
                gate.complete(Unit)
                advanceUntilIdle()
                model.onDestroy()
            }

        @Test
        fun `GIVEN a currency without a raw id WHEN pulled THEN no quote is fetched and the product still refreshes`() =
            runTest {
                // Arrange
                val customToken = MockCryptoCurrencyFactory().ethereum.copy(
                    id = CryptoCurrency.ID(
                        prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                        body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
                        suffix = CryptoCurrency.ID.Suffix.ContractAddress("0xcontract"),
                    ),
                )
                assertThat(customToken.id.rawCurrencyId).isNull()
                val model = createModel(cryptoCurrency = customToken)
                advanceUntilIdle()

                // Act
                model.pull()
                advanceUntilIdle()

                // Assert
                coVerify(exactly = 0) { singleQuoteStatusFetcher(any()) }
                assertThat(product.refreshCalls).hasSize(1)
                model.onDestroy()
            }

        @Test
        fun `GIVEN the product refresh returns Left WHEN pulled THEN the flag resets, uiState is unchanged, the router is untouched, the error is logged and the chart is not reloaded`() =
            runTest {
                // Arrange
                product.refreshResult = IllegalStateException("offline").left()
                val model = createModel()
                val reloads = getEmittedValues(model.chartReloads)
                advanceUntilIdle()
                val before = model.uiState.value

                // Act
                model.pull()
                advanceUntilIdle()

                // Assert
                assertThat(model.pullToRefresh.value.isRefreshing).isFalse()
                assertThat(model.uiState.value).isSameInstanceAs(before)
                verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
                verify(exactly = 1) {
                    TangemLogger.e("Earn info: refresh of ${product.id.value} failed", any(), any())
                }
                verify(exactly = 0) { router.navigate(any(), any()) }
                verify(exactly = 0) { router.pop(any()) }
                assertThat(reloads).isEmpty()
                model.onDestroy()
            }

        @Test
        fun `GIVEN the product refresh returns Right WHEN pulled THEN the chart reload is emitted once`() = runTest {
            // Arrange
            val model = createModel()
            val reloads = getEmittedValues(model.chartReloads)
            advanceUntilIdle()

            // Act
            model.pull()
            advanceUntilIdle()

            // Assert
            assertThat(reloads).containsExactly(Unit)
            model.onDestroy()
        }

        @Test
        fun `GIVEN the quote fetch returns Left WHEN pulled THEN it is logged, the flag resets and the chart is still reloaded`() =
            runTest {
                // Arrange
                coEvery { singleQuoteStatusFetcher(any()) } returns IllegalStateException("no quote").left()
                val model = createModel()
                val reloads = getEmittedValues(model.chartReloads)
                advanceUntilIdle()

                // Act
                model.pull()
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { TangemLogger.e(any(), any(), any()) }
                verify(exactly = 1) { TangemLogger.e("Earn info: quote refresh failed", any(), any()) }
                assertThat(model.pullToRefresh.value.isRefreshing).isFalse()
                assertThat(reloads).containsExactly(Unit)
                model.onDestroy()
            }

        @Test
        fun `GIVEN an unknown product WHEN pulled THEN nothing is fetched and the flag stays false`() = runTest {
            // Arrange
            val model = createModel(products = emptyList(), productId = EarnProductId("unknown"))
            advanceUntilIdle()

            // Act
            model.pull()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) { singleQuoteStatusFetcher(any()) }
            assertThat(product.refreshCalls).isEmpty()
            assertThat(model.pullToRefresh.value.isRefreshing).isFalse()
            model.onDestroy()
        }

        @Test
        fun `GIVEN Ready WHEN chartParams is built THEN its reloads is the chartReloads of the model`() = runTest {
            // Arrange
            val model = createModel()
            advanceUntilIdle()

            // Act
            val chartParams = model.chartParams()

            // Assert
            assertThat(chartParams.reloads).isSameInstanceAs(model.chartReloads)
            model.onDestroy()
        }
    }

    private fun EarnInfoModel.pull() = pullToRefresh.value.onRefresh(PullToRefreshConfig.ShowRefreshState())

    private fun TestScope.createModel(
        products: List<EarnProduct> = listOf(product),
        productId: EarnProductId = product.id,
        cryptoCurrency: CryptoCurrency = ethereum,
        source: EarnEntrySource = EarnEntrySource.TokenPage,
    ): EarnInfoModel = EarnInfoModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            EarnInfoComponent.Params(
                userWalletId = USER_WALLET_ID,
                cryptoCurrency = cryptoCurrency,
                productId = productId,
                source = source,
            ),
        ),
        registry = EarnProductsRegistry(products = products.toSet(), rateSources = emptySet()),
        singleAccountStatusListSupplier = accountStatusListSupplier,
        getSelectedAppCurrency = getSelectedAppCurrency,
        getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
        converter = converter,
        router = router,
        uiMessageSender = uiMessageSender,
        urlOpener = urlOpener,
        singleQuoteStatusFetcher = singleQuoteStatusFetcher,
        appScope = TestAppCoroutineScope(StandardTestDispatcher(testScheduler)),
        blogUrlBuilder = blogUrlBuilder,
        analyticsEventHandler = analyticsEventHandler,
        usdValuation = usdValuation,
    )

    /** Info redirects from under a flow pushed on top of it, so the flow must stay on top. */
    private fun verifyRedirectedOnce() {
        val transformer = slot<(List<Route>) -> List<Route>>()
        verify(exactly = 1) { router.navigate(capture(transformer), any()) }
        val stack = listOf(EarnInfoRoute(product.id), FlowOnTop)
        val expected = listOf(EarnPickerRoute(preselected = product.id, shouldRedirectToActive = false), FlowOnTop)
        assertThat(transformer.captured(stack)).isEqualTo(expected)
    }

    private object FlowOnTop : Route

    private fun knownState(): EarnProductState.Ready =
        fakeReadyState(FakeEarnProductPreset.Lending, FakeEarnPosition.Known)

    private fun EarnProductState.Ready.knownPosition() = (position as EarnPositionState.Known).position

    private fun stubStatus(status: CryptoCurrencyStatus?) {
        every {
            with(CryptoCurrencyStatusOperations) {
                accountStatusList.getCryptoCurrencyStatus(any<CryptoCurrency>())
            }
        } returns (status?.some() ?: none())
    }

    private fun status() = CryptoCurrencyStatus(
        currency = ethereum,
        value = CryptoCurrencyStatus.Custom(
            amount = BigDecimal.ONE,
            fiatAmount = null,
            fiatRate = BigDecimal("2000"),
            priceChange = null,
            yieldSupplyStatus = null,
            hasCurrentNetworkTransactions = false,
            pendingTransactions = emptySet(),
            networkAddress = NetworkAddress.Single(
                NetworkAddress.Address(value = "0xUser", type = NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    private fun EarnInfoUM.withoutCallbacks(): EarnInfoUM = copy(
        rewards = rewards?.copy(claim = rewards.claim.copy(onClick = NoOp)),
        myFunds = myFunds.copy(rows = persistentListOf(), onHowItWorksClick = null),
        details = persistentListOf(),
        secondary = secondary.map { it.copy(onClick = NoOp) }.toImmutableList(),
        primary = primary.copy(onClick = NoOp),
        onClose = NoOp,
    )

    /** Mirrors what a real `childSlot` tracks, since a bare [SlotNavigation] only relays events. */
    private fun <C : Any> SlotNavigation<C>.track(): List<C?> {
        val tracked = mutableListOf<C?>()
        subscribe { event -> tracked += event.transformer(tracked.lastOrNull()) }
        return tracked
    }

    internal class InfoProduct(
        private val delegate: EarnProduct,
        initialState: EarnProductState,
        private val customState: Flow<EarnProductState>? = null,
    ) : EarnProduct by delegate {

        val stateFlow = MutableStateFlow(initialState)

        var howItWorksContent: EarnHowItWorks? = delegate.howItWorks

        override val howItWorks: EarnHowItWorks?
            get() = howItWorksContent

        val handled = mutableListOf<EarnAction>()

        val apyHistoryCalls = mutableListOf<Pair<EarnProduct.Params, EarnPeriod>>()

        var actionHandlerCalls: Int = 0
            private set

        val refreshCalls = mutableListOf<EarnProduct.Params>()

        var refreshResult: Either<Throwable, Unit> = Unit.right()

        var refreshGate: CompletableDeferred<Unit>? = null

        var refreshesFinished: Int = 0

        fun readyState(): EarnProductState.Ready = stateFlow.value as EarnProductState.Ready

        override fun state(params: EarnProduct.Params): Flow<EarnProductState> = customState ?: stateFlow

        override suspend fun apyHistory(params: EarnProduct.Params, period: EarnPeriod): Either<Throwable, EarnApyHistory> {
            apyHistoryCalls += params to period
            return delegate.apyHistory(params, period)
        }

        override suspend fun refresh(params: EarnProduct.Params): Either<Throwable, Unit> {
            refreshCalls += params
            refreshGate?.await()
            refreshesFinished++
            return refreshResult
        }

        override fun actionHandler(params: EarnProduct.Params, navigator: EarnNavigator): EarnActionHandler {
            actionHandlerCalls++
            return EarnActionHandler { handled += it }
        }
    }

    private companion object {

        const val BLOG_URL = "https://tangem.com/blog/yield-mode"

        val HOW_IT_WORKS = EarnHowItWorks(
            title = stringReference("How it works"),
            entries = listOf(EarnTextBlock(stringReference("Supply"), stringReference("Your ETH is supplied"))),
            footnote = stringReference("Audited"),
            learnMore = TangemBlogUrlBuilder.Post.HowYieldModeWorks,
        )

        val NoOp: () -> Unit = {}

        val NoOpCallbacks = EarnInfoConverter.Callbacks(
            onActionClick = {},
            onBlocked = {},
            onInfoClick = {},
            onLinkClick = {},
            onHowItWorksClick = {},
            onClose = NoOp,
        )
    }
}
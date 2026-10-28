package com.tangem.feature.swap.mcp.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.core.error.DataError
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.models.ExchangeIntentError
import com.tangem.domain.swap.usecase.GetExchangeIntentUseCase
import com.tangem.feature.swap.mcp.createCoin
import com.tangem.feature.swap.mcp.createExchangeIntent
import com.tangem.feature.swap.mcp.createNetwork
import com.tangem.feature.swap.mcp.createToken
import com.tangem.feature.swap.mcp.holdings.ExchangeIntentHoldings
import com.tangem.feature.swap.mcp.holdings.ExchangeIntentHoldingsResolver
import com.tangem.feature.swap.mcp.ExchangeIntentAmountConverter
import com.tangem.feature.swap.mcp.ExchangeIntentSwapRouteConverter
import com.tangem.feature.swap.presentation.R
import com.tangem.features.swap.mcp.ExchangeIntentComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.jupiter.params.ParameterizedTest
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@ResourceLock(Resources.LOCALE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExchangeIntentModelTest {

    private val router: Router = mockk(relaxed = true)
    private val getExchangeIntentUseCase: GetExchangeIntentUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val holdingsResolver: ExchangeIntentHoldingsResolver = mockk()
    private val mcpRepository: McpRepository = mockk()
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val amountConverter = ExchangeIntentAmountConverter()
    private val swapRouteConverter = ExchangeIntentSwapRouteConverter(amountConverter)
    private val swapTextConverter = ExchangeIntentSwapTextConverter(amountConverter)
    private val offerConverter = ExchangeIntentOfferConverter(swapTextConverter)
    private val processedConverter = ExchangeIntentProcessedConverter(swapTextConverter)
    private val cantReachConverter = ExchangeIntentCantReachConverter()

    private val walletId = UserWalletId("011")
    private val intent = createExchangeIntent()

    private val usdt = createToken()
    private val claude = McpAgent(
        clientId = "client",
        origin = "mcp.claude.com",
        name = "Claude",
        iconUrl = null,
    )
    private val pol = createCoin(network = createNetwork(rawId = "polygon-pos", name = "Polygon"), symbol = "POL")

    /** Root stack the [router] falls back to; the model navigates it only through [Router.navigate]. */
    private var rootStack: List<Route> = emptyList()

    private lateinit var originalLocale: Locale

    @BeforeEach
    fun resetMocks() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        clearMocks(
            router,
            getExchangeIntentUseCase,
            userWalletsListRepository,
            holdingsResolver,
            mcpRepository,
            analyticsEventHandler,
        )
        rootStack = emptyList()
        every { router.navigate(any(), any()) } answers {
            rootStack = firstArg<(List<Route>) -> List<Route>>().invoke(rootStack)
            secondArg<(Boolean) -> Unit>().invoke(true)
        }
        coEvery { userWalletsListRepository.selectedUserWalletSync() } returns
            mockk<UserWallet> { every { this@mockk.walletId } returns this@ExchangeIntentModelTest.walletId }
        coEvery { holdingsResolver.resolve(walletId, any()) } returns ExchangeIntentHoldings.Both(from = usdt, to = pol)
        coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns intent.right()
        coEvery { mcpRepository.getAgent(any()) } returns claude
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `GIVEN intent request in flight WHEN model created THEN nothing is shown yet`() = runTest {
        // Arrange
        val response = CompletableDeferred<Either<Throwable, ExchangeIntent>>()
        coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } coAnswers { response.await() }

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.sheet.value).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN open intent the wallet can carry out WHEN model created THEN offer sheet opens naming the swap`() =
        runTest {
            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.shownOffer().title.toString()).contains("POL")
            model.onDestroy()
        }

    @Test
    fun `GIVEN known agent WHEN model created THEN offer is made in its verified origin`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.shownOffer().title).isEqualTo(
            resourceReference(
                R.string.mcp_swap_intent_offer_title,
                wrappedList(stringReference("mcp.claude.com"), "42.00\u00a0USDT", "POL"),
            ),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN agent lookup that never answers WHEN time passes THEN offer is made for the unknown agent`() = runTest {
        // Arrange
        coEvery { mcpRepository.getAgent(any()) } coAnswers { awaitCancellation() }

        // Act
        val model = createModel(testScope = this)
        advanceTimeBy(AGENT_LOOKUP_TIMEOUT_MILLIS + 1)
        runCurrent()

        // Assert
        assertThat(model.shownOffer().title).isEqualTo(
            resourceReference(
                R.string.mcp_swap_intent_offer_title,
                wrappedList(resourceReference(R.string.mcp_intent_unknown_agent_name), "42.00\u00a0USDT", "POL"),
            ),
        )
        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN intent that cannot be reached WHEN model created THEN can't reach sheet is shown and closes once`(
        model: CantReachModel,
    ) = runTest {
        // Arrange
        model.arrange()

        // Act
        val exchangeIntentModel = createModel(testScope = this)
        advanceUntilIdle()
        val status = exchangeIntentModel.shownStatus()
        status.primaryButton.onClick()
        status.primaryButton.onClick()

        // Assert
        assertThat(status.variant).isEqualTo(TangemModalStatus.Variant.Warning)
        assertThat(status.title).isEqualTo(resourceReference(R.string.mcp_swap_intent_cant_reach_title))
        assertThat(status.subtitle).isNull()
        assertThat(status.primaryButton.text).isEqualTo(resourceReference(R.string.common_got_it))
        verify(exactly = 1) { router.pop(any()) }
        exchangeIntentModel.onDestroy()
    }

    data class CantReachModel(val description: String, val arrange: () -> Unit) {
        override fun toString() = description
    }

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        CantReachModel(description = "intent belongs to another wallet") {
            coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns
                DataError.UserWalletError.WrongUserWallet("another wallet").left()
        },
        CantReachModel(description = "intent is not found") {
            coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns
                ExchangeIntentError.NotFound(INTENT_ID).left()
        },
        CantReachModel(description = "intent cannot be read for an unknown reason") {
            coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns IllegalStateException("no network").left()
        },
        CantReachModel(description = "wallet lacks the token to swap to") {
            coEvery { holdingsResolver.resolve(walletId, any()) } returns ExchangeIntentHoldings.MissingTo(from = usdt)
        },
        CantReachModel(description = "accounts of the wallet cannot be read") {
            coEvery { holdingsResolver.resolve(walletId, any()) } returns null
        },
        CantReachModel(description = "amount of the intent is malformed") {
            coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns intent.copy(amount = "many").right()
        },
    )

    @Test
    fun `GIVEN no wallet is selected WHEN model created THEN no sheet is shown and the slot is released once`() =
        runTest {
            // Arrange
            coEvery { userWalletsListRepository.selectedUserWalletSync() } returns null

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.onClose()

            // Assert
            assertThat(model.sheet.value).isNull()
            verify(exactly = 1) { router.pop(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN intent already carried out WHEN model created THEN processed sheet names the swap and closes once`() =
        runTest {
            // Arrange
            val processedIntent = intent.copy(isHandled = true)
            coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns processedIntent.right()
            val model = createModel(testScope = this)
            val expected = processedConverter.convert(
                ExchangeIntentProcessedConverter.Input(
                    intent = processedIntent,
                    from = usdt,
                    to = pol,
                    onClose = model::onClose,
                ),
            )

            // Act
            advanceUntilIdle()
            val status = model.shownStatus()
            status.primaryButton.onClick()
            status.primaryButton.onClick()

            // Assert
            assertThat(status).isEqualTo(expected)
            verify(exactly = 1) { router.pop(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN intent already carried out and tokens not held WHEN model created THEN processed sheet has no subtitle`() =
        runTest {
            // Arrange
            coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns intent.copy(isHandled = true).right()
            coEvery { holdingsResolver.resolve(walletId, any()) } returns ExchangeIntentHoldings.MissingBoth

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.shownStatus().subtitle).isNull()
            model.onDestroy()
        }

    @Test
    fun `GIVEN no connection WHEN model created THEN can't reach sheet offers to try again`() = runTest {
        // Arrange
        coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns
            ExchangeIntentError.Unreachable(INTENT_ID).left()

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val status = model.shownStatus()
        assertThat(status.variant).isEqualTo(TangemModalStatus.Variant.Warning)
        assertThat(status.primaryButton.text).isEqualTo(resourceReference(R.string.alert_button_try_again))
        verify(exactly = 0) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no connection WHEN try again and the intent is read THEN the offer replaces the status`() = runTest {
        // Arrange
        coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returnsMany listOf(
            ExchangeIntentError.Unreachable(INTENT_ID).left(),
            intent.right(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.shownStatus().primaryButton.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.shownOffer().title.toString()).contains("POL")
        coVerify(exactly = 2) { getExchangeIntentUseCase(walletId, INTENT_ID) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no connection WHEN try again is tapped while reading THEN the sheet stays and reading is not repeated`() =
        runTest {
            // Arrange
            val response = CompletableDeferred<Either<Throwable, ExchangeIntent>>()
            val unreachable: Either<Throwable, ExchangeIntent> =
                ExchangeIntentError.Unreachable(INTENT_ID).left()
            var calls = 0
            coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } coAnswers {
                if (calls++ == 0) unreachable else response.await()
            }
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val status = model.shownStatus()

            // Act
            status.primaryButton.onClick()
            status.primaryButton.onClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.sheet.value).isSameInstanceAs(status)
            coVerify(exactly = 2) { getExchangeIntentUseCase(walletId, INTENT_ID) }
            response.complete(intent.right())
            advanceUntilIdle()
            assertThat(model.shownOffer().title.toString()).contains("POL")
            model.onDestroy()
        }

    @Test
    fun `GIVEN no connection WHEN try again and it fails again THEN the can't reach sheet is shown again`() = runTest {
        // Arrange
        coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns
            ExchangeIntentError.Unreachable(INTENT_ID).left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.shownStatus().primaryButton.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.shownStatus().primaryButton.text).isEqualTo(resourceReference(R.string.alert_button_try_again))
        coVerify(exactly = 2) { getExchangeIntentUseCase(walletId, INTENT_ID) }
        verify(exactly = 0) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN offer WHEN proceed THEN swap opens bound to the intent and sheet closes`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val offer = model.shownOffer()

        // Act
        offer.primaryButton.onClick()

        // Assert
        assertThat(rootStack).containsExactly(expectedSwapRoute())
        verify(exactly = 1) { router.pop(any()) }
        assertThat(model.sheet.value).isEqualTo(offer)
        model.onDestroy()
    }

    @Test
    fun `GIVEN swap already open WHEN proceed THEN it is replaced`() = runTest {
        // Arrange
        rootStack = listOf(AppRoute.Swap(userWalletId = walletId, screenSource = "Main"))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.shownOffer().primaryButton.onClick()

        // Assert
        assertThat(rootStack).containsExactly(expectedSwapRoute())
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN account flow swap on top WHEN proceed THEN intent swap is pushed without replacing it`() = runTest {
        // Arrange
        val accountFlowSwap = AppRoute.Swap(
            userWalletId = walletId,
            screenSource = "Main",
            accountFlow = AppRoute.Swap.AccountFlow.TopUp,
        )
        rootStack = listOf(accountFlowSwap)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.shownOffer().primaryButton.onClick()

        // Assert
        assertThat(rootStack).containsExactly(accountFlowSwap, expectedSwapRoute()).inOrder()
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN intent swap covered by account flow WHEN proceed THEN existing intent swap is revealed`() = runTest {
        // Arrange
        rootStack = listOf(
            expectedSwapRoute(),
            AppRoute.Swap(
                userWalletId = walletId,
                screenSource = "Main",
                accountFlow = AppRoute.Swap.AccountFlow.TopUp,
            ),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.shownOffer().primaryButton.onClick()

        // Assert
        assertThat(rootStack).containsExactly(expectedSwapRoute())
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN offer WHEN sheet is dismissed THEN the queue slot is released once and nothing else happens`() =
        runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val offer = model.shownOffer()

            // Act
            model.onClose()
            model.onClose()

            // Assert
            assertThat(model.sheet.value).isEqualTo(offer)
            verify(exactly = 1) { router.pop(any()) }
            verify(exactly = 0) { router.navigate(any(), any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN intent still loading WHEN closed and the answer arrives later THEN nothing more happens`() = runTest {
        // Arrange
        val response = CompletableDeferred<Either<Throwable, ExchangeIntent>>()
        coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } coAnswers { response.await() }
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onClose()
        response.complete(intent.right())
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
        assertThat(model.sheet.value).isNull()
        model.onDestroy()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CallbackCompletion {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN retained offer callbacks WHEN completion repeats THEN only first completion has effects`(
            testModel: CompletionModel,
        ) = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val proceed = model.shownOffer().primaryButton.onClick

            // Act
            testModel.actions.forEach { action ->
                when (action) {
                    CompletionAction.Close -> model.onClose()
                    CompletionAction.Proceed -> proceed()
                    CompletionAction.Destroy -> model.onDestroy()
                }
            }

            // Assert
            verify(exactly = testModel.expectedPops) { router.pop(any()) }
            verify(exactly = testModel.expectedNavigations) { router.navigate(any(), any()) }
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            CompletionModel(listOf(CompletionAction.Close, CompletionAction.Close), 1, 0),
            CompletionModel(listOf(CompletionAction.Close, CompletionAction.Proceed), 1, 0),
            CompletionModel(listOf(CompletionAction.Proceed, CompletionAction.Proceed), 1, 1),
            CompletionModel(listOf(CompletionAction.Proceed, CompletionAction.Close), 1, 1),
            CompletionModel(listOf(CompletionAction.Destroy, CompletionAction.Proceed), 0, 0),
            CompletionModel(listOf(CompletionAction.Destroy, CompletionAction.Close), 0, 0),
            CompletionModel(listOf(CompletionAction.Close, CompletionAction.Destroy, CompletionAction.Proceed), 1, 0),
            CompletionModel(listOf(CompletionAction.Proceed, CompletionAction.Destroy, CompletionAction.Close), 1, 1),
        )

        @Test
        fun `GIVEN navigation triggers dismissal WHEN proceed THEN queue is released only once`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            every { router.navigate(any(), any()) } answers { model.onClose() }

            // Act
            model.shownOffer().primaryButton.onClick()

            // Assert
            verify(exactly = 1) { router.navigate(any(), any()) }
            verify(exactly = 1) { router.pop(any()) }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LoadingCompletion {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN noncancellable intent request WHEN closed before response THEN late response has no effects`(
            testModel: LateResponseModel,
        ) = runTest {
            // Arrange
            val response = CompletableDeferred<Either<Throwable, ExchangeIntent>>()
            coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } coAnswers {
                withContext(NonCancellable) { response.await() }
            }
            val model = createModel(testScope = this)
            runCurrent()

            // Act
            model.onClose()
            response.complete(testModel.response)
            advanceUntilIdle()

            // Assert
            assertThat(model.sheet.value).isNull()
            verify(exactly = 1) { router.pop(any()) }
            verify(exactly = 0) { router.navigate(any(), any()) }
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            LateResponseModel(description = "success", response = intent.right()),
            LateResponseModel(description = "failure", response = IllegalStateException("unavailable").left()),
        )

        @Test
        fun `GIVEN agent lookup in flight WHEN closed THEN loading is cancelled without showing an offer`() = runTest {
            // Arrange
            var lookupCancelled = false
            coEvery { mcpRepository.getAgent(any()) } coAnswers {
                try {
                    awaitCancellation()
                } finally {
                    lookupCancelled = true
                }
            }
            val model = createModel(testScope = this)
            runCurrent()

            // Act
            model.onClose()
            runCurrent()

            // Assert
            assertThat(lookupCancelled).isTrue()
            assertThat(model.sheet.value).isNull()
            verify(exactly = 1) { router.pop(any()) }
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Analytics {

        @Test
        fun `GIVEN open intent WHEN offer is shown THEN screen opened names the agent and the FROM token`() = runTest {
            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(sentEvents()).containsExactly(
                "[MCP Intent] Intent Screen Opened" to mapOf(
                    "Intent Type" to "exchange",
                    "Agent Name" to "mcp.claude.com",
                    "Blockchain" to usdt.network.name,
                    "Token" to usdt.symbol,
                    "Intent Id" to INTENT_ID,
                ),
            )
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN intent that is not offered WHEN its sheet is shown THEN load failed carries the reason`(
            testModel: LoadFailedModel,
        ) = runTest {
            // Arrange
            testModel.arrange()

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(sentEvents()).containsExactly(
                "[MCP Intent] Intent Load Failed" to buildMap {
                    put("Intent Type", "exchange")
                    put("Reason", testModel.expectedReason)
                    testModel.expectedErrorCode?.let { put("Error Code", it) }
                    put("Intent Id", INTENT_ID)
                },
            )
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            LoadFailedModel("intent of another wallet", "Wrong Wallet", "2610") {
                coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns
                    DataError.UserWalletError.WrongUserWallet("another wallet").left()
            },
            LoadFailedModel("intent not found", "Not Found", "2600") {
                coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns
                    ExchangeIntentError.NotFound(INTENT_ID).left()
            },
            LoadFailedModel("no connection", "Unreachable") {
                coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns
                    ExchangeIntentError.Unreachable(INTENT_ID).left()
            },
            LoadFailedModel("amount of both sides", "Invalid Amount") {
                coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns
                    ExchangeIntentError.AmbiguousAmountSide(INTENT_ID).left()
            },
            LoadFailedModel("server error", "Unknown", "502") {
                coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns ApiResponseError.HttpException(
                    code = ApiResponseError.HttpException.Code.BAD_GATEWAY,
                    message = null,
                    errorBody = null,
                ).left()
            },
            LoadFailedModel("unknown read error", "Unknown") {
                coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns
                    IllegalStateException("boom").left()
            },
            LoadFailedModel("already carried out", "Already Processed") {
                coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns intent.copy(isHandled = true).right()
            },
            LoadFailedModel("token to swap to missing", "Token Missing") {
                coEvery { holdingsResolver.resolve(walletId, any()) } returns ExchangeIntentHoldings.MissingTo(usdt)
            },
            LoadFailedModel("token to swap from missing", "Token Missing") {
                coEvery { holdingsResolver.resolve(walletId, any()) } returns ExchangeIntentHoldings.MissingFrom(pol)
            },
            LoadFailedModel("both tokens missing", "Token Missing") {
                coEvery { holdingsResolver.resolve(walletId, any()) } returns ExchangeIntentHoldings.MissingBoth
            },
            LoadFailedModel("accounts unreadable", "Unknown") {
                coEvery { holdingsResolver.resolve(walletId, any()) } returns null
            },
            LoadFailedModel("malformed amount", "Invalid Amount") {
                coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns intent.copy(amount = "many").right()
            },
        )

        @Test
        fun `GIVEN offer WHEN proceed is tapped twice THEN continue is sent once`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()
            val proceed = model.shownOffer().primaryButton.onClick

            // Act
            proceed()
            proceed()

            // Assert
            assertThat(sentEvents().last()).isEqualTo(
                "[MCP Intent] Button - Continue" to mapOf("Intent Type" to "exchange", "Intent Id" to INTENT_ID),
            )
            assertThat(sentEvents().count { it.first == "[MCP Intent] Button - Continue" }).isEqualTo(1)
            model.onDestroy()
        }

        @Test
        fun `GIVEN offer WHEN swap cannot be opened THEN continue is not sent`() = runTest {
            // Arrange
            every { router.navigate(any(), any()) } answers { secondArg<(Boolean) -> Unit>().invoke(false) }
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.shownOffer().primaryButton.onClick()

            // Assert
            assertThat(sentEvents().map { it.first }).doesNotContain("[MCP Intent] Button - Continue")
            model.onDestroy()
        }

        @Test
        fun `GIVEN no wallet is selected WHEN model created THEN no analytics is sent`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.selectedUserWalletSync() } returns null

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(sentEvents()).isEmpty()
            model.onDestroy()
        }
    }

    data class LoadFailedModel(
        val description: String,
        val expectedReason: String,
        val expectedErrorCode: String? = null,
        val arrange: () -> Unit,
    ) {
        override fun toString() = description
    }

    private fun sentEvents(): List<Pair<String, Map<String, String>>> {
        val events = mutableListOf<AnalyticsEvent>()
        verify(atLeast = 0) { analyticsEventHandler.send(capture(events)) }
        return events.map { it.id to it.params }
    }

    enum class CompletionAction { Close, Proceed, Destroy }

    data class CompletionModel(
        val actions: List<CompletionAction>,
        val expectedPops: Int,
        val expectedNavigations: Int,
    )

    data class LateResponseModel(val description: String, val response: Either<Throwable, ExchangeIntent>) {
        override fun toString() = description
    }

    private fun ExchangeIntentModel.shownOffer(): ExchangeIntentUM.Offer = sheet.value as ExchangeIntentUM.Offer

    private fun ExchangeIntentModel.shownStatus(): ExchangeIntentUM.Status = sheet.value as ExchangeIntentUM.Status

    private fun expectedSwapRoute() = swapRouteConverter.convert(
        ExchangeIntentSwapRouteConverter.Input(intent = intent, userWalletId = walletId, from = usdt, to = pol),
    )

    private fun createModel(testScope: TestScope): ExchangeIntentModel = ExchangeIntentModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        getExchangeIntentUseCase = getExchangeIntentUseCase,
        userWalletsListRepository = userWalletsListRepository,
        holdingsResolver = holdingsResolver,
        mcpRepository = mcpRepository,
        offerConverter = offerConverter,
        processedConverter = processedConverter,
        cantReachConverter = cantReachConverter,
        swapRouteConverter = swapRouteConverter,
        analyticsEventHandler = analyticsEventHandler,
        paramsContainer = MutableParamsContainer(value = ExchangeIntentComponent.Params(intentId = INTENT_ID)),
    )

    private companion object {
        const val INTENT_ID = "my_intent_id123"
        const val AGENT_LOOKUP_TIMEOUT_MILLIS = 3_000L
    }
}
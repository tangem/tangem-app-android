package com.tangem.feature.swap.mcp.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.core.error.DataError
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.ExchangeIntent
import com.tangem.domain.swap.usecase.GetExchangeIntentUseCase
import com.tangem.feature.swap.mcp.createCoin
import com.tangem.feature.swap.mcp.createExchangeIntent
import com.tangem.feature.swap.mcp.createNetwork
import com.tangem.feature.swap.mcp.createToken
import com.tangem.feature.swap.mcp.holdings.ExchangeIntentHoldings
import com.tangem.feature.swap.mcp.holdings.ExchangeIntentHoldingsResolver
import com.tangem.feature.swap.mcp.ExchangeIntentAmountConverter
import com.tangem.feature.swap.mcp.ExchangeIntentSwapRouteConverter
import com.tangem.features.swap.mcp.ExchangeIntentComponent
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExchangeIntentModelTest {

    private val router: Router = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val getExchangeIntentUseCase: GetExchangeIntentUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val holdingsResolver: ExchangeIntentHoldingsResolver = mockk()
    private val mcpRepository: McpRepository = mockk()
    private val amountConverter = ExchangeIntentAmountConverter()
    private val swapRouteConverter = ExchangeIntentSwapRouteConverter(amountConverter)
    private val offerConverter = ExchangeIntentOfferConverter(amountConverter)

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

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            appRouter,
            messageSender,
            getExchangeIntentUseCase,
            userWalletsListRepository,
            holdingsResolver,
            mcpRepository,
        )
        every { appRouter.stack } returns emptyList()
        coEvery { userWalletsListRepository.selectedUserWalletSync() } returns
            mockk<UserWallet> { every { this@mockk.walletId } returns this@ExchangeIntentModelTest.walletId }
        coEvery { holdingsResolver.resolve(walletId, any()) } returns ExchangeIntentHoldings.Both(from = usdt, to = pol)
        coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns intent.right()
        coEvery { mcpRepository.getAgent(any()) } returns claude
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
        assertThat(model.offer.value).isNull()
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
        assertThat(model.shownOffer().title.toString()).contains("${claude.origin} Agent")
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
        assertThat(model.shownOffer().title.toString()).contains("Unknown Agent")
        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN intent that cannot be offered WHEN model created THEN toast is sent and sheet closes`(
        model: CloseModel,
    ) = runTest {
        // Arrange
        model.arrange()

        // Act
        val exchangeIntentModel = createModel(testScope = this)
        advanceUntilIdle()
        exchangeIntentModel.onClose()
        exchangeIntentModel.onClose()

        // Assert
        verify(exactly = 1) { messageSender.send(any<ToastMessage>()) }
        verify(exactly = 1) { router.pop(any()) }
        assertThat(exchangeIntentModel.offer.value).isNull()
        exchangeIntentModel.onDestroy()
    }

    data class CloseModel(val description: String, val arrange: () -> Unit) {
        override fun toString() = description
    }

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        CloseModel(description = "no wallet is selected") {
            coEvery { userWalletsListRepository.selectedUserWalletSync() } returns null
        },
        CloseModel(description = "intent belongs to another wallet") {
            coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns
                DataError.UserWalletError.WrongUserWallet("another wallet").left()
        },
        CloseModel(description = "intent was already carried out") {
            coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns intent.copy(isHandled = true).right()
        },
        CloseModel(description = "wallet lacks the token to swap to") {
            coEvery { holdingsResolver.resolve(walletId, any()) } returns ExchangeIntentHoldings.MissingTo(from = usdt)
        },
        CloseModel(description = "accounts of the wallet cannot be read") {
            coEvery { holdingsResolver.resolve(walletId, any()) } returns null
        },
        CloseModel(description = "amount of the intent is malformed") {
            coEvery { getExchangeIntentUseCase(walletId, INTENT_ID) } returns intent.copy(amount = "many").right()
        },
    )

    @Test
    fun `GIVEN offer WHEN proceed THEN swap opens bound to the intent and sheet closes`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val offer = model.shownOffer()

        // Act
        offer.primaryButton.onClick()

        // Assert
        verify(exactly = 1) { appRouter.push(expectedSwapRoute(), any()) }
        verify(exactly = 1) { router.pop(any()) }
        assertThat(model.offer.value).isEqualTo(offer)
        model.onDestroy()
    }

    @Test
    fun `GIVEN swap already open WHEN proceed THEN it is replaced`() = runTest {
        // Arrange
        every { appRouter.stack } returns listOf(AppRoute.Swap(userWalletId = walletId, screenSource = "Main"))
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.shownOffer().primaryButton.onClick()

        // Assert
        verify(exactly = 1) { appRouter.replaceCurrent(expectedSwapRoute(), any()) }
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN account flow swap on top WHEN proceed THEN intent swap is pushed without replacing it`() = runTest {
        // Arrange
        every { appRouter.stack } returns listOf(
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
        verify(exactly = 1) { appRouter.push(expectedSwapRoute(), any()) }
        verify(exactly = 0) { appRouter.replaceCurrent(any(), any()) }
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN intent swap covered by account flow WHEN proceed THEN existing intent swap is revealed`() = runTest {
        // Arrange
        every { appRouter.stack } returns listOf(
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
        verify(exactly = 1) { appRouter.popTo(expectedSwapRoute(), any()) }
        verify(exactly = 0) { appRouter.push(any(), any()) }
        verify(exactly = 0) { appRouter.replaceCurrent(any(), any()) }
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
            assertThat(model.offer.value).isEqualTo(offer)
            verify(exactly = 1) { router.pop(any()) }
            verify(exactly = 0) { appRouter.push(any(), any()) }
            verify(exactly = 0) { messageSender.send(any<ToastMessage>()) }
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
        verify(exactly = 0) { messageSender.send(any<ToastMessage>()) }
        assertThat(model.offer.value).isNull()
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
            verify(exactly = testModel.expectedNavigations) { appRouter.push(expectedSwapRoute(), any()) }
            verify(exactly = 0) { appRouter.replaceCurrent(any(), any()) }
            verify(exactly = 0) { messageSender.send(any<ToastMessage>()) }
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
            every { appRouter.push(any(), any()) } answers { model.onClose() }

            // Act
            model.shownOffer().primaryButton.onClick()

            // Assert
            verify(exactly = 1) { appRouter.push(expectedSwapRoute(), any()) }
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
            assertThat(model.offer.value).isNull()
            verify(exactly = 1) { router.pop(any()) }
            verify(exactly = 0) { appRouter.push(any(), any()) }
            verify(exactly = 0) { messageSender.send(any<ToastMessage>()) }
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
            assertThat(model.offer.value).isNull()
            verify(exactly = 1) { router.pop(any()) }
            verify(exactly = 0) { messageSender.send(any<ToastMessage>()) }
            model.onDestroy()
        }
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

    private fun ExchangeIntentModel.shownOffer(): ExchangeIntentUM = checkNotNull(offer.value)

    private fun expectedSwapRoute() = swapRouteConverter.convert(
        ExchangeIntentSwapRouteConverter.Input(intent = intent, userWalletId = walletId, from = usdt, to = pol),
    )

    private fun createModel(testScope: TestScope): ExchangeIntentModel = ExchangeIntentModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        appRouter = appRouter,
        messageSender = messageSender,
        getExchangeIntentUseCase = getExchangeIntentUseCase,
        userWalletsListRepository = userWalletsListRepository,
        holdingsResolver = holdingsResolver,
        mcpRepository = mcpRepository,
        offerConverter = offerConverter,
        swapRouteConverter = swapRouteConverter,
        paramsContainer = MutableParamsContainer(value = ExchangeIntentComponent.Params(intentId = INTENT_ID)),
    )

    private companion object {
        const val INTENT_ID = "my_intent_id123"
        const val AGENT_LOOKUP_TIMEOUT_MILLIS = 3_000L
    }
}
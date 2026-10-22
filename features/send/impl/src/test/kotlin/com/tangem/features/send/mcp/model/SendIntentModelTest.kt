package com.tangem.features.send.mcp.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SendIntentError
import com.tangem.domain.transaction.models.SendIntent
import com.tangem.domain.transaction.usecase.GetSendIntentUseCase
import com.tangem.features.send.api.mcp.SendIntentComponent
import com.tangem.features.send.mcp.SendIntentSendRouteConverter
import com.tangem.features.send.mcp.SendIntentSource
import com.tangem.features.send.mcp.SendIntentSourceResolver
import com.tangem.features.send.mcp.createSendIntent
import com.tangem.features.send.mcp.createToken
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@ResourceLock(Resources.LOCALE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SendIntentModelTest {

    private val router: Router = mockk(relaxed = true)
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val getSendIntentUseCase: GetSendIntentUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val sourceResolver: SendIntentSourceResolver = mockk()
    private val mcpRepository: McpRepository = mockk()
    private val offerConverter = SendIntentOfferConverter()
    private val sendRouteConverter = SendIntentSendRouteConverter()

    private val walletId = UserWalletId("011")
    private val intent = createSendIntent(id = INTENT_ID, memo = "memo-7")

    private val usdt = createToken()
    private val usdtStatus: CryptoCurrencyStatus = mockk { every { currency } returns usdt }
    private val claude = McpAgent(
        clientId = "client",
        origin = "mcp.claude.com",
        name = "Claude",
        iconUrl = "https://icons.tangem.com/claude.png",
    )

    /** Root stack the [router] falls back to; the model navigates it only through [Router.navigate]. */
    private var rootStack: List<Route> = emptyList()

    private lateinit var originalLocale: Locale

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        clearMocks(
            router,
            messageSender,
            getSendIntentUseCase,
            userWalletsListRepository,
            sourceResolver,
            mcpRepository,
        )
        rootStack = emptyList()
        every { router.navigate(any(), any()) } answers {
            rootStack = firstArg<(List<Route>) -> List<Route>>().invoke(rootStack)
            secondArg<(Boolean) -> Unit>().invoke(true)
        }
        coEvery { userWalletsListRepository.selectedUserWalletSync() } returns
            mockk<UserWallet> { every { this@mockk.walletId } returns this@SendIntentModelTest.walletId }
        coEvery { getSendIntentUseCase(INTENT_ID) } returns intent.right()
        coEvery { sourceResolver.resolve(walletId, any()) } returns
            SendIntentSource.Found(accountId = mockk<AccountId>(), status = usdtStatus)
        coEvery { mcpRepository.getAgent(any()) } returns claude
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `GIVEN source resolution in flight WHEN model created THEN nothing is shown yet`() = runTest {
        // Arrange
        val response = CompletableDeferred<SendIntentSource?>()
        coEvery { sourceResolver.resolve(walletId, any()) } coAnswers { response.await() }

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.offer.value).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN open intent the wallet carries WHEN model created THEN offer sheet opens naming the send`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.shownOffer().title).isEqualTo(
            stringReference("mcp.claude.com Agent wants to send 42.00\u00A0USDT"),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN known agent WHEN model created THEN offer shows its verified origin`() = runTest {
        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.shownOffer().agent).isEqualTo(
            SendIntentAgentUM.Verified(iconUrl = claude.iconUrl, label = stringReference(claude.origin.orEmpty())),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN agent unknown to the repository WHEN model created THEN offer shows the unknown agent card`() =
        runTest {
            // Arrange
            coEvery { mcpRepository.getAgent(any()) } returns McpAgent.unknown(clientId = "client")

            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.shownOffer().agent).isEqualTo(
                SendIntentAgentUM.Unknown(iconUrl = null, label = stringReference("Unknown")),
            )
            model.onDestroy()
        }

    @Test
    fun `GIVEN agent lookup that never answers WHEN time passes THEN offer is made for the unknown agent`() = runTest {
        // Arrange
        coEvery { mcpRepository.getAgent(any()) } coAnswers { awaitCancellation() }

        // Act
        val model = createModel(testScope = this)
        advanceTimeBy(AGENT_LOOKUP_TIMEOUT_MILLIS - 1)
        runCurrent()
        val offerBeforeTimeout = model.offer.value
        advanceTimeBy(2)
        runCurrent()

        // Assert
        assertThat(offerBeforeTimeout).isNull()
        assertThat(model.shownOffer().title).isEqualTo(
            stringReference("Unknown Agent wants to send 42.00\u00A0USDT"),
        )
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
        val sendIntentModel = createModel(testScope = this)
        advanceUntilIdle()
        sendIntentModel.onClose()

        // Assert
        verify(exactly = 1) { messageSender.send(any<ToastMessage>()) }
        verify(exactly = 1) { router.pop(any()) }
        assertThat(sendIntentModel.offer.value).isNull()
        verify(exactly = 0) { router.navigate(any(), any()) }
        coVerify(exactly = 0) { mcpRepository.getAgent(any()) }
        sendIntentModel.onDestroy()
    }

    data class CloseModel(val description: String, val arrange: () -> Unit) {
        override fun toString() = description
    }

    @Suppress("unused")
    private fun provideTestModels() = listOf(
        CloseModel(description = "no wallet is selected") {
            coEvery { userWalletsListRepository.selectedUserWalletSync() } returns null
        },
        CloseModel(description = "intent not found") {
            coEvery { getSendIntentUseCase(INTENT_ID) } returns SendIntentError.NotFound(INTENT_ID).left()
        },
        CloseModel(description = "network failure") {
            coEvery { getSendIntentUseCase(INTENT_ID) } returns ApiResponseError.NetworkException().left()
        },
        CloseModel(description = "intent was already carried out") {
            coEvery { getSendIntentUseCase(INTENT_ID) } returns intent.copy(isHandled = true).right()
        },
        CloseModel(description = "wallet does not send from the intent address") {
            coEvery { sourceResolver.resolve(walletId, any()) } returns SendIntentSource.NotFound
        },
        CloseModel(description = "accounts of the wallet cannot be read") {
            coEvery { sourceResolver.resolve(walletId, any()) } returns null
        },
        CloseModel(description = "zero amount") {
            coEvery { getSendIntentUseCase(INTENT_ID) } returns intent.copy(amount = BigDecimal.ZERO).right()
        },
        CloseModel(description = "negative amount") {
            coEvery { getSendIntentUseCase(INTENT_ID) } returns intent.copy(amount = BigDecimal("-1")).right()
        },
        CloseModel(description = "amount would be rounded up") {
            coEvery { getSendIntentUseCase(INTENT_ID) } returns intent.copy(amount = BigDecimal("0.1234565")).right()
        },
        CloseModel(description = "amount would be rounded down") {
            coEvery { getSendIntentUseCase(INTENT_ID) } returns intent.copy(amount = BigDecimal("0.1234564")).right()
        },
        CloseModel(description = "amount would become zero") {
            coEvery { getSendIntentUseCase(INTENT_ID) } returns intent.copy(amount = BigDecimal("0.0000004")).right()
        },
        CloseModel(description = "amount would become a token unit") {
            coEvery { getSendIntentUseCase(INTENT_ID) } returns intent.copy(amount = BigDecimal("0.0000005")).right()
        },
        CloseModel(description = "amount scale overflows") {
            coEvery { getSendIntentUseCase(INTENT_ID) } returns intent.copy(amount = BigDecimal("100E+2147483647")).right()
        },
    )

    @Test
    fun `GIVEN offer WHEN proceed THEN send opens bound to the intent and sheet closes`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val expectedRoute = AppRoute.Send(
            userWalletId = walletId,
            currency = usdt,
            transactionId = null,
            amount = "42",
            tag = "memo-7",
            destinationAddress = intent.toAddress,
            intentId = INTENT_ID,
        )
        val offer = model.shownOffer()

        // Act
        offer.primaryButton.onClick()

        // Assert
        assertThat(rootStack).containsExactly(expectedRoute)
        verify(exactly = 1) { router.pop(any()) }
        verify(exactly = 0) { messageSender.send(any<ToastMessage>()) }
        assertThat(model.offer.value).isEqualTo(offer)
        model.onDestroy()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "another_intent"])
    fun `GIVEN another send on top WHEN proceed THEN it is preserved below the new send`(intentId: String) = runTest {
        // Arrange
        val existing = AppRoute.Send(
            userWalletId = walletId,
            currency = usdt,
            intentId = intentId.takeIf(String::isNotEmpty),
        )
        rootStack = listOf(existing)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.shownOffer().primaryButton.onClick()

        // Assert
        assertThat(rootStack).containsExactly(existing, expectedSendRoute()).inOrder()
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN sell redirect send on top WHEN proceed THEN intent send is pushed without replacing it`() = runTest {
        // Arrange
        val sellRedirect = AppRoute.Send(userWalletId = walletId, currency = usdt, transactionId = "sell-tx")
        rootStack = listOf(sellRedirect)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.shownOffer().primaryButton.onClick()

        // Assert
        assertThat(rootStack).containsExactly(sellRedirect, expectedSendRoute()).inOrder()
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "sell-tx"])
    fun `GIVEN equal send covered by another send WHEN proceed THEN covering send is preserved`(transactionId: String) =
        runTest {
            // Arrange
            val covering = AppRoute.Send(
                userWalletId = walletId,
                currency = usdt,
                transactionId = transactionId.takeIf(String::isNotEmpty),
            )
            rootStack = listOf(expectedSendRoute(), covering)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.shownOffer().primaryButton.onClick()

            // Assert
            assertThat(rootStack).containsExactly(expectedSendRoute(), covering).inOrder()
            verify(exactly = 1) { messageSender.send(any<ToastMessage>()) }
            verify(exactly = 1) { router.pop(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN equal send on top WHEN proceed THEN send is reused without navigation`() = runTest {
        // Arrange
        rootStack = listOf(expectedSendRoute())
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.shownOffer().primaryButton.onClick()
        model.onClose()

        // Assert
        assertThat(rootStack).containsExactly(expectedSendRoute())
        verify(exactly = 0) { messageSender.send(any<ToastMessage>()) }
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN offer WHEN proceed twice THEN send opens once and the queue slot is released once`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val proceed = model.shownOffer().primaryButton.onClick

        // Act
        proceed()
        proceed()
        model.onClose()

        // Assert
        assertThat(rootStack).containsExactly(expectedSendRoute())
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN offer WHEN sheet is dismissed twice THEN the queue slot is released once and nothing else happens`() =
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
            verify(exactly = 0) { router.navigate(any(), any()) }
            verify(exactly = 0) { messageSender.send(any<ToastMessage>()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN offer dismissed WHEN proceed callback fires later THEN send does not open`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val proceed = model.shownOffer().primaryButton.onClick

        // Act
        model.onClose()
        proceed()

        // Assert
        verify(exactly = 0) { router.navigate(any(), any()) }
        verify(exactly = 1) { router.pop(any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN intent still loading WHEN closed and the answer arrives later THEN nothing more happens`() = runTest {
        // Arrange
        val response = CompletableDeferred<Either<Throwable, SendIntent>>()
        coEvery { getSendIntentUseCase(INTENT_ID) } coAnswers { response.await() }
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

    private fun SendIntentModel.shownOffer(): SendIntentUM = checkNotNull(offer.value)

    private fun expectedSendRoute() = sendRouteConverter.convert(
        SendIntentSendRouteConverter.Input(intent = intent, userWalletId = walletId, currency = usdt),
    ).getOrNull() ?: error("Expected a valid send route")

    private fun createModel(testScope: TestScope): SendIntentModel = SendIntentModel(
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        messageSender = messageSender,
        getSendIntentUseCase = getSendIntentUseCase,
        userWalletsListRepository = userWalletsListRepository,
        sourceResolver = sourceResolver,
        mcpRepository = mcpRepository,
        offerConverter = offerConverter,
        sendRouteConverter = sendRouteConverter,
        paramsContainer = MutableParamsContainer(value = SendIntentComponent.Params(intentId = INTENT_ID)),
    )

    private companion object {
        const val INTENT_ID = "my_intent_id123"
        const val AGENT_LOOKUP_TIMEOUT_MILLIS = 3_000L
    }
}
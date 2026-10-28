package com.tangem.features.send.mcp

import android.os.SystemClock
import arrow.core.Either
import arrow.core.right
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.message.ToastMessage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.mcp.models.McpAgent
import com.tangem.domain.mcp.repository.McpRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.GetSendIntentUseCase
import com.tangem.features.send.api.mcp.SendIntentComponent
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeExtraInfo
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.loadedStatus
import com.tangem.features.send.mcp.model.SendIntentModel
import com.tangem.features.send.mcp.model.SendIntentOfferConverter
import com.tangem.features.send.send.SendModelTestBase
import com.tangem.features.send.send.confirm.model.SendConfirmModel
import com.tangem.features.send.send.ui.state.SendUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class SendIntentNavigationTest : SendModelTestBase() {

    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val getSendIntentUseCase: GetSendIntentUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val sourceResolver: SendIntentSourceResolver = mockk()
    private val mcpRepository: McpRepository = mockk()
    private val lifecycle = LifecycleRegistry()
    private val currency = createToken()
    private val status = loadedStatus(currency)
    private val intent = createSendIntent()
    private val onSendTransaction: () -> Unit = mockk(relaxed = true)

    private val amount: AmountState.Data = mockk(relaxed = true) {
        every { amountTextField.cryptoAmount.value } returns BigDecimal.ONE
        every { reduceAmountBy } returns BigDecimal.ZERO
    }
    private val destination: DestinationUM.Content = mockk(relaxed = true) {
        every { addressTextField.actualAddress } returns "recipient"
        every { memoTextField } returns null
    }
    private val feeSelector: FeeSelectorUM.Content = mockk(relaxed = true) {
        every { selectedFeeItem } returns FeeItem.Market(
            Fee.Common(Amount(currencySymbol = "ETH", value = BigDecimal("0.001"), decimals = 18)),
        )
        every { feeNonce } returns FeeNonce.None
        every { feeExtraInfo } returns mockk<FeeExtraInfo>(relaxed = true) {
            every { transactionFeeExtended } returns null
        }
    }

    @BeforeEach
    fun prepareIntent() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0L
        coEvery { userWalletsListRepository.selectedUserWalletSync() } returns testUserWallet
        every { testUserWallet.walletId } returns testUserWalletId
        coEvery { getSendIntentUseCase(intent.id) } returns intent.right()
        coEvery { sourceResolver.resolve(testUserWalletId, intent) } returns
            SendIntentSource.Found(accountId = mockk<AccountId>(), status = status)
        coEvery { mcpRepository.getAgent(any()) } returns McpAgent.unknown("client")
    }

    @AfterEach
    fun releaseContext() {
        lifecycle.destroy()
        unmockkStatic(SystemClock::class)
    }

    @ParameterizedTest
    @CsvSource("true, false", "false, false", "true, true", "false, true")
    fun `GIVEN hot or post NFC send broadcasting WHEN intent proceeds THEN original send finishes without cancellation`(
        isHotWallet: Boolean,
        isDuplicateBelow: Boolean,
    ) = runTest {
        // Arrange
        val response = CompletableDeferred<Either<SendTransactionError, String>>()
        lateinit var broadcastJob: Job
        coEvery { sendTransactionUseCase(any(), any(), any()) } coAnswers {
            broadcastJob = currentCoroutineContext().job
            response.await()
        }
        val wallet: UserWallet = if (isHotWallet) mockk<UserWallet.Hot>(relaxed = true) else {
            mockk<UserWallet.Cold>(relaxed = true)
        }
        val confirmParams = defaultSendConfirmParams(
            state = SendUM(
                amountUM = amount,
                destinationUM = destination,
                feeSelectorUM = feeSelector,
                confirmUM = mockk<ConfirmUM.Content>(relaxed = true),
                confirmData = null,
            ),
            cryptoCurrencyStatus = status,
            feeCryptoCurrencyStatus = status,
        ).copy(userWallet = wallet, onSendTransaction = onSendTransaction)
        val newRoute = checkNotNull(
            SendIntentSendRouteConverter().convert(
                SendIntentSendRouteConverter.Input(intent, testUserWalletId, currency),
            ).getOrNull(),
        )
        val existingRoute = AppRoute.Send(testUserWalletId, currency, intentId = "in-flight")
        val initialRoutes = if (isDuplicateBelow) listOf(AppRoute.Wallet, newRoute, existingRoute) else {
            listOf(AppRoute.Wallet, existingRoute)
        }
        val navigation = StackNavigation<AppRoute>()
        lateinit var sendingModel: SendConfirmModel
        val context = DefaultComponentContext(lifecycle)
        val stack = context.childStack(
            source = navigation,
            serializer = null,
            initialStack = { initialRoutes },
            childFactory = { route, childContext ->
                if (route == existingRoute) {
                    childContext.instanceKeeper.getOrCreate("confirm") {
                        createSendConfirmModel(this, MutableParamsContainer(confirmParams))
                    }.also { sendingModel = it }
                } else {
                    Unit
                }
            },
        )
        every { router.navigate(any(), any()) } answers {
            val transformer = firstArg<(List<Route>) -> List<Route>>()
            val complete = secondArg<(Boolean) -> Unit>()
            navigation.navigate(
                transformer = { routes -> transformer(routes).map { it as AppRoute } },
                onComplete = { _, _ -> complete(true) },
            )
        }
        lifecycle.resume()
        val model = SendIntentModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            router = router,
            messageSender = messageSender,
            getSendIntentUseCase = getSendIntentUseCase,
            userWalletsListRepository = userWalletsListRepository,
            sourceResolver = sourceResolver,
            mcpRepository = mcpRepository,
            offerConverter = SendIntentOfferConverter(),
            sendRouteConverter = SendIntentSendRouteConverter(),
            paramsContainer = MutableParamsContainer(SendIntentComponent.Params(intent.id)),
        )
        advanceUntilIdle()
        sendingModel.onSendClick()
        advanceUntilIdle()
        coVerify(exactly = 1) { sendTransactionUseCase(any(), wallet, any()) }

        // Act
        checkNotNull(model.offer.value).primaryButton.onClick()
        advanceUntilIdle()
        val wasBroadcastActive = broadcastJob.isActive
        response.complete("tx-hash".right())
        advanceUntilIdle()

        // Assert
        val expectedRoutes = if (isDuplicateBelow) initialRoutes else initialRoutes + newRoute
        assertThat(stack.value.items.map { it.configuration }).containsExactlyElementsIn(expectedRoutes).inOrder()
        assertThat(wasBroadcastActive).isTrue()
        verify(exactly = 1) { onSendTransaction() }
        verify(exactly = 1) { router.pop(any()) }
        verify(exactly = if (isDuplicateBelow) 1 else 0) { messageSender.send(any<ToastMessage>()) }
        assertThat(sendingModel.uiState.value.confirmUM).isInstanceOf(ConfirmUM.Success::class.java)
        model.onDestroy()
    }
}
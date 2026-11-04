package com.tangem.features.moneyaccount.details

import arrow.core.left
import arrow.core.right
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.models.ReceiveAddressModel
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents
import com.tangem.domain.moneyaccount.analytics.MoneyAccountAnalyticsEvents.BalanceBand
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBankCredentialsUseCase
import com.tangem.domain.moneyaccount.usecase.RenewTangemPaySessionUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.visa.model.TangemPayTxHistoryItem
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.features.moneyaccount.details.component.MoneyAccountDetailsComponent
import com.tangem.features.moneyaccount.details.impl.R
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.pay.MockBankCredentials
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MoneyAccountDetailsModelTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val token: CryptoCurrency.Token = mockk {
        every { id } returns mockk { every { value } returns "token/USDC" }
    }

    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier = mockk()
    private val router: Router = mockk(relaxed = true)
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val getExploreUrlUseCase: GetExploreUrlUseCase = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val getMoneyAccountBankCredentialsUseCase: GetMoneyAccountBankCredentialsUseCase = mockk()
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher = mockk()
    private val txHistoryContentUpdateEmitter: TxHistoryContentUpdateEmitter = mockk(relaxed = true)
    private val renewTangemPaySessionUseCase: RenewTangemPaySessionUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val isBalanceHiddenFlow = MutableStateFlow(false)

    @BeforeEach
    fun resetMocks() {
        clearMocks(moneyAccountStatusSupplier, router, sendFeedbackEmailUseCase, urlOpener, analytics)
        clearMocks(getMoneyAccountBankCredentialsUseCase, moneyAccountStatusFetcher, txHistoryContentUpdateEmitter)
        clearMocks(renewTangemPaySessionUseCase, uiMessageSender, getBalanceHidingSettingsUseCase)
        isBalanceHiddenFlow.value = false
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns isBalanceHiddenFlow
    }

    @Test
    fun `GIVEN balance hidden WHEN model created THEN state is hidden`() = runTest {
        // Arrange
        isBalanceHiddenFlow.value = true

        // Act
        val model = createModel(activeAccount(balance = balance()))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.isBalanceHidden).isTrue()
        model.onDestroy()
    }

    @Test
    fun `GIVEN screen opened WHEN balance hiding toggled THEN state follows it`() = runTest {
        // Arrange
        val model = createModel(activeAccount(balance = balance()))
        advanceUntilIdle()
        val states = mutableListOf(model.uiState.value.isBalanceHidden)

        // Act
        isBalanceHiddenFlow.value = true
        advanceUntilIdle()
        states += model.uiState.value.isBalanceHidden
        isBalanceHiddenFlow.value = false
        advanceUntilIdle()
        states += model.uiState.value.isBalanceHidden

        // Assert
        assertThat(states).containsExactly(false, true, false).inOrder()
        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN active account WHEN add funds clicked THEN options sheet opens with its deposit address`(
        model: AddFundsModel,
    ) = runTest {
        // Arrange
        val detailsModel = createModel(activeAccount(balance = model.balance))
        val openedSheets = detailsModel.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()

        // Act
        detailsModel.uiState.value.onAddFundsClick()

        // Assert
        assertThat(openedSheets).containsExactly(
            MoneyAccountDetailsNavigation.AddFundsOptions(depositAddress = model.expectedDepositAddress),
        )
        verify(exactly = 1) { analytics.send(ofType<MoneyAccountAnalyticsEvents.AddFundsClicked>()) }
        detailsModel.onDestroy()
    }

    @Test
    fun `GIVEN options sheet WHEN receive clicked THEN options dismissed and receive sheet opens`() = runTest {
        // Arrange
        val model = createModel(activeAccount(balance = balance()))
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()
        model.uiState.value.onAddFundsClick()

        // Act
        model.onReceiveClick(DEPOSIT_ADDRESS)

        // Assert
        assertThat(openedSheets).containsExactly(
            MoneyAccountDetailsNavigation.AddFundsOptions(depositAddress = DEPOSIT_ADDRESS),
            null,
            MoneyAccountDetailsNavigation.Receive(receiveConfig()),
        ).inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN options sheet WHEN dismissed THEN slot cleared`() = runTest {
        // Arrange
        val model = createModel(activeAccount(balance = balance()))
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()
        model.uiState.value.onAddFundsClick()

        // Act
        model.onDismissAddFunds()

        // Assert
        assertThat(openedSheets.last()).isNull()
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideSheetsOpenedFromOptions")
    fun `GIVEN sheet opened from options WHEN options dismissal arrives late THEN that sheet stays`(
        model: SheetFromOptionsModel,
    ) = runTest {
        // Arrange
        val detailsModel = createModel(activeAccount(balance = balance()))
        val openedSheets = detailsModel.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()
        detailsModel.uiState.value.onAddFundsClick()
        model.openSheet(detailsModel)

        // Act
        detailsModel.onDismissAddFunds()

        // Assert
        assertThat(openedSheets.last()).isEqualTo(model.expectedSheet)
        detailsModel.onDestroy()
    }

    @Test
    fun `GIVEN banking details fail to load WHEN notified THEN options replaced by error sheet and event sent`() =
        runTest {
            // Arrange
            val model = createModel(activeAccount(balance = balance()))
            val openedSheets = model.bottomSheetNavigation.trackSlot()
            advanceUntilIdle()
            model.uiState.value.onAddFundsClick()

            // Act
            model.onBankingDetailsLoadFailed()

            // Assert
            assertThat(openedSheets.last()).isEqualTo(MoneyAccountDetailsNavigation.BankDetailsError)
            verify(exactly = 1) { analytics.send(ofType<MoneyAccountAnalyticsEvents.DetailsLoadFailed>()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN error sheet WHEN retried THEN credentials requested again and retry event sent`() = runTest {
        // Arrange
        coEvery { getMoneyAccountBankCredentialsUseCase(userWalletId) } returns CREDENTIALS.right()
        val model = createModel(activeAccount(balance = balance()))
        advanceUntilIdle()

        // Act
        val actual = model.retryBankCredentials()

        // Assert
        assertThat(actual).isEqualTo(CREDENTIALS.right())
        verify(exactly = 1) { analytics.send(ofType<MoneyAccountAnalyticsEvents.DetailsRetryTapped>()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN bank details loaded WHEN reported THEN sheet replaced by details screen and bank details event sent`() =
        runTest {
            // Arrange
            val model = createModel(activeAccount(balance = balance()))
            val openedSheets = model.bottomSheetNavigation.trackSlot()
            advanceUntilIdle()
            model.onBankingDetailsLoadFailed()

            // Act
            model.onBankDetailsLoaded(CREDENTIALS)

            // Assert
            assertThat(openedSheets.last()).isNull()
            verify(exactly = 1) { router.push(MoneyAccountDetailsRoute.BankDetails(CREDENTIALS), any()) }
            verify(exactly = 1) { analytics.send(MoneyAccountAnalyticsEvents.AddFundsBankDetailsShown()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN error sheet WHEN support clicked THEN sheet dismissed and support email sent with customer id`() =
        runTest {
            // Arrange
            val model = createModel(activeAccount(balance = balance()))
            val openedSheets = model.bottomSheetNavigation.trackSlot()
            advanceUntilIdle()
            model.onBankingDetailsLoadFailed()

            // Act
            model.onBankDetailsSupportClick()
            advanceUntilIdle()

            // Assert
            assertThat(openedSheets.last()).isNull()
            verify(exactly = 1) { analytics.send(ofType<MoneyAccountAnalyticsEvents.DetailsErrorSupportTapped>()) }
            verify(exactly = 0) { analytics.send(ofType<MoneyAccountAnalyticsEvents.SupportClicked>()) }
            coVerify(exactly = 1) { sendFeedbackEmailUseCase(supportEmailType()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN account menu WHEN support clicked THEN support event sent and support email sent with customer id`() =
        runTest {
            // Arrange
            val model = createModel(activeAccount(balance = balance()))
            advanceUntilIdle()

            // Act
            model.menuItem(resourceReference(R.string.tangempay_pay_support)).onClick()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { analytics.send(MoneyAccountAnalyticsEvents.SupportClicked()) }
            coVerify(exactly = 1) { sendFeedbackEmailUseCase(supportEmailType()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN account menu WHEN terms clicked THEN account terms event sent and terms opened`() = runTest {
        // Arrange
        val model = createModel(activeAccount(balance = balance()))
        advanceUntilIdle()

        // Act
        model.menuItem(stringReference("Terms and Limits")).onClick()

        // Assert
        verify(exactly = 1) {
            analytics.send(
                TangemPayAnalyticsEvents.TermsAndLimitsClicked(product = TangemPayAnalyticsEvents.Product.ACCOUNT),
            )
        }
        verify(exactly = 1) { urlOpener.openUrl(TERMS_URL) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN account screen WHEN menu opened THEN account menu event sent`() = runTest {
        // Arrange
        val model = createModel(activeAccount(balance = balance()))
        advanceUntilIdle()

        // Act
        model.uiState.value.onMenuClick()

        // Assert
        verify(exactly = 1) { analytics.send(MoneyAccountAnalyticsEvents.AccountMenuClicked()) }
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideScreenOpenedModels")
    fun `GIVEN account WHEN first loaded THEN screen opened event sent with its state and balance band`(
        model: ScreenOpenedModel,
    ) = runTest {
        // Act
        val detailsModel = createModel(model.status)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analytics.send(model.expectedEvent) }
        detailsModel.onDestroy()
    }

    @Test
    fun `GIVEN account shown WHEN account updated THEN screen opened event not repeated`() = runTest {
        // Arrange
        val statuses = MutableStateFlow<MoneyAccountStatusValue>(activeAccount(balance = balance()))
        val model = createModel(statuses)
        advanceUntilIdle()

        // Act
        statuses.value = activeAccount(balance = balance(amount = BigDecimal.ZERO))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analytics.send(ofType<MoneyAccountAnalyticsEvents.AccountScreenOpened>()) }
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideBalanceBannerModels")
    fun `GIVEN account updates WHEN balance becomes unavailable THEN banner event sent once per appearance`(
        model: BalanceBannerModel,
    ) = runTest {
        // Act
        val detailsModel = createModel(model.balances.map { activeAccount(balance = it) }.asFlow())
        advanceUntilIdle()

        // Assert
        verify(exactly = model.expectedEvents) {
            analytics.send(ofType<MoneyAccountAnalyticsEvents.BalanceErrorBannerShown>())
        }
        detailsModel.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideTransactionClickModels")
    fun `GIVEN history row WHEN clicked THEN account transaction event sent with its type and status`(
        model: TransactionClickModel,
    ) = runTest {
        // Arrange
        val detailsModel = createModel(activeAccount(balance = balance()))
        advanceUntilIdle()

        // Act
        detailsModel.onTransactionClick(model.item)

        // Assert
        verify(exactly = 1) {
            analytics.send(
                TangemPayAnalyticsEvents.TransactionInListClicked(
                    product = TangemPayAnalyticsEvents.Product.ACCOUNT,
                    type = model.expectedType,
                    status = model.expectedStatus,
                ),
            )
        }
        detailsModel.onDestroy()
    }

    @Test
    fun `GIVEN options sheet WHEN swap clicked THEN swap opens with the account token as TO`() = runTest {
        // Arrange
        val model = createModel(activeAccount(balance = balance()))
        val openedSheets = model.bottomSheetNavigation.trackSlot()
        advanceUntilIdle()
        model.uiState.value.onAddFundsClick()

        // Act
        model.onSwapClick()

        // Assert
        assertThat(openedSheets.last()).isNull()
        val expectedRoute = AppRoute.Swap(
            userWalletId = userWalletId,
            fromCryptoCurrency = token,
            screenSource = "MoneyAccount",
            fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.TO,
            accountFlow = AppRoute.Swap.AccountFlow.MoneyTopUp,
        )
        verify(exactly = 1) { router.push(expectedRoute, any()) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN account without balance WHEN swap clicked THEN swap not opened`() = runTest {
        // Arrange
        val model = createModel(activeAccount(balance = null))
        advanceUntilIdle()

        // Act
        model.onSwapClick()

        // Assert
        verify(exactly = 0) { router.push(any(), any()) }
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideSendClickModels")
    fun `GIVEN account WHEN send clicked THEN withdraw hub offers bank transfer only with the bank off-ramp channel`(
        model: SendClickModel,
    ) = runTest {
        // Arrange
        val pushedRoutes = mutableListOf<Route>()
        every { router.push(capture(pushedRoutes), any()) } returns Unit
        val detailsModel = createModel(model.status)
        advanceUntilIdle()

        // Act
        detailsModel.uiState.value.onSendClick()

        // Assert
        assertThat(pushedRoutes).containsExactlyElementsIn(listOfNotNull(model.expectedRoute))
        detailsModel.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideSendClickModels")
    fun `GIVEN account WHEN send clicked THEN send button tap reported only when the hub opens`(
        model: SendClickModel,
    ) = runTest {
        // Arrange
        val detailsModel = createModel(model.status)
        advanceUntilIdle()

        // Act
        detailsModel.uiState.value.onSendClick()

        // Assert
        val expectedTaps = if (model.expectedRoute == null) 0 else 1
        verify(exactly = expectedTaps) { analytics.send(MoneyAccountAnalyticsEvents.SendClicked()) }
        detailsModel.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideNonAccountStatuses")
    fun `GIVEN account shown WHEN its status stops being an account THEN screen stays open`(
        status: MoneyAccountStatusValue,
    ) = runTest {
        // Arrange
        val statuses = MutableStateFlow<MoneyAccountStatusValue>(activeAccount(balance = balance()))
        val model = createModel(statuses)
        advanceUntilIdle()

        // Act
        statuses.value = status
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { router.pop(any()) }
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideNonAccountStatuses")
    fun `GIVEN no account shown WHEN status is not an account THEN screen closed`(status: MoneyAccountStatusValue) =
        runTest {
            // Act
            val model = createModel(status)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { router.pop(any()) }
            model.onDestroy()
        }

    @Test
    fun `GIVEN account status WHEN model created THEN tx history currency is the account token`() = runTest {
        // Act
        val model = createModel(activeAccount(balance = null))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.txHistoryCurrency).isEqualTo(token)
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideNonAccountStatuses")
    fun `GIVEN non account status WHEN model created THEN tx history currency is absent`(
        status: MoneyAccountStatusValue,
    ) = runTest {
        // Act
        val model = createModel(status)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.txHistoryCurrency).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN account shown WHEN pulled to refresh THEN status fetched tx history reloaded and indicator hidden`() =
        runTest {
            // Arrange
            coEvery { moneyAccountStatusFetcher(userWalletId) } returns Unit.right()
            val model = createModel(activeAccount(balance = balance()))
            advanceUntilIdle()

            // Act
            model.pullToRefresh()
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { moneyAccountStatusFetcher(userWalletId) }
            coVerify(exactly = 1) { txHistoryContentUpdateEmitter.triggerUpdate() }
            assertThat(model.uiState.value.pullToRefreshConfig.isRefreshing).isFalse()
            model.onDestroy()
        }

    @Test
    fun `GIVEN status fetch in progress WHEN account status updated THEN refresh indicator stays`() = runTest {
        // Arrange
        coEvery { moneyAccountStatusFetcher(userWalletId) } coAnswers { awaitCancellation() }
        val statuses = MutableStateFlow<MoneyAccountStatusValue>(activeAccount(balance = null))
        val model = createModel(statuses)
        advanceUntilIdle()
        model.pullToRefresh()

        // Act
        statuses.value = activeAccount(balance = balance())
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.pullToRefreshConfig.isRefreshing).isTrue()
        assertThat(model.uiState.value.balance).isInstanceOf(MoneyAccountBalanceBlockState.Content::class.java)
        model.onDestroy()
    }

    @Test
    fun `GIVEN refresh in progress WHEN pulled again THEN status fetched once`() = runTest {
        // Arrange
        coEvery { moneyAccountStatusFetcher(userWalletId) } coAnswers { awaitCancellation() }
        val model = createModel(activeAccount(balance = balance()))
        advanceUntilIdle()
        model.pullToRefresh()
        advanceUntilIdle()

        // Act
        model.pullToRefresh()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { moneyAccountStatusFetcher(userWalletId) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN account not loaded WHEN pulled to refresh THEN status fetched without tx history reload`() = runTest {
        // Arrange
        coEvery { moneyAccountStatusFetcher(userWalletId) } returns Unit.right()
        val model = createModel(emptyFlow())
        advanceUntilIdle()

        // Act
        model.pullToRefresh()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { moneyAccountStatusFetcher(userWalletId) }
        coVerify(exactly = 0) { txHistoryContentUpdateEmitter.triggerUpdate() }
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideErrorBannerModels")
    fun `GIVEN account error and balance WHEN model created THEN matching banner shown and actions toggled`(
        model: ErrorBannerModel,
    ) = runTest {
        // Arrange
        val detailsModel = createModel(activeAccount(balance = model.balance, error = model.error))

        // Act
        advanceUntilIdle()

        // Assert
        val banner = detailsModel.uiState.value.messageBanner
        assertThat(banner?.title).isEqualTo(model.expectedTitle)
        assertThat(banner?.secondaryButton != null).isEqualTo(model.hasRenewButton)
        assertThat(detailsModel.uiState.value.isAddFundsEnabled).isEqualTo(model.isAddFundsEnabled)
        assertThat(detailsModel.uiState.value.isSendEnabled).isEqualTo(model.isSendEnabled)
        detailsModel.onDestroy()
    }

    @Test
    fun `GIVEN not synced banner WHEN renew session succeeds THEN status fetched and progress hidden`() = runTest {
        // Arrange
        coEvery { renewTangemPaySessionUseCase(userWalletId) } returns Unit.right()
        coEvery { moneyAccountStatusFetcher(userWalletId) } returns Unit.right()
        val model = createModel(activeAccount(balance = balance(), error = MoneyAccountStatusValue.Error.NotSynced))
        advanceUntilIdle()

        // Act
        model.renewSession()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { moneyAccountStatusFetcher(userWalletId) }
        verify(exactly = 0) { uiMessageSender.send(any()) }
        assertThat(model.uiState.value.messageBanner?.secondaryButton?.isLoading).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN not synced banner WHEN renew session fails THEN error shown and status not fetched`() = runTest {
        // Arrange
        coEvery { renewTangemPaySessionUseCase(userWalletId) } returns IllegalStateException().left()
        val model = createModel(activeAccount(balance = balance(), error = MoneyAccountStatusValue.Error.NotSynced))
        advanceUntilIdle()

        // Act
        model.renewSession()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { moneyAccountStatusFetcher(any<UserWalletId>()) }
        verify(exactly = 1) { uiMessageSender.send(match<SnackbarMessage> { true }) }
        assertThat(model.uiState.value.messageBanner?.secondaryButton?.isLoading).isFalse()
        model.onDestroy()
    }

    private fun MoneyAccountDetailsModel.renewSession() {
        requireNotNull(uiState.value.messageBanner?.secondaryButton).onClick()
    }

    private fun MoneyAccountDetailsModel.pullToRefresh() {
        uiState.value.pullToRefreshConfig.onRefresh(PullToRefreshConfig.ShowRefreshState())
    }

    private fun TestScope.createModel(status: MoneyAccountStatusValue) = createModel(flowOf(status))

    private fun TestScope.createModel(statuses: Flow<MoneyAccountStatusValue>): MoneyAccountDetailsModel {
        every { moneyAccountStatusSupplier(userWalletId) } returns statuses.map { status ->
            AccountStatus.Money(account = Account.Money(userWalletId = userWalletId), value = status)
        }
        return MoneyAccountDetailsModel(
            paramsContainer = MutableParamsContainer(MoneyAccountDetailsComponent.Params(userWalletId = userWalletId)),
            moneyAccountStatusSupplier = moneyAccountStatusSupplier,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            dispatchers = createTestingCoroutineDispatcherProvider(),
            router = router,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            urlOpener = urlOpener,
            getExploreUrlUseCase = getExploreUrlUseCase,
            analytics = analytics,
            getMoneyAccountBankCredentialsUseCase = getMoneyAccountBankCredentialsUseCase,
            moneyAccountStatusFetcher = moneyAccountStatusFetcher,
            txHistoryContentUpdateEmitter = txHistoryContentUpdateEmitter,
            renewTangemPaySessionUseCase = renewTangemPaySessionUseCase,
            uiMessageSender = uiMessageSender,
        )
    }

    private fun provideErrorBannerModels() = listOf(
        ErrorBannerModel(
            balance = balance(),
            error = null,
            expectedTitle = null,
            hasRenewButton = false,
            isAddFundsEnabled = true,
            isSendEnabled = true,
        ),
        ErrorBannerModel(
            balance = null,
            error = null,
            expectedTitle = resourceReference(R.string.money_account_balance_unavailable_title),
            hasRenewButton = false,
            isAddFundsEnabled = false,
            isSendEnabled = false,
        ),
        ErrorBannerModel(
            balance = balance(),
            error = MoneyAccountStatusValue.Error.NotSynced,
            expectedTitle = resourceReference(R.string.tangempay_sync_needed_title),
            hasRenewButton = true,
            isAddFundsEnabled = false,
            isSendEnabled = false,
        ),
        ErrorBannerModel(
            balance = null,
            error = MoneyAccountStatusValue.Error.NotSynced,
            expectedTitle = resourceReference(R.string.tangempay_sync_needed_title),
            hasRenewButton = true,
            isAddFundsEnabled = false,
            isSendEnabled = false,
        ),
        ErrorBannerModel(
            balance = balance(),
            error = MoneyAccountStatusValue.Error.Unavailable,
            expectedTitle = resourceReference(R.string.tangempay_temporarily_unavailable),
            hasRenewButton = false,
            isAddFundsEnabled = false,
            isSendEnabled = false,
        ),
    )

    data class ErrorBannerModel(
        val balance: MoneyAccountStatusValue.Balance?,
        val error: MoneyAccountStatusValue.Error?,
        val expectedTitle: TextReference?,
        val hasRenewButton: Boolean,
        val isAddFundsEnabled: Boolean,
        val isSendEnabled: Boolean,
    )

    data class AddFundsModel(val balance: MoneyAccountStatusValue.Balance?, val expectedDepositAddress: String?)

    private fun provideSheetsOpenedFromOptions() = listOf(
        SheetFromOptionsModel(
            openSheet = { model -> model.onReceiveClick(DEPOSIT_ADDRESS) },
            expectedSheet = MoneyAccountDetailsNavigation.Receive(receiveConfig()),
        ),
        SheetFromOptionsModel(
            openSheet = { model -> model.onBankingDetailsLoadFailed() },
            expectedSheet = MoneyAccountDetailsNavigation.BankDetailsError,
        ),
    )

    data class SheetFromOptionsModel(
        val openSheet: (MoneyAccountDetailsModel) -> Unit,
        val expectedSheet: MoneyAccountDetailsNavigation,
    )

    private fun provideSendClickModels() = listOf(
        SendClickModel(
            status = activeAccount(balance(), isBankOfframpAvailable = true),
            expectedRoute = MoneyAccountDetailsRoute.WithdrawHub(productInstanceId = PRODUCT_INSTANCE_ID),
        ),
        SendClickModel(
            status = activeAccount(balance(), isBankOfframpAvailable = false),
            expectedRoute = MoneyAccountDetailsRoute.WithdrawHub(productInstanceId = null),
        ),
        SendClickModel(
            status = activeAccount(balance(), isBankOfframpAvailable = null),
            expectedRoute = MoneyAccountDetailsRoute.WithdrawHub(productInstanceId = null),
        ),
        SendClickModel(
            status = activeAccount(balance = null, isBankOfframpAvailable = true),
        ),
    )

    data class SendClickModel(
        val status: MoneyAccountStatusValue.Account,
        val expectedRoute: MoneyAccountDetailsRoute? = null,
    )

    private fun provideScreenOpenedModels() = listOf(
        activeScreenOpenedModel(amount = BigDecimal.ZERO, expectedBand = BalanceBand.ZERO),
        activeScreenOpenedModel(amount = BigDecimal("99.99"), expectedBand = BalanceBand.UNDER_100),
        activeScreenOpenedModel(amount = BigDecimal("100"), expectedBand = BalanceBand.FROM_100_TO_1K),
        activeScreenOpenedModel(amount = BigDecimal("1000"), expectedBand = BalanceBand.FROM_1K_TO_10K),
        activeScreenOpenedModel(amount = BigDecimal("10000"), expectedBand = BalanceBand.FROM_1K_TO_10K),
        activeScreenOpenedModel(amount = BigDecimal("25250.50"), expectedBand = BalanceBand.OVER_10K),
        ScreenOpenedModel(
            status = activeAccount(balance = null),
            expectedEvent = MoneyAccountAnalyticsEvents.AccountScreenOpened(
                accountState = MoneyAccountAnalyticsEvents.AccountState.ACTIVE,
                balanceBand = null,
                currency = null,
            ),
        ),
    )

    data class ScreenOpenedModel(
        val status: MoneyAccountStatusValue.Account,
        val expectedEvent: MoneyAccountAnalyticsEvents.AccountScreenOpened,
    )

    private fun activeScreenOpenedModel(amount: BigDecimal, expectedBand: BalanceBand) = ScreenOpenedModel(
        status = activeAccount(balance = balance(amount = amount)),
        expectedEvent = MoneyAccountAnalyticsEvents.AccountScreenOpened(
            accountState = MoneyAccountAnalyticsEvents.AccountState.ACTIVE,
            balanceBand = expectedBand,
            currency = "USD",
        ),
    )

    private fun provideBalanceBannerModels() = listOf(
        BalanceBannerModel(balances = listOf(null), expectedEvents = 1),
        BalanceBannerModel(balances = listOf(balance()), expectedEvents = 0),
        BalanceBannerModel(balances = listOf(balance(), null), expectedEvents = 1),
        BalanceBannerModel(balances = listOf(null, null), expectedEvents = 1),
        BalanceBannerModel(balances = listOf(null, balance(), null), expectedEvents = 2),
    )

    data class BalanceBannerModel(val balances: List<MoneyAccountStatusValue.Balance?>, val expectedEvents: Int)

    private fun provideTransactionClickModels() = listOf(
        TransactionClickModel(
            item = OnChainTx.BSDK(txInfo(isOutgoing = false, status = TxInfo.TransactionStatus.Confirmed)),
            expectedType = "deposit",
            expectedStatus = "completed",
        ),
        TransactionClickModel(
            item = OnChainTx.BSDK(txInfo(isOutgoing = true, status = TxInfo.TransactionStatus.Unconfirmed)),
            expectedType = "send",
            expectedStatus = "pending",
        ),
        TransactionClickModel(
            item = OnChainTx.BSDK(txInfo(isOutgoing = true, status = TxInfo.TransactionStatus.Failed)),
            expectedType = "send",
            expectedStatus = "declined",
        ),
        TransactionClickModel(
            item = ExpressTx.Swap(tx = mockk(), isOutgoing = true, txInfo = null),
            expectedType = "send",
            expectedStatus = "unknown",
        ),
        TransactionClickModel(
            item = ExpressTx.Onramp(
                tx = mockk(),
                txInfo = OnChainTx.BSDK(txInfo(isOutgoing = false, status = TxInfo.TransactionStatus.Confirmed)),
            ),
            expectedType = "deposit",
            expectedStatus = "completed",
        ),
        TransactionClickModel(
            item = JointOperationTx(operation = mockk(), transfer = mockk(), txInfo = null),
            expectedType = "send",
            expectedStatus = "unknown",
        ),
        collateralClick(collateralType = TangemPayTxHistoryItem.Type.Deposit, expectedType = "deposit"),
        collateralClick(collateralType = TangemPayTxHistoryItem.Type.Cashback, expectedType = "deposit"),
        collateralClick(collateralType = TangemPayTxHistoryItem.Type.Withdrawal, expectedType = "send"),
        spendClick(spendStatus = TangemPayTxHistoryItem.Status.PENDING, expectedStatus = "pending"),
        spendClick(spendStatus = TangemPayTxHistoryItem.Status.RESERVED, expectedStatus = "pending"),
        spendClick(spendStatus = TangemPayTxHistoryItem.Status.COMPLETED, expectedStatus = "completed"),
        spendClick(spendStatus = TangemPayTxHistoryItem.Status.DECLINED, expectedStatus = "declined"),
        spendClick(spendStatus = TangemPayTxHistoryItem.Status.REVERSED, expectedStatus = "declined"),
        spendClick(spendStatus = TangemPayTxHistoryItem.Status.UNKNOWN, expectedStatus = "unknown"),
        TransactionClickModel(
            item = OnChainTx.TangemPay(mockk<TangemPayTxHistoryItem.Payment>()),
            expectedType = "send",
            expectedStatus = "unknown",
        ),
        TransactionClickModel(
            item = OnChainTx.TangemPay(mockk<TangemPayTxHistoryItem.Fee>()),
            expectedType = "send",
            expectedStatus = "unknown",
        ),
    )

    private fun collateralClick(collateralType: TangemPayTxHistoryItem.Type, expectedType: String) =
        TransactionClickModel(
            item = OnChainTx.TangemPay(mockk<TangemPayTxHistoryItem.Collateral> { every { type } returns collateralType }),
            expectedType = expectedType,
            expectedStatus = "unknown",
        )

    private fun spendClick(spendStatus: TangemPayTxHistoryItem.Status, expectedStatus: String) = TransactionClickModel(
        item = OnChainTx.TangemPay(mockk<TangemPayTxHistoryItem.Spend> { every { status } returns spendStatus }),
        expectedType = "send",
        expectedStatus = expectedStatus,
    )

    data class TransactionClickModel(val item: TxHistoryInfo, val expectedType: String, val expectedStatus: String)

    private fun provideNonAccountStatuses() = listOf(
        MoneyAccountStatusValue.Error.Unavailable,
        MoneyAccountStatusValue.Error.ExposedDevice,
        MoneyAccountStatusValue.Empty,
        MoneyAccountStatusValue.Onboarding.Activating(
            source = StatusSource.ACTUAL,
            customerId = CUSTOMER_ID,
            step = MoneyAccountStatusValue.Onboarding.Activating.Step.ISSUING,
        ),
    )

    private fun activeAccount(
        balance: MoneyAccountStatusValue.Balance?,
        isBankOfframpAvailable: Boolean? = null,
        error: MoneyAccountStatusValue.Error? = null,
    ) = MoneyAccountStatusValue.Account.Active(
        source = StatusSource.ACTUAL,
        cryptoCurrency = token,
        balance = balance,
        customerId = CUSTOMER_ID,
        productInstanceId = PRODUCT_INSTANCE_ID,
        isBankOfframpAvailable = isBankOfframpAvailable,
        error = error,
    )

    private fun balance(amount: BigDecimal = BigDecimal("25250.50")) = MoneyAccountStatusValue.Balance(
        cryptoAmount = amount,
        depositAddress = DEPOSIT_ADDRESS,
        currencyCode = "USD",
        fiatRate = BigDecimal.ONE,
    )

    private fun receiveConfig() = TokenReceiveConfig(
        shouldShowWarning = true,
        cryptoCurrency = token,
        userWalletId = userWalletId,
        shouldShowMemoDisclaimer = false,
        receiveAddress = listOf(
            ReceiveAddressModel(displayType = ReceiveAddressModel.DisplayType.Default, value = DEPOSIT_ADDRESS),
        ),
    )

    @Suppress("Unused") // need for ParameterizedTest 
    private fun provideTestModels() = listOf(
        AddFundsModel(balance = balance(), expectedDepositAddress = DEPOSIT_ADDRESS),
        AddFundsModel(balance = null, expectedDepositAddress = null),
    )

    private fun txInfo(isOutgoing: Boolean, status: TxInfo.TransactionStatus) = TxInfo.Single(
        txHash = "0xhash",
        timestampInMillis = 0L,
        isOutgoing = isOutgoing,
        destinationType = TxInfo.DestinationType.Single(addressType = TxInfo.AddressType.User(DEPOSIT_ADDRESS)),
        sourceType = TxInfo.SourceType.Single(address = DEPOSIT_ADDRESS),
        interactionAddressType = null,
        status = status,
        type = TxInfo.TransactionType.Transfer,
        amount = BigDecimal.ONE,
    )

    private fun supportEmailType() = FeedbackEmailType.Visa.MoneyAccountRequest(
        walletMetaInfo = WalletMetaInfo(userWalletId = userWalletId),
        customerId = CUSTOMER_ID,
    )

    private fun MoneyAccountDetailsModel.menuItem(title: TextReference) =
        uiState.value.menuItems.first { it.title == title }

    private fun SlotNavigation<MoneyAccountDetailsNavigation>.trackSlot(): List<MoneyAccountDetailsNavigation?> {
        val tracked = mutableListOf<MoneyAccountDetailsNavigation?>()
        subscribe { event -> tracked.add(event.transformer(tracked.lastOrNull())) }
        return tracked
    }

    private companion object {
        const val CUSTOMER_ID = "customer"
        const val DEPOSIT_ADDRESS = "0xdeposit"
        const val PRODUCT_INSTANCE_ID = "instance-1"
        const val TERMS_URL = "https://tangem.com/docs/en/virtual-account-terms.pdf"

        val CREDENTIALS = MockBankCredentials.bankCredentials()
    }
}
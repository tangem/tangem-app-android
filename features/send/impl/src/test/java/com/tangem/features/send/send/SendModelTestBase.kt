package com.tangem.features.send.send

import arrow.core.Either
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.qrscanning.usecases.ParseQrCodeUseCase
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendTronGaslessTransactionUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.transaction.usecase.gasless.GetTronGaslessFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.IsTronGaslessSupportedUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.send.api.SendComponent
import com.tangem.features.send.api.SendFeatureToggles
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorCheckReloadListener
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorCheckReloadTrigger
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.api.subcomponents.notifications.SendNotificationsUpdateListener
import com.tangem.features.send.api.subcomponents.notifications.SendNotificationsUpdateTrigger
import com.tangem.features.send.common.SendBalanceUpdater
import com.tangem.features.send.common.SendConfirmAlertFactory
import com.tangem.features.send.send.analytics.SendAnalyticHelper
import com.tangem.features.send.send.confirm.SendConfirmComponent
import com.tangem.features.send.send.confirm.model.SendConfirmModel
import com.tangem.features.send.send.ui.state.SendUM
import com.tangem.features.send.api.subcomponents.amount.SendAmountReduceTrigger
import com.tangem.features.send.api.subcomponents.amount.SendAmountUpdateTrigger
import com.tangem.features.send.testDispatcherProvider
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.settings.IsSendTapHelpEnabledUseCase
import com.tangem.domain.settings.NeverShowTapHelpUseCase
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.tokens.IsAmountSubtractAvailableUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.transaction.usecase.gasless.CreateAndSendGaslessTransactionUseCase
import com.tangem.domain.qrscanning.models.SourceType
import arrow.core.right
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.send.model.SendModel
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.BeforeEach

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class SendModelTestBase {

    protected val testUserWalletId = UserWalletId("1234567890ABCDEF")
    protected val testCryptoCurrency: CryptoCurrency = mockk(relaxed = true)
    protected val testUserWallet: UserWallet = mockk(relaxed = true)
    protected val testCryptoCurrencyStatus: CryptoCurrencyStatus = mockk(relaxed = true) {
        io.mockk.every { currency } returns testCryptoCurrency
    }

    protected val router: Router = mockk(relaxed = true)
    protected val appRouter: AppRouter = mockk(relaxed = true)
    protected val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)
    protected val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase = mockk(relaxed = true)
    protected val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk(relaxed = true)
    protected val listenToQrScanningUseCase: ListenToQrScanningUseCase = mockk(relaxed = true)
    protected val parseQrCodeUseCase: ParseQrCodeUseCase = mockk(relaxed = true)
    protected val sendConfirmAlertFactory: SendConfirmAlertFactory = mockk(relaxed = true)
    protected val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase = mockk(relaxed = true)
    protected val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk(relaxed = true)
    protected val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    protected val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk(relaxed = true)
    protected val createTransferTransactionUseCase: CreateTransferTransactionUseCase = mockk(relaxed = true)
    protected val getFeeUseCase: GetFeeUseCase = mockk(relaxed = true)
    protected val getFeeForGaslessUseCase: GetFeeForGaslessUseCase = mockk(relaxed = true)
    protected val getFeeForTokenUseCase: GetFeeForTokenUseCase = mockk(relaxed = true)
    protected val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase = mockk(relaxed = true)
    protected val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk(relaxed = true)
    protected val sendAmountUpdateTrigger: SendAmountUpdateTrigger = mockk(relaxed = true)
    protected val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    protected val sendTransactionUseCase: SendTransactionUseCase = mockk(relaxed = true)
    protected val getTronGaslessFeeUseCase: GetTronGaslessFeeUseCase = mockk(relaxed = true)
    protected val isTronGaslessSupportedUseCase: IsTronGaslessSupportedUseCase = mockk(relaxed = true)
    protected val sendFeatureToggles: SendFeatureToggles = mockk(relaxed = true)

    // SendConfirmModel-specific dependencies
    protected val isSendTapHelpEnabledUseCase: IsSendTapHelpEnabledUseCase = mockk(relaxed = true)
    protected val neverShowTapHelpUseCase: NeverShowTapHelpUseCase = mockk(relaxed = true)
    protected val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase = mockk(relaxed = true)
    protected val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase = mockk(relaxed = true)
    protected val feeSelectorCheckReloadListener: FeeSelectorCheckReloadListener = mockk(relaxed = true)
    protected val feeSelectorCheckReloadTrigger: FeeSelectorCheckReloadTrigger = mockk(relaxed = true)
    protected val notificationsUpdateTrigger: SendNotificationsUpdateTrigger = mockk(relaxed = true)
    protected val notificationsUpdateListener: SendNotificationsUpdateListener = mockk(relaxed = true)
    protected val urlOpener: UrlOpener = mockk(relaxed = true)
    protected val shareManager: ShareManager = mockk(relaxed = true)
    protected val feeSelectorReloadTrigger: FeeSelectorReloadTrigger = mockk(relaxed = true)
    protected val sendAmountReduceTrigger: SendAmountReduceTrigger = mockk(relaxed = true)
    protected val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase = mockk(relaxed = true)
    protected val currenciesRepository: CurrenciesRepository = mockk(relaxed = true)
    protected val createAndSendGaslessTransactionUseCase: CreateAndSendGaslessTransactionUseCase = mockk(relaxed = true)
    protected val createAndSendTronGaslessTransactionUseCase: CreateAndSendTronGaslessTransactionUseCase =
        mockk(relaxed = true)
    protected val sendAnalyticHelper: SendAnalyticHelper = mockk(relaxed = true)
    protected val sendBalanceUpdaterFactory: SendBalanceUpdater.Factory = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        // Reset recorded calls on use-cases asserted via coVerify(exactly=N). PER_CLASS parameterized
        // tests (e.g. SendConfirmModelTest) reuse one instance, so calls would otherwise accumulate
        // across rows. answers=false keeps the happy-path stubs re-applied below.
        clearMocks(
            createTransferTransactionUseCase,
            sendTransactionUseCase,
            createAndSendGaslessTransactionUseCase,
            feeSelectorCheckReloadTrigger,
            answers = false,
            recordedCalls = true,
            childMocks = false,
        )

        // --- SendModel init-path happy stubs ---
        every { getUserWalletUseCase(testUserWalletId) } returns testUserWallet.right()
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        every { getSelectedAppCurrencyUseCase() } returns flowOf(AppCurrency.Default.right())
        every { listenToQrScanningUseCase(SourceType.SEND) } returns emptyFlow<String>().right()
        every { getBalanceHidingSettingsUseCase() } returns emptyFlow()
        every { getAccountCurrencyStatusUseCase(testUserWalletId, testCryptoCurrency) } returns emptyFlow()
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any()) } returns testCryptoCurrencyStatus.right()
        // no-fee overload (disambiguated by memo: String at position 2); 6 matchers cover defaulted nonce
        coEvery {
            createTransferTransactionUseCase(any(), any<String>(), any(), any(), any(), any())
        } returns mockk<TransactionData.Uncompiled>(relaxed = true).right()
        // with-fee overload (disambiguated by Fee at position 2); 7 matchers cover defaulted nonce
        coEvery {
            createTransferTransactionUseCase(any(), any<Fee>(), any(), any(), any(), any(), any())
        } returns mockk<TransactionData.Uncompiled>(relaxed = true).right()
        coEvery { sendTransactionUseCase(any(), any(), any()) } returns "txHash".right()
        coEvery { createAndSendGaslessTransactionUseCase(any(), any(), any()) } returns "txHash".right()
        every { getExplorerTransactionUrlUseCase(any(), any()) } returns "https://explorer/tx".right()

        // --- SendConfirmModel init-path happy stubs ---
        coEvery { isSendTapHelpEnabledUseCase.invokeSync() } returns false.right()
        every { isSendTapHelpEnabledUseCase() } returns emptyFlow<Boolean>().right()
        coEvery { isAmountSubtractAvailableUseCase(any(), any(), any()) } returns false.right()
        every { feeSelectorCheckReloadListener.checkReloadResultFlow } returns emptyFlow()
        every { notificationsUpdateListener.hasErrorFlow } returns emptyFlow()
    }

    protected fun createSendModel(
        testScope: TestScope,
        paramsContainer: ParamsContainer = MutableParamsContainer(defaultSendParams()),
    ): SendModel {
        return SendModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.testDispatcherProvider(),
            router = router,
            getUserWalletUseCase = getUserWalletUseCase,
            getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            listenToQrScanningUseCase = listenToQrScanningUseCase,
            parseQrCodeUseCase = parseQrCodeUseCase,
            sendConfirmAlertFactory = sendConfirmAlertFactory,
            saveBlockchainErrorUseCase = saveBlockchainErrorUseCase,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            createTransferTransactionUseCase = createTransferTransactionUseCase,
            getFeeUseCase = getFeeUseCase,
            getFeeForGaslessUseCase = getFeeForGaslessUseCase,
            getFeeForTokenUseCase = getFeeForTokenUseCase,
            getAccountCurrencyStatusUseCase = getAccountCurrencyStatusUseCase,
            isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
            sendAmountUpdateTrigger = sendAmountUpdateTrigger,
            analyticsEventHandler = analyticsEventHandler,
            getTronGaslessFeeUseCase = getTronGaslessFeeUseCase,
            isTronGaslessSupportedUseCase = isTronGaslessSupportedUseCase,
            sendFeatureToggles = sendFeatureToggles,
        )
    }

    protected fun createSendConfirmModel(
        testScope: TestScope,
        paramsContainer: ParamsContainer = MutableParamsContainer(defaultSendConfirmParams()),
    ): SendConfirmModel {
        return SendConfirmModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.testDispatcherProvider(),
            analyticsEventHandler = analyticsEventHandler,
            appRouter = appRouter,
            router = router,
            isSendTapHelpEnabledUseCase = isSendTapHelpEnabledUseCase,
            neverShowTapHelpUseCase = neverShowTapHelpUseCase,
            createTransferTransactionUseCase = createTransferTransactionUseCase,
            sendTransactionUseCase = sendTransactionUseCase,
            saveBlockchainErrorUseCase = saveBlockchainErrorUseCase,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            getExplorerTransactionUrlUseCase = getExplorerTransactionUrlUseCase,
            isAmountSubtractAvailableUseCase = isAmountSubtractAvailableUseCase,
            feeSelectorCheckReloadListener = feeSelectorCheckReloadListener,
            feeSelectorCheckReloadTrigger = feeSelectorCheckReloadTrigger,
            notificationsUpdateTrigger = notificationsUpdateTrigger,
            notificationsUpdateListener = notificationsUpdateListener,
            alertFactory = sendConfirmAlertFactory,
            sendAnalyticHelper = sendAnalyticHelper,
            urlOpener = urlOpener,
            shareManager = shareManager,
            feeSelectorReloadTrigger = feeSelectorReloadTrigger,
            sendAmountReduceTrigger = sendAmountReduceTrigger,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            manageCryptoCurrenciesUseCase = manageCryptoCurrenciesUseCase,
            currenciesRepository = currenciesRepository,
            createAndSendGaslessTransactionUseCase = createAndSendGaslessTransactionUseCase,
            createAndSendTronGaslessTransactionUseCase = createAndSendTronGaslessTransactionUseCase,
            sendBalanceUpdaterFactory = sendBalanceUpdaterFactory,
        )
    }

    protected open fun defaultSendParams(): SendComponent.Params = SendComponent.Params(
        userWalletId = testUserWalletId,
        currency = testCryptoCurrency,
        amount = null,
        destinationAddress = null,
        tag = null,
        transactionId = null,
        entryType = SendComponent.EntryType.Manual,
        callback = mockk(relaxed = true),
    )

    protected fun defaultSendConfirmParams(
        state: SendUM = SendUM(
            amountUM = AmountState.Empty,
            destinationUM = DestinationUM.Empty(),
            feeSelectorUM = FeeSelectorUM.Loading,
            confirmUM = ConfirmUM.Empty,
            navigationUM = NavigationUM.Empty,
            confirmData = null,
        ),
        cryptoCurrencyStatus: CryptoCurrencyStatus = testCryptoCurrencyStatus,
        feeCryptoCurrencyStatus: CryptoCurrencyStatus = testCryptoCurrencyStatus,
    ): SendConfirmComponent.Params = SendConfirmComponent.Params(
        state = state,
        analyticsCategoryName = "test_send",
        analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Send,
        userWallet = testUserWallet,
        cryptoCurrencyStatus = cryptoCurrencyStatus,
        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
        cryptoCurrencyStatusFlow = kotlinx.coroutines.flow.MutableStateFlow(cryptoCurrencyStatus),
        feeCryptoCurrencyStatusFlow = kotlinx.coroutines.flow.MutableStateFlow(feeCryptoCurrencyStatus),
        accountFlow = kotlinx.coroutines.flow.MutableStateFlow(null),
        isAccountModeFlow = kotlinx.coroutines.flow.MutableStateFlow(false),
        appCurrency = AppCurrency.Default,
        callback = mockk(relaxed = true),
        currentRoute = kotlinx.coroutines.flow.flowOf(),
        isBalanceHidingFlow = kotlinx.coroutines.flow.MutableStateFlow(false),
        predefinedValues = PredefinedValues.Empty,
        onLoadFee = { Either.Right(mockk(relaxed = true)) },
        onLoadFeeExtended = { Either.Right(mockk(relaxed = true)) },
        onSendTransaction = {},
    )
}
package com.tangem.feature.swap.model

import arrow.core.right
import com.tangem.common.ui.bottomsheet.permission.state.ApproveType
import com.tangem.core.analytics.api.AnalyticsErrorHandler
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.common.routing.AppRouter
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetPaymentAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.stories.ShouldShowStoriesUseCase
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.swap.usecase.CalculateAmountUseCase
import com.tangem.domain.tangempay.GetTangemPayCustomerIdUseCase
import com.tangem.domain.tangempay.TangemPayWithdrawWithSwapUseCase
import com.tangem.domain.tokens.GetMinimumTransactionAmountSyncUseCase
import com.tangem.domain.tokens.UpdateDelayedNetworkStatusUseCase
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.feature.swap.domain.GetSwapUiModeUseCase
import com.tangem.feature.swap.domain.SetSwapUiModeUseCase
import com.tangem.feature.swap.domain.AllowPermissionsHandler
import com.tangem.feature.swap.domain.SwapInteractor
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.IntegratedApprovalData
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.transfer.SwapTransferInteractor
import com.tangem.feature.swap.ui.transfer.SwapTransferStateBuilder
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridge
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.swap.SwapComponent
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.emptyFlow

/**
 * Shared test infrastructure for [SwapModel] unit tests.
 *
 * Wires every constructor dependency as a relaxed MockK and stubs the init-block calls so that
 * constructing the model has no observable side effects. See `swap-model-test-plan.md` for details.
 */
internal abstract class SwapModelTestBase {

    protected val router: Router = mockk(relaxed = true)
    protected val appRouter: AppRouter = mockk(relaxed = true)
    protected val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    protected val analyticsErrorEventHandler: AnalyticsErrorHandler = mockk(relaxed = true)
    protected val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk(relaxed = true)
    protected val updateDelayedCurrencyStatusUseCase: UpdateDelayedNetworkStatusUseCase = mockk(relaxed = true)
    protected val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase =
        mockk(relaxed = true)
    protected val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk(relaxed = true)
    protected val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase = mockk(relaxed = true)
    protected val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    protected val getMinimumTransactionAmountSyncUseCase: GetMinimumTransactionAmountSyncUseCase =
        mockk(relaxed = true)
    protected val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase = mockk(relaxed = true)
    protected val shouldShowStoriesUseCase: ShouldShowStoriesUseCase = mockk(relaxed = true)
    protected val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk(relaxed = true)
    protected val swapInteractor: SwapInteractor = mockk(relaxed = true)
    protected val swapTransferInteractor: SwapTransferInteractor = mockk(relaxed = true)
    protected val swapTransferStateBuilder: SwapTransferStateBuilder = mockk(relaxed = true)
    protected val urlOpener: UrlOpener = mockk(relaxed = true)
    protected val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase = mockk(relaxed = true)
    protected val getPaymentAccountCryptoCurrencyStatusUseCase: GetPaymentAccountCryptoCurrencyStatusUseCase =
        mockk(relaxed = true)
    protected val tangemPayWithdrawWithSwapUseCase: TangemPayWithdrawWithSwapUseCase = mockk(relaxed = true)
    protected val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk(relaxed = true)
    protected val feeSelectorReloadTrigger: FeeSelectorReloadTrigger = mockk(relaxed = true)
    protected val getTangemPayCustomerIdUseCase: GetTangemPayCustomerIdUseCase = mockk(relaxed = true)
    protected val appsFlyerStore: AppsFlyerStore = mockk(relaxed = true)
    protected val messageSender: UiMessageSender = mockk(relaxed = true)
    protected val initialCurrenciesResolver: InitialCurrenciesResolver = mockk(relaxed = true)
    protected val allowPermissionsHandler: AllowPermissionsHandler = mockk(relaxed = true)
    protected val swapFeatureToggles: SwapFeatureToggles = mockk(relaxed = true)
    protected val getSwapUiModeUseCase: GetSwapUiModeUseCase = mockk(relaxed = true)
    protected val setSwapUiModeUseCase: SetSwapUiModeUseCase = mockk(relaxed = true)
    protected val calculateAmountUseCase: CalculateAmountUseCase = mockk(relaxed = true)

    private val chooseTokenBridgeFactory: ChooseTokenBridge.Factory = mockk(relaxed = true)
    private val getUserCountryUseCase: GetUserCountryUseCase = mockk(relaxed = true)
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk(relaxed = true)

    protected val userWalletId = UserWalletId(stringValue = "0123456789ABCDEF")

    /** Stubs init-block calls so [createModel] has no side effects. Call from `@BeforeEach`. */
    protected fun setUpBase() {
        val bridge = mockk<ChooseTokenBridge>(relaxed = true) {
            every { onCurrencyChosen } returns Channel()
            every { onClose } returns Channel()
        }
        every { chooseTokenBridgeFactory.create(any(), any(), any()) } returns bridge

        every { getUserCountryUseCase.invokeSync() } returns UserCountry.Other("US").right()
        every { getBalanceHidingSettingsUseCase.invoke() } returns emptyFlow()
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false
        coEvery { shouldShowStoriesUseCase.invokeSync(any()) } returns false
        coEvery { initialCurrenciesResolver.invoke(any(), any(), any(), any()) } returns (null to null)
        every { getSelectedAppCurrencyUseCase.invoke() } returns emptyFlow()
        every { swapFeatureToggles.isSwapIntegratedApproveEnabled } returns true
    }

    protected fun createParams(): SwapComponent.Params = SwapComponent.Params(
        userWalletId = userWalletId,
        fromCryptoCurrency = null,
        screenSource = "Test",
    )

    @Suppress("LongMethod")
    protected fun createModel(): SwapModel = SwapModel(
        paramsContainer = MutableParamsContainer(createParams()),
        getUserCountryUseCase = getUserCountryUseCase,
        getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
        chooseTokenBridgeFactory = chooseTokenBridgeFactory,
        router = router,
        appRouter = appRouter,
        dispatchers = TestingCoroutineDispatcherProvider(),
        analyticsEventHandler = analyticsEventHandler,
        analyticsErrorEventHandler = analyticsErrorEventHandler,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        updateDelayedCurrencyStatusUseCase = updateDelayedCurrencyStatusUseCase,
        getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
        getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
        saveBlockchainErrorUseCase = saveBlockchainErrorUseCase,
        sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
        getMinimumTransactionAmountSyncUseCase = getMinimumTransactionAmountSyncUseCase,
        getExplorerTransactionUrlUseCase = getExplorerTransactionUrlUseCase,
        shouldShowStoriesUseCase = shouldShowStoriesUseCase,
        isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
        swapInteractor = swapInteractor,
        swapTransferInteractor = swapTransferInteractor,
        swapTransferStateBuilder = swapTransferStateBuilder,
        urlOpener = urlOpener,
        getAccountCurrencyStatusUseCase = getAccountCurrencyStatusUseCase,
        getPaymentAccountCryptoCurrencyStatusUseCase = getPaymentAccountCryptoCurrencyStatusUseCase,
        tangemPayWithdrawWithSwapUseCase = tangemPayWithdrawWithSwapUseCase,
        isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
        feeSelectorReloadTrigger = feeSelectorReloadTrigger,
        getTangemPayCustomerIdUseCase = getTangemPayCustomerIdUseCase,
        appsFlyerStore = appsFlyerStore,
        messageSender = messageSender,
        initialCurrenciesResolver = initialCurrenciesResolver,
        allowPermissionsHandler = allowPermissionsHandler,
        swapFeatureToggles = swapFeatureToggles,
        getSwapUiModeUseCase = getSwapUiModeUseCase,
        setSwapUiModeUseCase = setSwapUiModeUseCase,
        calculateAmountUseCase = calculateAmountUseCase,
    )

    // region builders

    protected fun swapProvider(
        id: String = "provider-1",
        name: String = "1inch",
        type: ExchangeProviderType = ExchangeProviderType.DEX,
    ): SwapProvider = SwapProvider(
        providerId = id,
        name = name,
        type = type,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        slippage = null,
    )

    protected fun permissionSettings(
        type: ApproveType = ApproveType.LIMITED,
        spender: String = "0xSpender",
    ): PermissionDataState.PermissionSettings = PermissionDataState.PermissionSettings(
        type = type,
        spenderAddress = spender,
    )

    protected fun quotesLoadedState(
        provider: SwapProvider,
        permissionState: PermissionDataState = PermissionDataState.Empty,
        integratedApprovalData: IntegratedApprovalData? = null,
    ): SwapState.QuotesLoadedState = mockk(relaxed = true) {
        every { swapProvider } returns provider
        every { this@mockk.permissionState } returns permissionState
        every { this@mockk.integratedApprovalData } returns integratedApprovalData
        // Matcher for the copy(...) overload `handleFeeError` uses on the integrated-approval
        // fallback path: it copies `integratedApprovalData` (→ null) and `permissionState`
        // (→ PermissionRequired). Includes `integratedApprovalData` so MockK matches that call
        // and the rebuilt mock reflects the new permissionState / integratedApprovalData.
        every {
            copy(
                fromTokenInfo = any(),
                toTokenInfo = any(),
                priceImpact = any(),
                preparedSwapConfigState = any(),
                permissionState = any(),
                swapDataModel = any(),
                currencyCheck = any(),
                validationResult = any(),
                minAdaValue = any(),
                swapProvider = any(),
                integratedApprovalData = any(),
            )
        } answers {
            // `copy` arg indices follow the QuotesLoadedState primary-constructor order:
            // 0 fromTokenInfo, 1 toTokenInfo, 2 swapProvider, 3 priceImpact,
            // 4 preparedSwapConfigState, 5 permissionState, 6 swapDataModel,
            // 7 integratedApprovalData, 8 currencyCheck, 9 validationResult, 10 minAdaValue.
            quotesLoadedState(
                provider = provider,
                permissionState = arg(5),
                integratedApprovalData = arg(7),
            )
        }
    }

    protected fun swapCurrencyStatus(
        wallet: UserWallet = mockk(relaxed = true),
        status: CryptoCurrencyStatus = mockk(relaxed = true),
        currency: CryptoCurrency = mockk(relaxed = true),
    ): SwapCurrencyStatus = mockk(relaxed = true) {
        every { userWallet } returns wallet
        every { this@mockk.status } returns status
        every { this@mockk.currency } returns currency
    }

    // endregion
}
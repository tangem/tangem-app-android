package com.tangem.feature.tokendetails.presentation.tokendetails.model

import arrow.core.none
import arrow.core.right
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.backup.BackupErrorWarningSender
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.haptic.VibratorHapticManager
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.account.status.usecase.IsCryptoCurrencyCouldHideUseCase
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.dynamicaddresses.IsDynamicAddressesAvailableUseCase
import com.tangem.domain.dynamicaddresses.IsXpubSupportedUseCase
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.jointaccount.usecase.GetJointOperationSlotUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.offramp.GetOfframpUrlUseCase
import com.tangem.domain.onramp.CheckOnrampAvailabilityUseCase
import com.tangem.domain.onramp.OnrampGetDefaultCurrencyUseCase
import com.tangem.domain.staking.FetchStakingOptionsUseCase
import com.tangem.domain.staking.GetStakingAvailabilityUseCase
import com.tangem.domain.staking.GetStakingEntryInfoUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyActionsUseCase
import com.tangem.domain.tokens.NeedShowYieldSupplyDepositedWarningUseCase
import com.tangem.domain.tokens.SaveViewedTokenReceiveWarningUseCase
import com.tangem.domain.tokens.SaveViewedYieldSupplyWarningUseCase
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.transaction.usecase.AssociateAssetUseCase
import com.tangem.domain.transaction.usecase.DismissIncompleteTransactionUseCase
import com.tangem.domain.transaction.usecase.OpenTrustlineUseCase
import com.tangem.domain.transaction.usecase.ReceiveAddressesFactory
import com.tangem.domain.transaction.usecase.RetryIncompleteTransactionUseCase
import com.tangem.domain.transaction.usecase.SignCloreMessageUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetFixedTxHistoryItemsUseCase
import com.tangem.domain.wallets.usecase.GetExploreUrlUseCase
import com.tangem.domain.wallets.usecase.GetExtendedPublicKeyForCurrencyUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.domain.wallets.usecase.NetworkHasDerivationUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetRewardsBalanceUseCase
import com.tangem.feature.tokendetails.domain.GetCurrencyWarningsUseCase
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsStateController
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.TokenDetailsNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.QuickTopUpBlockFactory
import com.tangem.features.tokendetails.ExpressTransactionsEventListener
import com.tangem.features.tokendetails.TokenDetailsComponent
import com.tangem.features.txhistory.entity.TxHistoryContentUpdateEmitter
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.core.getEmittedValues
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.BeforeEach
import java.math.BigDecimal

/**
 * Builds a [TokenDetailsModel] whose currency status and warnings are driven by the test.
 *
 * [getCurrencyWarningsUseCase] mirrors the production rule for the outdated data warning: it is present only when
 * the status total source is [StatusSource.ONLY_CACHE]. Other warnings come from [extraWarnings].
 */
internal abstract class TokenDetailsModelTestBase {

    protected val userWallet: UserWallet.Cold = MockUserWalletFactory.create()
    protected val userWalletId = userWallet.walletId

    private val currencyFactory = MockCryptoCurrencyFactory(userWallet)
    protected val ethereum: CryptoCurrency = currencyFactory.ethereum
    protected val bitcoin: CryptoCurrency = currencyFactory.bitcoin

    private val account = MockAccounts.createAccount(
        derivationIndex = 1,
        cryptoCurrencies = listOf(ethereum, bitcoin),
        userWalletId = userWalletId,
    )

    protected var extraWarnings: Set<CryptoCurrencyWarning> = emptySet()
    private val statusFlows = mutableMapOf<CryptoCurrency.ID, MutableStateFlow<CryptoCurrencyStatus?>>()

    protected val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase = mockk()
    protected val getCurrencyWarningsUseCase: GetCurrencyWarningsUseCase = mockk()
    protected val dismissIncompleteTransactionUseCase: DismissIncompleteTransactionUseCase = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val getCryptoCurrencyActionsUseCase: GetCryptoCurrencyActionsUseCase = mockk()
    private val getStakingAvailabilityUseCase: GetStakingAvailabilityUseCase = mockk()
    private val networkHasDerivationUseCase: NetworkHasDerivationUseCase = mockk()
    private val isXpubSupportedUseCase: IsXpubSupportedUseCase = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val dynamicAddressesRepository: DynamicAddressesRepository = mockk()

    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher = mockk(relaxed = true)
    private val getExploreUrlUseCase: GetExploreUrlUseCase = mockk(relaxed = true)
    private val isCryptoCurrencyCouldHideUseCase: IsCryptoCurrencyCouldHideUseCase = mockk(relaxed = true)
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase = mockk(relaxed = true)
    private val getExtendedPublicKeyForCurrencyUseCase: GetExtendedPublicKeyForCurrencyUseCase = mockk(relaxed = true)
    private val getStakingEntryInfoUseCase: GetStakingEntryInfoUseCase = mockk(relaxed = true)
    private val fetchStakingOptionsUseCase: FetchStakingOptionsUseCase = mockk(relaxed = true)
    private val isDemoCardUseCase: IsDemoCardUseCase = mockk(relaxed = true)
    private val backupErrorWarningSender: BackupErrorWarningSender = mockk(relaxed = true)
    protected val associateAssetUseCase: AssociateAssetUseCase = mockk(relaxed = true)
    protected val retryIncompleteTransactionUseCase: RetryIncompleteTransactionUseCase = mockk(relaxed = true)
    protected val openTrustlineUseCase: OpenTrustlineUseCase = mockk(relaxed = true)
    private val getOfframpUrlUseCase: GetOfframpUrlUseCase = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val vibratorHapticManager: VibratorHapticManager = mockk(relaxed = true)
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val txHistoryContentUpdateEmitter: TxHistoryContentUpdateEmitter = mockk(relaxed = true)
    private val expressTransactionsEventListener: ExpressTransactionsEventListener = mockk(relaxed = true)
    private val router: Router = mockk(relaxed = true)
    private val tokenDetailsRouter: InnerTokenDetailsRouter = mockk(relaxed = true)
    private val receiveAddressesFactory: ReceiveAddressesFactory = mockk(relaxed = true)
    private val saveViewedYieldSupplyWarningUseCase: SaveViewedYieldSupplyWarningUseCase = mockk(relaxed = true)
    private val saveViewedTokenReceiveWarningUseCase: SaveViewedTokenReceiveWarningUseCase = mockk(relaxed = true)
    private val needShowYieldSupplyDepositedWarningUseCase: NeedShowYieldSupplyDepositedWarningUseCase =
        mockk(relaxed = true)
    private val getJointOperationSlotUseCase: GetJointOperationSlotUseCase = mockk(relaxed = true)
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase = mockk(relaxed = true)
    private val yieldSupplyGetRewardsBalanceUseCase: YieldSupplyGetRewardsBalanceUseCase = mockk(relaxed = true)
    private val signCloreMessageUseCase: SignCloreMessageUseCase = mockk(relaxed = true)
    private val dynamicAddressesDelegateFactory: DynamicAddressesDelegate.Factory = mockk(relaxed = true)
    private val isDynamicAddressesAvailableUseCase: IsDynamicAddressesAvailableUseCase = mockk(relaxed = true)
    private val dialogFactory: TokenDetailsDialogFactory = mockk(relaxed = true)
    private val getWalletIconUseCase: GetWalletIconUseCase = mockk(relaxed = true)
    private val walletIconUMConverter: WalletIconUMConverter = mockk(relaxed = true)
    private val quickTopUpBlockFactory: QuickTopUpBlockFactory = mockk(relaxed = true)
    private val getFixedTxHistoryItemsUseCase: GetFixedTxHistoryItemsUseCase = mockk(relaxed = true)
    private val checkOnrampAvailabilityUseCase: CheckOnrampAvailabilityUseCase = mockk(relaxed = true)
    private val onrampGetDefaultCurrencyUseCase: OnrampGetDefaultCurrencyUseCase = mockk(relaxed = true)

    @BeforeEach
    fun setUpTokenDetailsModelMocks() {
        clearMocks(
            getAccountCurrencyStatusUseCase,
            getCurrencyWarningsUseCase,
            dismissIncompleteTransactionUseCase,
            getUserWalletUseCase,
            getSelectedAppCurrencyUseCase,
            getBalanceHidingSettingsUseCase,
            getCryptoCurrencyActionsUseCase,
            getStakingAvailabilityUseCase,
            networkHasDerivationUseCase,
            isXpubSupportedUseCase,
            userWalletsListRepository,
            isAccountsModeEnabledUseCase,
            singleAccountListSupplier,
            dynamicAddressesRepository,
        )
        extraWarnings = emptySet()
        statusFlows.clear()

        every { getUserWalletUseCase(any()) } returns userWallet.right()
        every { getSelectedAppCurrencyUseCase() } returns flowOf(AppCurrency.Default.right())
        every { getBalanceHidingSettingsUseCase() } returns emptyFlow()
        every { getCryptoCurrencyActionsUseCase(any(), any(), any()) } returns emptyFlow()
        every { getStakingAvailabilityUseCase(any(), any()) } returns emptyFlow()
        every { networkHasDerivationUseCase(any(), any()) } returns false.right()
        coEvery { isXpubSupportedUseCase(any(), any()) } returns false
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every { isAccountsModeEnabledUseCase() } returns flowOf(false)
        every { singleAccountListSupplier(any<UserWalletId>()) } returns emptyFlow()
        coEvery { dismissIncompleteTransactionUseCase(any(), any()) } returns Unit.right()
        every { dynamicAddressesRepository.isDynamicAddressesEnabledForNetwork(any(), any()) } returns flowOf(false)

        coEvery {
            getAccountCurrencyStatusUseCase.invokeSync(userWalletId = any(), currency = any<CryptoCurrency>())
        } returns none()
        every {
            getAccountCurrencyStatusUseCase(userWalletId = any(), currency = any<CryptoCurrency>())
        } answers {
            statusFlowOf(secondArg())
                .filterNotNull()
                .map { status -> AccountCryptoCurrencyStatus(account = account, status = status) }
        }
        coEvery { getCurrencyWarningsUseCase(any(), any(), any()) } answers {
            flowOf(warningsFor(status = secondArg()))
        }
    }

    protected fun createModel(testScope: TestScope, currency: CryptoCurrency = ethereum): TokenDetailsModel {
        return TokenDetailsModel(
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            cryptoCurrencyBalanceFetcher = cryptoCurrencyBalanceFetcher,
            getExploreUrlUseCase = getExploreUrlUseCase,
            getCryptoCurrencyActionsUseCase = getCryptoCurrencyActionsUseCase,
            isCryptoCurrencyCouldHideUseCase = isCryptoCurrencyCouldHideUseCase,
            getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
            getCurrencyWarningsUseCase = getCurrencyWarningsUseCase,
            getExplorerTransactionUrlUseCase = getExplorerTransactionUrlUseCase,
            getExtendedPublicKeyForCurrencyUseCase = getExtendedPublicKeyForCurrencyUseCase,
            getStakingEntryInfoUseCase = getStakingEntryInfoUseCase,
            getStakingAvailabilityUseCase = getStakingAvailabilityUseCase,
            fetchStakingOptionsUseCase = fetchStakingOptionsUseCase,
            networkHasDerivationUseCase = networkHasDerivationUseCase,
            isDemoCardUseCase = isDemoCardUseCase,
            backupErrorWarningSender = backupErrorWarningSender,
            associateAssetUseCase = associateAssetUseCase,
            retryIncompleteTransactionUseCase = retryIncompleteTransactionUseCase,
            openTrustlineUseCase = openTrustlineUseCase,
            dismissIncompleteTransactionUseCase = dismissIncompleteTransactionUseCase,
            getOfframpUrlUseCase = getOfframpUrlUseCase,
            urlOpener = urlOpener,
            analyticsEventsHandler = analyticsEventHandler,
            vibratorHapticManager = vibratorHapticManager,
            clipboardManager = clipboardManager,
            uiMessageSender = uiMessageSender,
            txHistoryContentUpdateEmitter = txHistoryContentUpdateEmitter,
            expressTransactionsEventListener = expressTransactionsEventListener,
            paramsContainer = MutableParamsContainer(
                TokenDetailsComponent.Params(userWalletId = userWalletId, currency = currency),
            ),
            getUserWalletUseCase = getUserWalletUseCase,
            router = router,
            tokenDetailsRouter = tokenDetailsRouter,
            receiveAddressesFactory = receiveAddressesFactory,
            saveViewedYieldSupplyWarningUseCase = saveViewedYieldSupplyWarningUseCase,
            saveViewedTokenReceiveWarningUseCase = saveViewedTokenReceiveWarningUseCase,
            needShowYieldSupplyDepositedWarningUseCase = needShowYieldSupplyDepositedWarningUseCase,
            getJointOperationSlotUseCase = getJointOperationSlotUseCase,
            getAccountCryptoCurrencyStatusUseCase = getAccountCurrencyStatusUseCase,
            manageCryptoCurrenciesUseCase = manageCryptoCurrenciesUseCase,
            yieldSupplyGetRewardsBalanceUseCase = yieldSupplyGetRewardsBalanceUseCase,
            signCloreMessageUseCase = signCloreMessageUseCase,
            isXpubSupportedUseCase = isXpubSupportedUseCase,
            dynamicAddressesRepository = dynamicAddressesRepository,
            dynamicAddressesDelegateFactory = dynamicAddressesDelegateFactory,
            isDynamicAddressesAvailableUseCase = isDynamicAddressesAvailableUseCase,
            dialogFactory = dialogFactory,
            userWalletsListRepository = userWalletsListRepository,
            singleAccountListSupplier = singleAccountListSupplier,
            getWalletIconUseCase = getWalletIconUseCase,
            walletIconUMConverter = walletIconUMConverter,
            isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
            redesignStateController = TokenDetailsStateController(),
            quickTopUpBlockFactory = quickTopUpBlockFactory,
            getFixedTxHistoryItemsUseCase = getFixedTxHistoryItemsUseCase,
            checkOnrampAvailabilityUseCase = checkOnrampAvailabilityUseCase,
            onrampGetDefaultCurrencyUseCase = onrampGetDefaultCurrencyUseCase,
        )
    }

    /** Pushes a new status into the currency status flow the model subscribes to. */
    protected fun emitStatus(status: CryptoCurrencyStatus) {
        statusFlowOf(status.currency).value = status
    }

    /** Every distinct notifications list the legacy UI state goes through, starting from the current one. */
    protected fun TestScope.recordNotifications(model: TokenDetailsModel): List<List<TokenDetailsNotification>> {
        return getEmittedValues(
            flow = model.uiState.map { state -> state.notifications.toList() }.distinctUntilChanged(),
        )
    }

    protected fun createLoadedStatus(
        currency: CryptoCurrency = ethereum,
        networkSource: StatusSource,
        amount: BigDecimal = BigDecimal.ONE,
    ): CryptoCurrencyStatus {
        return CryptoCurrencyStatus(
            currency = currency,
            value = CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = amount,
                fiatRate = BigDecimal.ONE,
                priceChange = BigDecimal.ZERO,
                yieldSupplyStatus = null,
                hasCurrentNetworkTransactions = false,
                pendingTransactions = emptySet(),
                networkAddress = NetworkAddress.Single(
                    defaultAddress = NetworkAddress.Address(value = "0x1", type = NetworkAddress.Address.Type.Primary),
                ),
                sources = CryptoCurrencyStatus.Sources(networkSource = networkSource),
            ),
        )
    }

    private fun statusFlowOf(currency: CryptoCurrency): MutableStateFlow<CryptoCurrencyStatus?> {
        return statusFlows.getOrPut(currency.id) { MutableStateFlow(null) }
    }

    private fun warningsFor(status: CryptoCurrencyStatus): Set<CryptoCurrencyWarning> {
        return if (status.value.sources.total == StatusSource.ONLY_CACHE) {
            extraWarnings + CryptoCurrencyWarning.UsedOutdatedDataWarning
        } else {
            extraWarnings
        }
    }
}
package com.tangem.features.send.impl.presentation.viewmodel

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.qrscanning.usecases.ParseQrCodeUseCase
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.settings.IsSendTapHelpEnabledUseCase
import com.tangem.domain.settings.NeverShowTapHelpUseCase
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.tokens.utils.convertToAmount
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetFixedTxHistoryItemsUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.wallets.usecase.ValidateWalletAddressUseCase
import com.tangem.domain.wallets.usecase.ValidateWalletMemoUseCase
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.send.impl.navigation.InnerSendRouter
import com.tangem.features.send.impl.presentation.analytics.EnterAddressSource
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents
import com.tangem.features.send.impl.presentation.analytics.SendScreenSource
import com.tangem.features.send.impl.presentation.analytics.utils.SendOnNextScreenAnalyticSender
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.state.*
import com.tangem.features.send.impl.presentation.state.amount.AmountStateFactory
import com.tangem.features.send.impl.presentation.state.confirm.SendNotificationFactory
import com.tangem.features.send.impl.presentation.state.fee.*
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
@HiltViewModel
internal class SendViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getCryptoCurrencyStatusSyncUseCase: GetCryptoCurrencyStatusSyncUseCase,
    private val getCryptoCurrencyStatusesSyncUseCase: GetCryptoCurrencyStatusesSyncUseCase,
    private val getFixedTxHistoryItemsUseCase: GetFixedTxHistoryItemsUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val validateWalletAddressUseCase: ValidateWalletAddressUseCase,
    private val reduxStateHolder: ReduxStateHolder,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val parseQrCodeUseCase: ParseQrCodeUseCase,
    private val isSendTapHelpEnabledUseCase: IsSendTapHelpEnabledUseCase,
    private val neverShowTapHelpUseCase: NeverShowTapHelpUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    currencyChecksRepository: CurrencyChecksRepository,
    isFeeApproximateUseCase: IsFeeApproximateUseCase,
    validateWalletMemoUseCase: ValidateWalletMemoUseCase,
    getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, SendClickIntents {

    private val userWalletId: UserWalletId = savedStateHandle.get<String>(SendRouter.USER_WALLET_ID_KEY)
        ?.let { stringValue -> UserWalletId(stringValue) }
        ?: error("This screen can't open without `UserWalletId`")

    private val cryptoCurrency: CryptoCurrency = savedStateHandle[SendRouter.CRYPTO_CURRENCY_KEY]
        ?: error("This screen can't open without `CryptoCurrency`")

    private val transactionId: String? = savedStateHandle[SendRouter.TRANSACTION_ID_KEY]
    private val amount: String? = savedStateHandle[SendRouter.AMOUNT_KEY]
    private val destinationAddress: String? = savedStateHandle[SendRouter.DESTINATION_ADDRESS_KEY]
    private val memo: String? = savedStateHandle[SendRouter.TAG_KEY]

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private var innerRouter: InnerSendRouter by Delegates.notNull()
    var stateRouter: StateRouter by Delegates.notNull()
        private set

    private val stateFactory = SendStateFactory(
        clickIntents = this,
        currentStateProvider = Provider { uiState },
        userWalletProvider = Provider { userWallet },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        feeCryptoCurrencyStatusProvider = Provider { feeCryptoCurrencyStatus },
        validateWalletMemoUseCase = validateWalletMemoUseCase,
        isTapHelpPreviewEnabledProvider = Provider { isTapHelpPreviewEnabled },
    )

    private val amountStateFactory = AmountStateFactory(
        currentStateProvider = Provider { uiState },
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
    )

    private val feeStateFactory = FeeStateFactory(
        clickIntents = this,
        currentStateProvider = Provider { uiState },
        feeCryptoCurrencyStatusProvider = Provider { feeCryptoCurrencyStatus },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        isFeeApproximateUseCase = isFeeApproximateUseCase,
    )

    private val eventStateFactory = SendEventStateFactory(
        clickIntents = this,
        currentStateProvider = Provider { uiState },
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        feeStateFactory = feeStateFactory,
    )

    private val feeNotificationFactory = FeeNotificationFactory(
        currentStateProvider = Provider { uiState },
        stateRouterProvider = Provider { stateRouter },
        clickIntents = this,
    )

    private val sendNotificationFactory = SendNotificationFactory(
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        coinCryptoCurrencyStatusProvider = Provider { coinCryptoCurrencyStatus },
        feePaidCryptoCurrencyStatusProvider = Provider { feeCryptoCurrencyStatus },
        currentStateProvider = Provider { uiState },
        userWalletProvider = Provider { userWallet },
        stateRouterProvider = Provider { stateRouter },
        currencyChecksRepository = currencyChecksRepository,
        clickIntents = this,
        analyticsEventHandler = analyticsEventHandler,
        getBalanceNotEnoughForFeeWarningUseCase = getBalanceNotEnoughForFeeWarningUseCase,
    )

    private val sendOnNextScreenAnalyticSender by lazy(LazyThreadSafetyMode.NONE) {
        SendOnNextScreenAnalyticSender(analyticsEventHandler)
    }

    // todo convert to StateFlow
    var uiState: SendUiState by mutableStateOf(stateFactory.getInitialState())
        private set

    private var userWallet: UserWallet by Delegates.notNull()
    private var isAmountSubtractAvailable: Boolean = false
    private var isTapHelpPreviewEnabled: Boolean = false
    private var coinCryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    private var cryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    private var feeCryptoCurrencyStatus: CryptoCurrencyStatus? = null

    private var balanceJobHolder = JobHolder()
    private var balanceHidingJobHolder = JobHolder()
    private var feeJobHolder = JobHolder()
    private var addressValidationJobHolder = JobHolder()
    private var memoValidationJobHolder = JobHolder()
    private var sendNotificationsJobHolder = JobHolder()
    private var feeNotificationsJobHolder = JobHolder()
    private var qrScannerJobHolder = JobHolder()

    private var sendIdleTimer = 0L
    private var feeIdleTimer = 0L

    init {
        subscribeOnCurrencyStatusUpdates()
        subscribeOnBalanceHidden()
        getTapHelpPreviewAvailability()
    }

    override fun onCreate(owner: LifecycleOwner) {
        onStateActive()
    }

    override fun onCleared() {
        super.onCleared()
        balanceHidingJobHolder.cancel()
        balanceJobHolder.cancel()
        stateRouter.clear()
    }

    fun setRouter(router: InnerSendRouter, stateRouter: StateRouter) {
        innerRouter = router
        this.stateRouter = stateRouter
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        viewModelScope.launch(dispatchers.main) {
            getUserWalletUseCase(userWalletId).fold(
                ifRight = { wallet ->
                    userWallet = wallet
                    checkIfSubtractAvailable()

                    val isSingleWalletWithToken = wallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
                    val isMultiCurrency = wallet.isMultiCurrency
                    getCurrenciesStatusUpdates(
                        isSingleWalletWithToken = isSingleWalletWithToken,
                        isMultiCurrency = isMultiCurrency,
                    )
                },
                ifLeft = {
                    uiState = eventStateFactory.getGenericErrorState(
                        onConsume = { uiState = eventStateFactory.onConsumeEventState() },
                    )
                    return@launch
                },
            )
        }
    }

    private fun subscribeOnBalanceHidden() {
        getBalanceHidingSettingsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach {
                uiState = stateFactory.getOnHideBalanceState(isBalanceHidden = it.isBalanceHidden)
            }
            .launchIn(viewModelScope)
            .saveIn(balanceHidingJobHolder)
    }

    private fun getCurrenciesStatusUpdates(isSingleWalletWithToken: Boolean, isMultiCurrency: Boolean) {
        if (cryptoCurrency is CryptoCurrency.Coin) {
            getCurrencyStatusUpdates(
                isSingleWalletWithToken = isSingleWalletWithToken,
                isMultiCurrency = isMultiCurrency,
            ).onEach { currencyStatus ->
                currencyStatus.onRight {
                    onDataLoaded(
                        currencyStatus = it,
                        coinCurrencyStatus = it,
                        feeCurrencyStatus = getFeeCurrencyStatusSync(it, isMultiCurrency),
                    )
                }
            }
                .flowOn(dispatchers.main)
                .launchIn(viewModelScope)
                .saveIn(balanceJobHolder)
        } else {
            combine(
                flow = getCoinCurrencyStatusUpdates(isSingleWalletWithToken),
                flow2 = getCurrencyStatusUpdates(
                    isSingleWalletWithToken = isSingleWalletWithToken,
                    isMultiCurrency = isMultiCurrency,
                ),
            ) { maybeCoinStatus, maybeCurrencyStatus ->
                if (maybeCoinStatus.isRight() && maybeCurrencyStatus.isRight()) {
                    val currencyStatus = maybeCurrencyStatus.getOrElse { error("Currency status is unreachable") }
                    val coinStatus = maybeCoinStatus.getOrElse { error("Coin status is unreachable") }
                    onDataLoaded(
                        currencyStatus = currencyStatus,
                        coinCurrencyStatus = coinStatus,
                        feeCurrencyStatus = getFeeCurrencyStatusSync(currencyStatus, isMultiCurrency),
                    )
                }
            }
                .flowOn(dispatchers.main)
                .launchIn(viewModelScope)
                .saveIn(balanceJobHolder)
        }
    }

    private fun getTapHelpPreviewAvailability() {
        viewModelScope.launch(dispatchers.main) {
            isTapHelpPreviewEnabled = isSendTapHelpEnabledUseCase().getOrElse { false }
        }
    }

    private fun getCoinCurrencyStatusUpdates(isSingleWalletWithToken: Boolean) = getNetworkCoinStatusUseCase(
        userWalletId = userWalletId,
        networkId = cryptoCurrency.network.id,
        derivationPath = cryptoCurrency.network.derivationPath,
        isSingleWalletWithTokens = isSingleWalletWithToken,
    ).conflate().distinctUntilChanged()

    private fun getCurrencyStatusUpdates(
        isSingleWalletWithToken: Boolean,
        isMultiCurrency: Boolean,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return if (isMultiCurrency) {
            getCurrencyStatusUpdatesUseCase(
                userWalletId = userWalletId,
                currencyId = cryptoCurrency.id,
                isSingleWalletWithTokens = isSingleWalletWithToken,
            ).conflate().distinctUntilChanged()
        } else {
            getPrimaryCurrencyStatusUpdatesUseCase(userWalletId = userWalletId)
        }
    }

    private suspend fun getFeeCurrencyStatusSync(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        isMultiCurrency: Boolean,
    ): CryptoCurrencyStatus? {
        return if (isMultiCurrency) {
            getFeePaidCryptoCurrencyStatusSyncUseCase(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            ).getOrNull()
        } else {
            cryptoCurrencyStatus
        }
    }

    private fun createSelectedAppCurrencyFlow(): StateFlow<AppCurrency> {
        return getSelectedAppCurrencyUseCase()
            .map { maybeAppCurrency ->
                maybeAppCurrency.getOrElse { AppCurrency.Default }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = AppCurrency.Default,
            )
    }

    private fun onDataLoaded(
        currencyStatus: CryptoCurrencyStatus,
        coinCurrencyStatus: CryptoCurrencyStatus,
        feeCurrencyStatus: CryptoCurrencyStatus?,
    ) {
        cryptoCurrencyStatus = currencyStatus
        coinCryptoCurrencyStatus = coinCurrencyStatus
        feeCryptoCurrencyStatus = feeCurrencyStatus

        when {
            uiState.sendState?.isSuccess == true -> {
                stateRouter.showSend()
            }
            transactionId != null && amount != null && destinationAddress != null -> {
                loadFee()
                uiState = stateFactory.getReadyState(amount, destinationAddress, memo)
                stateRouter.showSend()
                updateNotifications()
            }
            else -> {
                uiState = stateFactory.getReadyState()
                getWalletsAndRecent()
                stateRouter.showRecipient()
                updateNotifications()
            }
        }
    }

    private fun getWalletsAndRecent() {
        getUserWallets()
        viewModelScope.launch(dispatchers.main) {
            getTxHistory()
        }
    }

    private fun getUserWallets() {
        getWalletsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach { userWallets ->
                coroutineScope {
                    runCatching {
                        userWallets
                            .filterNot { it.walletId == userWalletId || it.isLocked }
                            .map { wallet ->
                                async(dispatchers.io) { wallet.toAvailableWallet() }
                            }.awaitAll()
                    }.onSuccess { result ->
                        uiState = stateFactory.onLoadedWalletsList(wallets = result)
                    }.onFailure {
                        uiState = stateFactory.onLoadedWalletsList(wallets = emptyList())
                    }
                }
            }
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
    }

    private suspend fun UserWallet.toAvailableWallet(): AvailableWallet? {
        return if (!isMultiCurrency) {
            val status = getCryptoCurrencyStatusSyncUseCase(walletId).getOrNull()
            val address = status?.value?.networkAddress.takeIf {
                status?.currency?.network?.id == cryptoCurrency.network.id &&
                    status.currency.network.derivationPath !is Network.DerivationPath.Custom
            }
            address?.let {
                AvailableWallet(
                    name = name,
                    address = it.defaultAddress.value,
                )
            }
        } else {
            val statuses = getCryptoCurrencyStatusesSyncUseCase(walletId).getOrNull()
            val walletCurrency = statuses?.firstOrNull {
                it.currency.network.id == cryptoCurrency.network.id &&
                    it.currency.network.derivationPath !is Network.DerivationPath.Custom
            }
            val address = walletCurrency?.value?.networkAddress
            address?.let {
                AvailableWallet(
                    name = name,
                    address = it.defaultAddress.value,
                )
            }
        }
    }

    private suspend fun getTxHistory() {
        val txHistoryList = getFixedTxHistoryItemsUseCase.getSync(
            userWalletId = userWalletId,
            currency = cryptoCurrency,
        ).getOrElse { emptyList() }
        uiState = stateFactory.onLoadedHistoryList(txHistory = txHistoryList)
    }

    private fun onStateActive() {
        stateRouter.currentState
            .onEach {
                when (it.type) {
                    SendUiStateType.Fee -> loadFee()
                    SendUiStateType.Send -> sendIdleTimer = SystemClock.elapsedRealtime()
                    else -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updateNotifications() {
        sendNotificationFactory.create()
            .conflate()
            .distinctUntilChanged()
            .onEach { uiState = stateFactory.getSendNotificationState(notifications = it) }
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
            .saveIn(sendNotificationsJobHolder)
    }

    private fun updateFeeNotifications() {
        feeNotificationFactory.create()
            .conflate()
            .distinctUntilChanged()
            .onEach { uiState = feeStateFactory.getFeeNotificationState(notifications = it) }
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
            .saveIn(feeNotificationsJobHolder)
    }

    // region screen state navigation
    override fun popBackStack() = stateRouter.popBackStack()
    override fun onBackClick() {
        cancelFeeRequest()
        stateRouter.onBackClick(isSuccess = uiState.sendState?.isSuccess == true)
    }

    override fun onNextClick() {
        val currentState = stateRouter.currentState.value
        sendOnNextScreenAnalyticSender.send(currentState.type, uiState)
        when (currentState.type) {
            SendUiStateType.Fee -> {
                if (onFeeNext()) return
            }
            SendUiStateType.Amount -> {
                if (uiState.feeState?.feeSelectorState is FeeSelectorState.Content) {
                    if (onFeeCoverageAlert()) return
                } else {
                    loadFee(isToNextState = true)
                    return
                }
            }
            else -> Unit
        }

        stateRouter.onNextClick()
    }

    override fun onPrevClick() {
        cancelFeeRequest()
        stateRouter.onPrevClick()
    }

    override fun onQrCodeScanClick() {
        analyticsEventHandler.send(SendAnalyticEvents.QrCodeButtonClicked)
        innerRouter.openQrCodeScanner(cryptoCurrency.network.name)
    }

    override fun onFailedTxEmailClick(errorMessage: String) {
        reduxStateHolder.dispatch(LegacyAction.SendEmailTransactionFailed(errorMessage))
    }

    override fun onTokenDetailsClick(userWalletId: UserWalletId, currency: CryptoCurrency) =
        innerRouter.openTokenDetails(userWalletId, currency)

    private fun onFeeNext(): Boolean {
        if (onFeeCoverageAlert()) return true
        if (checkIfFeeTooLow(uiState)) {
            uiState = eventStateFactory.getFeeTooLowAlert(
                onConsume = { uiState = eventStateFactory.onConsumeEventState() },
            )
            return true
        }
        val feeSelectorState = uiState.feeState?.feeSelectorState as? FeeSelectorState.Content ?: return false
        return checkIfFeeTooHigh(
            feeSelectorState = feeSelectorState,
            onShow = { diff ->
                uiState = eventStateFactory.getFeeTooHighAlert(
                    diff = diff,
                    onConsume = { uiState = eventStateFactory.onConsumeEventState() },
                )
            },
        )
    }

    private fun onFeeCoverageAlert(): Boolean {
        val isFeeCoverage = checkFeeCoverage(uiState, cryptoCurrencyStatus)
        return if (isAmountSubtractAvailable && isFeeCoverage) {
            uiState = eventStateFactory.getFeeCoverageAlert(
                onConsume = { uiState = eventStateFactory.onConsumeEventState() },
            )
            true
        } else {
            analyticsEventHandler.send(SendAnalyticEvents.SubtractFromAmount(false))
            false
        }
    }

    private fun cancelFeeRequest() {
        viewModelScope.launch(dispatchers.main) {
            feeJobHolder.cancel()
        }
    }
    // endregion

    // region amount state clicks
    override fun onCurrencyChangeClick(isFiat: Boolean) {
        uiState = amountStateFactory.getOnCurrencyChangedState(isFiat)
    }

    override fun onAmountValueChange(value: String) {
        uiState = amountStateFactory.getOnAmountValueChange(value)
    }

    override fun onMaxValueClick() {
        uiState = amountStateFactory.getOnMaxAmountClick()
        analyticsEventHandler.send(SendAnalyticEvents.MaxAmountButtonClicked)
    }
    // endregion

    // region recipient state clicks
    fun onQrCodeScanned(address: String) {
        viewModelScope.launch(dispatchers.main) {
            parseQrCodeUseCase(address, cryptoCurrency).fold(
                ifRight = { parsedCode ->
                    onRecipientAddressValueChange(parsedCode.address, EnterAddressSource.QRCode)
                    parsedCode.amount?.let {
                        onAmountValueChange(it.parseBigDecimal(decimals = cryptoCurrency.decimals))
                    }
                    parsedCode.memo?.let { onRecipientMemoValueChange(it) }
                },
                ifLeft = {
                    onRecipientAddressValueChange(address, EnterAddressSource.QRCode)
                    Timber.w(it)
                },
            )
        }.saveIn(qrScannerJobHolder)
    }

    override fun onRecipientAddressValueChange(value: String, type: EnterAddressSource?) {
        viewModelScope.launch(dispatchers.main) {
            if (!checkIfXrpAddressValue(value)) {
                uiState = stateFactory.onRecipientAddressValueChange(value)
                uiState = stateFactory.getOnRecipientAddressValidationStarted()
                val isValidAddress = validateAddress(value)
                uiState = stateFactory.getOnRecipientAddressValidState(value, isValidAddress)
                type?.let { analyticsEventHandler.send(SendAnalyticEvents.AddressEntered(it, isValidAddress)) }
                autoNextFromRecipient(type, isValidAddress)
            }
        }.saveIn(addressValidationJobHolder)
    }

    override fun onRecipientMemoValueChange(value: String) {
        viewModelScope.launch(dispatchers.main) {
            if (!checkIfXrpAddressValue(value)) {
                uiState = stateFactory.getOnRecipientMemoValueChange(value)
                uiState = stateFactory.getOnRecipientAddressValidationStarted()
                val isValidAddress = validateAddress(uiState.recipientState?.addressTextField?.value.orEmpty())
                uiState = stateFactory.getOnRecipientMemoValidState(value, isValidAddress)
            }
        }.saveIn(memoValidationJobHolder)
    }

    private suspend fun validateAddress(value: String): Boolean {
        val isValidAddress = validateWalletAddressUseCase(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
            address = value,
        ).getOrElse { false }
        val isAddressInWallet = cryptoCurrencyStatus.value.networkAddress?.availableAddresses
            ?.any { it.value == value } ?: true
        onEnteredValidAddress(isValidAddress, isAddressInWallet)
        return isValidAddress
    }

    private suspend fun checkIfXrpAddressValue(value: String): Boolean {
        return BlockchainUtils.decodeRippleXAddress(value, cryptoCurrency.network.id.value)?.let { decodedAddress ->
            uiState = stateFactory.onRecipientAddressValueChange(value, isXAddress = true)
            uiState = stateFactory.getOnXAddressMemoState()
            val isValidAddress = validateAddress(decodedAddress.address)
            uiState = stateFactory.getOnRecipientAddressValidState(decodedAddress.address, isValidAddress)
            true
        } ?: false
    }

    private fun onEnteredValidAddress(isValidAddress: Boolean, isAddressInWallet: Boolean) {
        uiState = stateFactory.getHiddenRecentListState(
            isAddressInWallet = isAddressInWallet,
            isValidAddress = isValidAddress,
        )
    }

    private fun autoNextFromRecipient(type: EnterAddressSource?, isValidAddress: Boolean) {
        val isRecent = type == EnterAddressSource.RecentAddress
        if (isRecent && isValidAddress) onNextClick()
    }
    // endregion

    // region fee
    override fun feeReload(isToNextState: Boolean) = loadFee(isToNextState = isToNextState)

    override fun onFeeSelectorClick(feeType: FeeType) {
        uiState = feeStateFactory.onFeeSelectedState(feeType)
        updateFeeNotifications()
        if (feeType == FeeType.Custom) {
            analyticsEventHandler.send(SendAnalyticEvents.CustomFeeButtonClicked)
        }
    }

    override fun onCustomFeeValueChange(index: Int, value: String) {
        uiState = feeStateFactory.onCustomFeeValueChange(index, value)
        updateFeeNotifications()
    }

    override fun onSubtractSelect() {
        uiState = stateFactory.onSubtractSelect(isAmountSubtractAvailable)
        stateRouter.showSend()
        analyticsEventHandler.send(SendAnalyticEvents.SubtractFromAmount(true))
    }

    override fun onReadMoreClick() {
        val locale = if (Locale.getDefault().language == RU_LOCALE) RU_LOCALE else EN_LOCALE
        val url = buildString {
            append(FEE_READ_MORE_URL_FIRST_PART)
            append(locale)
            append(FEE_READ_MORE_URL_SECOND_PART)
        }
        innerRouter.openUrl(url)
    }

    private fun loadFee(isToNextState: Boolean = false) {
        // debouncing fee request
        if (SystemClock.elapsedRealtime() - feeIdleTimer < FEE_UPDATE_DELAY) return

        viewModelScope.launch(dispatchers.main) {
            val isShowStatus = uiState.feeState?.fee == null
            if (isShowStatus) {
                uiState = feeStateFactory.onFeeOnLoadingState()
            }
            val result = callFeeUseCase()?.fold(
                ifRight = {
                    feeIdleTimer = SystemClock.elapsedRealtime()
                    uiState = feeStateFactory.onFeeOnLoadedState(it)
                    if (isToNextState && !onFeeCoverageAlert()) {
                        stateRouter.showSend()
                    }
                },
                ifLeft = {
                    onFeeLoadFailed(isShowStatus, isToNextState)
                },
            )
            if (result == null) {
                onFeeLoadFailed(isShowStatus, isToNextState)
            }
            updateNotifications()
            updateFeeNotifications()
        }.saveIn(feeJobHolder)
            .invokeOnCompletion {
                uiState = amountStateFactory.getOnAmountFeeLoadingCancel()
            }
    }

    private fun onFeeLoadFailed(isShowStatus: Boolean, isToNextState: Boolean) {
        when {
            isToNextState -> {
                uiState = eventStateFactory.getFeeUnreachableErrorState {
                    uiState = eventStateFactory.onConsumeEventState()
                }
            }
            isShowStatus -> uiState = feeStateFactory.onFeeOnErrorState()
        }
    }

    private suspend fun checkIfSubtractAvailable() {
        isAmountSubtractAvailable = isAmountSubtractAvailableUseCase(userWalletId, cryptoCurrency).fold(
            ifRight = { it },
            ifLeft = { false },
        )
    }

    private suspend fun callFeeUseCase(): Either<GetFeeError, TransactionFee>? {
        val amountState = uiState.amountState ?: return null
        val recipientState = uiState.recipientState ?: return null
        val amount = amountState.amountTextField.cryptoAmount.value ?: return null

        return getFeeUseCase.invoke(
            amount = amount,
            destination = recipientState.addressTextField.value,
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        )
    }
    // endregion

    // region send state clicks
    override fun onSendClick() {
        val sendState = uiState.sendState ?: return
        if (sendState.isSuccess) popBackStack()

        uiState = stateFactory.getSendingStateUpdate(isSending = true)
        if (SystemClock.elapsedRealtime() - sendIdleTimer < CHECK_FEE_UPDATE_DELAY) {
            verifyAndSendTransaction()
        } else {
            onCheckFeeUpdate()
            feeIdleTimer = SystemClock.elapsedRealtime()
            sendIdleTimer = SystemClock.elapsedRealtime()
        }
    }

    override fun showAmount() {
        stateRouter.showAmount(isFromConfirmation = true)
        setNeverToShowTapHelp()
        analyticsEventHandler.send(SendAnalyticEvents.ScreenReopened(SendScreenSource.Amount))
    }

    override fun showRecipient() {
        stateRouter.showRecipient(isFromConfirmation = true)
        uiState = stateFactory.getHiddenTapHelpState()
        setNeverToShowTapHelp()
        analyticsEventHandler.send(SendAnalyticEvents.ScreenReopened(SendScreenSource.Address))
    }

    override fun showFee() {
        stateRouter.showFee(isFromConfirmation = true)
        setNeverToShowTapHelp()
        analyticsEventHandler.send(SendAnalyticEvents.ScreenReopened(SendScreenSource.Fee))
    }

    override fun showSend() {
        stateRouter.showSend()
    }

    override fun onExploreClick() {
        val sendState = uiState.sendState ?: return
        analyticsEventHandler.send(SendAnalyticEvents.ExploreButtonClicked)
        innerRouter.openUrl(sendState.txUrl)
    }

    override fun onShareClick() {
        analyticsEventHandler.send(SendAnalyticEvents.ShareButtonClicked)
    }

    override fun onAmountReduceClick(reducedAmount: BigDecimal, clazz: Class<out SendNotification>) {
        uiState = amountStateFactory.getOnAmountValueChange(reducedAmount.parseBigDecimal(cryptoCurrency.decimals))
        uiState = sendNotificationFactory.dismissNotificationState(clazz)
        updateNotifications()
    }

    override fun onNotificationCancel(clazz: Class<out SendNotification>) {
        uiState = sendNotificationFactory.dismissNotificationState(clazz = clazz, isIgnored = true)
    }

    private fun verifyAndSendTransaction() {
        val recipient = uiState.recipientState?.addressTextField?.value ?: return
        val feeState = uiState.feeState ?: return
        val fee = feeState.fee ?: return
        val memo = uiState.recipientState?.memoTextField?.value
        val amountValue = uiState.amountState?.amountTextField?.cryptoAmount?.value ?: return

        viewModelScope.launch(dispatchers.main) {
            createTransactionUseCase(
                amount = amountValue.convertToAmount(cryptoCurrency),
                fee = fee,
                memo = memo,
                destination = recipient,
                userWalletId = userWalletId,
                network = cryptoCurrency.network,
            ).fold(
                ifLeft = {
                    Timber.e(it)
                    uiState = stateFactory.getSendingStateUpdate(isSending = false)
                    uiState = eventStateFactory.getGenericErrorState(
                        error = it,
                        onConsume = { uiState = eventStateFactory.onConsumeEventState() },
                    )
                },
                ifRight = { txData ->
                    sendTransaction(txData)
                },
            )
        }
    }

    private suspend fun sendTransaction(txData: TransactionData) {
        sendTransactionUseCase(
            txData = txData,
            userWallet = userWallet,
            network = cryptoCurrency.network,
        ).fold(
            ifLeft = { error ->
                uiState = stateFactory.getSendingStateUpdate(isSending = false)
                uiState = eventStateFactory.getSendTransactionErrorState(
                    error = error,
                    onConsume = { uiState = eventStateFactory.onConsumeEventState() },
                )
                analyticsEventHandler.send(SendAnalyticEvents.TransactionError(cryptoCurrency.symbol))
            },
            ifRight = {
                uiState = stateFactory.getSendingStateUpdate(isSending = false)
                updateTransactionStatus(txData)
                scheduleBalanceUpdate()
                analyticsEventHandler.send(SendAnalyticEvents.TransactionScreenOpened)
            },
        )
    }

    private suspend fun updateTransactionStatus(txData: TransactionData) {
        val txUrl = getExplorerTransactionUrlUseCase(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
        ).getOrElse { "" }
        uiState = stateFactory.getTransactionSendState(txData, txUrl)
    }

    private fun scheduleBalanceUpdate() {
        viewModelScope.launch(dispatchers.io) {
            delay(BALANCE_UPDATE_DELAY)
            fetchCurrencyStatusUseCase.invoke(
                userWalletId = userWalletId,
                id = cryptoCurrency.id,
                refresh = true,
            )
        }
    }

    private fun onCheckFeeUpdate() {
        val sendState = uiState.sendState ?: return
        val isSuccess = sendState.isSuccess
        val noErrorNotifications = sendState.notifications.none { it is SendNotification.Error }

        if (!isSuccess && noErrorNotifications) {
            viewModelScope.launch(dispatchers.main) {
                val feeUpdatedState = callFeeUseCase()?.fold(
                    ifRight = {
                        uiState = stateFactory.getSendingStateUpdate(isSending = false)
                        eventStateFactory.getFeeUpdatedAlert(
                            fee = it,
                            onConsume = { uiState = eventStateFactory.onConsumeEventState() },
                            onFeeNotIncreased = {
                                uiState = stateFactory.getSendingStateUpdate(isSending = true)
                                verifyAndSendTransaction()
                            },
                        )
                    },
                    ifLeft = {
                        uiState = stateFactory.getSendingStateUpdate(isSending = false)
                        eventStateFactory.getFeeUnreachableErrorState(
                            onConsume = { uiState = eventStateFactory.onConsumeEventState() },
                        )
                    },
                )

                uiState = if (feeUpdatedState != null) {
                    feeUpdatedState
                } else {
                    uiState = stateFactory.getSendingStateUpdate(isSending = false)
                    eventStateFactory.getFeeUnreachableErrorState(
                        onConsume = { uiState = eventStateFactory.onConsumeEventState() },
                    )
                }
            }
        }
    }

    private fun setNeverToShowTapHelp() {
        viewModelScope.launch(dispatchers.main) {
            neverShowTapHelpUseCase()
        }
        uiState = stateFactory.getHiddenTapHelpState()
    }
    // endregion

    private companion object {
        const val CHECK_FEE_UPDATE_DELAY = 60_000L
        const val FEE_UPDATE_DELAY = 10_000L
        const val BALANCE_UPDATE_DELAY = 10_000L

        const val RU_LOCALE = "ru"
        const val EN_LOCALE = "en"
        const val FEE_READ_MORE_URL_FIRST_PART = "https://tangem.com/"
        const val FEE_READ_MORE_URL_SECOND_PART = "/blog/post/what-is-a-transaction-fee-and-why-do-we-need-it/"
    }
}
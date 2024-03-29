package com.tangem.features.send.impl.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.api.AnalyticsEventHandler
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
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.tokens.utils.convertToAmount
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetFixedTxHistoryItemsUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
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
import com.tangem.features.send.impl.presentation.state.fee.*
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
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
    private val getCryptoCurrenciesUseCase: GetCryptoCurrenciesUseCase,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getFixedTxHistoryItemsUseCase: GetFixedTxHistoryItemsUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val validateWalletAddressUseCase: ValidateWalletAddressUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val reduxStateHolder: ReduxStateHolder,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val parseQrCodeUseCase: ParseQrCodeUseCase,
    private val isSendTapHelpEnabledUseCase: IsSendTapHelpEnabledUseCase,
    private val neverShowTapHelpUseCase: NeverShowTapHelpUseCase,
    currencyChecksRepository: CurrencyChecksRepository,
    isFeeApproximateUseCase: IsFeeApproximateUseCase,
    getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
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
        getExplorerTransactionUrlUseCase = getExplorerTransactionUrlUseCase,
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
        feeStateFactory = feeStateFactory,
    )

    private val feeNotificationFactory = FeeNotificationFactory(
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        coinCryptoCurrencyStatusProvider = Provider { coinCryptoCurrencyStatus },
        currentStateProvider = Provider { uiState },
        userWalletProvider = Provider { userWallet },
        stateRouterProvider = Provider { stateRouter },
        clickIntents = this,
        getBalanceNotEnoughForFeeWarningUseCase = getBalanceNotEnoughForFeeWarningUseCase,
        analyticsEventHandler = analyticsEventHandler,
    )

    private val sendNotificationFactory = SendNotificationFactory(
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        coinCryptoCurrencyStatusProvider = Provider { coinCryptoCurrencyStatus },
        currentStateProvider = Provider { uiState },
        userWalletProvider = Provider { userWallet },
        stateRouterProvider = Provider { stateRouter },
        currencyChecksRepository = currencyChecksRepository,
        clickIntents = this,
        analyticsEventHandler = analyticsEventHandler,
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
    private var feeCryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()

    private var balanceJobHolder = JobHolder()
    private var balanceHidingJobHolder = JobHolder()
    private var recipientsJobHolder = JobHolder()
    private var feeJobHolder = JobHolder()
    private var addressValidationJobHolder = JobHolder()
    private var memoValidationJobHolder = JobHolder()
    private var sendNotificationsJobHolder = JobHolder()
    private var feeNotificationsJobHolder = JobHolder()
    private var qrScannerJobHolder = JobHolder()

    private var sendIdleTimer = 0L

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
    ): CryptoCurrencyStatus {
        return if (isMultiCurrency) {
            getFeePaidCryptoCurrencyStatusSyncUseCase(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            ).getOrNull() ?: error("Fee currency is unreachable")
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
        feeCurrencyStatus: CryptoCurrencyStatus,
    ) {
        cryptoCurrencyStatus = currencyStatus
        coinCryptoCurrencyStatus = coinCurrencyStatus
        feeCryptoCurrencyStatus = feeCurrencyStatus

        when {
            uiState.sendState?.isSuccess == true -> {
                stateRouter.showSend()
            }
            transactionId != null && amount != null && destinationAddress != null -> {
                uiState = stateFactory.getReadyState(amount, destinationAddress, memo)
                stateRouter.showFee()
                updateNotifications()
            }
            else -> {
                getWalletsAndRecent()
                uiState = stateFactory.getReadyState()
                stateRouter.showRecipient()
                updateNotifications()
            }
        }
    }

    private fun getWalletsAndRecent() {
        combine(
            flow = getUserWallets().conflate(),
            flow2 = getTxHistory().conflate(),
        ) { wallets, txHistory ->
            uiState = stateFactory.onLoadedRecipientList(
                wallets = wallets,
                txHistory = txHistory,
            )
        }
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(recipientsJobHolder)
    }

    private fun getUserWallets(): Flow<List<AvailableWallet?>> {
        return getWalletsUseCase()
            .distinctUntilChanged()
            .map { userWallets ->
                coroutineScope {
                    userWallets
                        .filterNot { it.walletId == userWalletId || it.isLocked }
                        .map { wallet ->
                            async(dispatchers.io) {
                                getCryptoCurrenciesUseCase.getSync(wallet.walletId)
                                    .fold(
                                        ifRight = { currencyItem ->
                                            val walletCurrency = currencyItem.firstOrNull {
                                                it.network.id == cryptoCurrency.network.id
                                            } ?: return@fold null
                                            val addresses = walletManagersFacade.getAddress(
                                                userWalletId = wallet.walletId,
                                                network = walletCurrency.network,
                                            )
                                            return@fold addresses.firstOrNull()?.let {
                                                AvailableWallet(
                                                    name = wallet.name,
                                                    address = it.value,
                                                )
                                            }
                                        },
                                        ifLeft = { null },
                                    )
                            }
                        }
                }.awaitAll()
            }
    }

    private fun getTxHistory(): Flow<List<TxHistoryItem>> {
        return getFixedTxHistoryItemsUseCase(
            userWalletId = userWalletId,
            currency = cryptoCurrency,
        ).fold(
            ifRight = { it.distinctUntilChanged() },
            ifLeft = { emptyFlow() },
        )
    }

    private fun onStateActive() {
        stateRouter.currentState
            .onEach {
                when (it.type) {
                    SendUiStateType.Fee -> loadFee()
                    SendUiStateType.Send -> sendIdleTimer = System.currentTimeMillis()
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
            .flowOn(dispatchers.io)
            .launchIn(viewModelScope)
            .saveIn(feeNotificationsJobHolder)
    }

    // region screen state navigation
    override fun popBackStack() = stateRouter.popBackStack()
    override fun onBackClick() = stateRouter.onBackClick(isSuccess = uiState.sendState?.isSuccess == true)
    override fun onNextClick() {
        val currentState = stateRouter.currentState.value
        val isCurrentFee = currentState.type == SendUiStateType.Fee
        if (isCurrentFee) {
            val isFeeCoverage = checkFeeCoverage(uiState, cryptoCurrencyStatus)
            if (isAmountSubtractAvailable && isFeeCoverage) {
                uiState = eventStateFactory.getFeeCoverageAlert(
                    onConsume = { uiState = eventStateFactory.onConsumeEventState() },
                )
                return
            } else {
                analyticsEventHandler.send(SendAnalyticEvents.SubtractFromAmount(false))
            }
            if (checkIfFeeTooLow(uiState)) {
                uiState = eventStateFactory.getFeeTooLowAlert(
                    onConsume = { uiState = eventStateFactory.onConsumeEventState() },
                )
                return
            }
        }

        sendOnNextScreenAnalyticSender.send(currentState.type, uiState)
        stateRouter.onNextClick()
    }

    override fun onPrevClick() = stateRouter.onPrevClick()

    override fun onQrCodeScanClick() {
        analyticsEventHandler.send(SendAnalyticEvents.QrCodeButtonClicked)
        innerRouter.openQrCodeScanner(cryptoCurrency.network.name)
    }

    override fun onFailedTxEmailClick(errorMessage: String) {
        reduxStateHolder.dispatch(LegacyAction.SendEmailTransactionFailed(errorMessage))
    }

    override fun onTokenDetailsClick(userWalletId: UserWalletId, currency: CryptoCurrency) =
        innerRouter.openTokenDetails(userWalletId, currency)
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
    fun onRecipientAddressScanned(address: String) {
        viewModelScope.launch(dispatchers.main) {
            parseQrCodeUseCase(address, cryptoCurrency).fold(
                ifRight = { parsedCode ->
                    onRecipientAddressValueChange(parsedCode.address, EnterAddressSource.QRCode)
                    parsedCode.amount?.let { onAmountValueChange(it.toPlainString()) }
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
        onEnteredValidAddress(isValidAddress)
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

    private fun onEnteredValidAddress(isValidAddress: Boolean) {
        val recipientState = uiState.recipientState ?: return
        uiState = uiState.copy(
            recipientState = recipientState.copy(
                recent = recipientState.recent.map { it.copy(isVisible = !isValidAddress) }.toPersistentList(),
                wallets = recipientState.wallets.map { it.copy(isVisible = !isValidAddress) }.toPersistentList(),
            ),
        )
    }

    private fun autoNextFromRecipient(type: EnterAddressSource?, isValidAddress: Boolean) {
        val isRecent = type == EnterAddressSource.RecentAddress
        val isAddressOnly = uiState.recipientState?.memoTextField == null
        if (isRecent && isAddressOnly && isValidAddress) onNextClick()
    }
    // endregion

    // region fee
    override fun feeReload() = loadFee()

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

    private fun loadFee() {
        viewModelScope.launch(dispatchers.main) {
            if (uiState.feeState?.fee == null) {
                uiState = feeStateFactory.onFeeOnLoadingState()
            }
            uiState = callFeeUseCase()?.fold(
                ifRight = feeStateFactory::onFeeOnLoadedState,
                ifLeft = { feeStateFactory.onFeeOnErrorState() },
            ) ?: feeStateFactory.onFeeOnErrorState()
            updateFeeNotifications()
        }.saveIn(feeJobHolder)
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
        if (System.currentTimeMillis() - sendIdleTimer < CHECK_FEE_UPDATE_DELAY) {
            verifyAndSendTransaction()
        } else {
            onCheckFeeUpdate()
        }
        sendIdleTimer = System.currentTimeMillis()
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

    override fun onAmountReduceClick(reducedAmount: String, clazz: Class<out SendNotification>) {
        uiState = amountStateFactory.getOnAmountValueChange(reducedAmount)
        uiState = sendNotificationFactory.dismissNotificationState(clazz)
        loadFee()
    }

    override fun onNotificationCancel(clazz: Class<out SendNotification>) {
        uiState = sendNotificationFactory.dismissNotificationState(clazz)
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
                uiState = stateFactory.getTransactionSendState(txData)
                scheduleBalanceUpdate()
                analyticsEventHandler.send(SendAnalyticEvents.TransactionScreenOpened)
            },
        )
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
                        eventStateFactory.getGenericErrorState(
                            error = (it as? GetFeeError.DataError)?.cause,
                            onConsume = { uiState = eventStateFactory.onConsumeEventState() },
                        )
                    },
                )

                uiState = if (feeUpdatedState != null) {
                    feeUpdatedState
                } else {
                    uiState = stateFactory.getSendingStateUpdate(isSending = false)
                    eventStateFactory.getGenericErrorState(
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

    companion object {
        private const val CHECK_FEE_UPDATE_DELAY = 60_000L
        private const val BALANCE_UPDATE_DELAY = 10_000L

        private const val RU_LOCALE = "ru"
        private const val EN_LOCALE = "en"
        private const val FEE_READ_MORE_URL_FIRST_PART = "https://tangem.com/"
        private const val FEE_READ_MORE_URL_SECOND_PART = "/blog/post/what-is-a-transaction-fee-and-why-do-we-need-it/"
    }
}

package com.tangem.features.send.impl.presentation.viewmodel

import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.*
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.bundle.unbundle
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
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
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.ValidateAddressError
import com.tangem.domain.transaction.usecase.*
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.domain.txhistory.usecase.GetFixedTxHistoryItemsUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.send.impl.navigation.InnerSendRouter
import com.tangem.features.send.impl.presentation.analytics.EnterAddressSource
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents
import com.tangem.features.send.impl.presentation.analytics.SendScreenSource
import com.tangem.features.send.impl.presentation.analytics.utils.SendScreenAnalyticSender
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.errors.FeeErrorStateMapper
import com.tangem.features.send.impl.presentation.state.*
import com.tangem.features.send.impl.presentation.state.amount.AmountStateFactory
import com.tangem.features.send.impl.presentation.state.confirm.SendNotificationFactory
import com.tangem.features.send.impl.presentation.state.fee.*
import com.tangem.features.send.impl.presentation.state.recipient.RecipientSendFactory
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val getCryptoCurrencyStatusSyncUseCase: GetCryptoCurrencyStatusSyncUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getCryptoCurrencyUseCase: GetCryptoCurrencyUseCase,
    private val getNetworkAddressesUseCase: GetNetworkAddressesUseCase,
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
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val updateDelayedCurrencyStatusUseCase: UpdateDelayedNetworkStatusUseCase,
    private val fetchPendingTransactionsUseCase: FetchPendingTransactionsUseCase,
    private val isUtxoConsolidationAvailableUseCase: IsUtxoConsolidationAvailableUseCase,
    private val validateWalletMemoUseCase: ValidateWalletMemoUseCase,
    @DelayedWork private val coroutineScope: CoroutineScope,
    validateTransactionUseCase: ValidateTransactionUseCase,
    currencyChecksRepository: CurrencyChecksRepository,
    isFeeApproximateUseCase: IsFeeApproximateUseCase,
    getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, SendClickIntents {

    private val userWalletId: UserWalletId = savedStateHandle.get<Bundle>(AppRoute.Send.USER_WALLET_ID_KEY)
        ?.unbundle(UserWalletId.serializer())
        ?: error("This screen can't open without `UserWalletId`")

    private val cryptoCurrency: CryptoCurrency = savedStateHandle.get<Bundle>(AppRoute.Send.CRYPTO_CURRENCY_KEY)
        ?.unbundle(CryptoCurrency.serializer())
        ?: error("This screen can't open without `CryptoCurrency`")

    private val transactionId: String? = savedStateHandle[AppRoute.Send.TRANSACTION_ID_KEY]
    private val amount: String? = savedStateHandle[AppRoute.Send.AMOUNT_KEY]
    private val destinationAddress: String? = savedStateHandle[AppRoute.Send.DESTINATION_ADDRESS_KEY]
    private val memo: String? = savedStateHandle[AppRoute.Send.TAG_KEY]

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private var innerRouter: InnerSendRouter by Delegates.notNull()
    var stateRouter: StateRouter by Delegates.notNull()
        private set

    private val feeErrorHandler = FeeErrorStateMapper()

    private val stateFactory = SendStateFactory(
        clickIntents = this,
        stateRouterProvider = Provider { stateRouter },
        currentStateProvider = Provider { uiState.value },
        userWalletProvider = Provider { userWallet },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        feeCryptoCurrencyStatusProvider = Provider { feeCryptoCurrencyStatus },
        isTapHelpPreviewEnabledProvider = Provider { isTapHelpPreviewEnabled },
    )

    private val recipientStateFactory = RecipientSendFactory(
        stateRouterProvider = Provider { stateRouter },
        currentStateProvider = Provider { uiState.value },
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        isUtxoConsolidationAvailableProvider = Provider { isUtxoConsolidationAvailable },
        validateWalletMemoUseCase = validateWalletMemoUseCase,
    )

    private val amountStateFactory = AmountStateFactory(
        stateRouterProvider = Provider { stateRouter },
        currentStateProvider = Provider { uiState.value },
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
    )

    private val feeStateFactory = FeeStateFactory(
        clickIntents = this,
        stateRouterProvider = Provider { stateRouter },
        currentStateProvider = Provider { uiState.value },
        feeCryptoCurrencyStatusProvider = Provider { feeCryptoCurrencyStatus },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        isFeeApproximateUseCase = isFeeApproximateUseCase,
    )

    private val eventStateFactory = SendEventStateFactory(
        clickIntents = this,
        stateRouterProvider = Provider { stateRouter },
        currentStateProvider = Provider { uiState.value },
        feeStateFactory = feeStateFactory,
    )

    private val feeNotificationFactory = FeeNotificationFactory(
        currentStateProvider = Provider { uiState.value },
        stateRouterProvider = Provider { stateRouter },
        clickIntents = this,
    )

    private val sendNotificationFactory = SendNotificationFactory(
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        feeCryptoCurrencyStatusProvider = Provider { feeCryptoCurrencyStatus },
        currentStateProvider = Provider { uiState.value },
        userWalletProvider = Provider { userWallet },
        stateRouterProvider = Provider { stateRouter },
        isSubtractAvailableProvider = Provider { isAmountSubtractAvailable },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        currencyChecksRepository = currencyChecksRepository,
        clickIntents = this,
        analyticsEventHandler = analyticsEventHandler,
        getBalanceNotEnoughForFeeWarningUseCase = getBalanceNotEnoughForFeeWarningUseCase,
        validateTransactionUseCase = validateTransactionUseCase,
    )

    private val sendScreenAnalyticSender by lazy(LazyThreadSafetyMode.NONE) {
        SendScreenAnalyticSender(
            stateRouterProvider = Provider { stateRouter },
            currentStateProvider = Provider { uiState.value },
            analyticsEventHandler = analyticsEventHandler,
            cryptoCurrencyProvider = Provider { cryptoCurrency },
        )
    }

    // todo convert to StateFlow
    val uiState: MutableStateFlow<SendUiState> = MutableStateFlow(
        value = stateFactory.getInitialState(),
    )

    private var userWallet: UserWallet by Delegates.notNull()
    private var userWallets: List<AvailableWallet> = emptyList()
    private var isAmountSubtractAvailable: Boolean = false
    private var isUtxoConsolidationAvailable: Boolean = false
    private var isTapHelpPreviewEnabled: Boolean = false
    private var cryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    private var feeCryptoCurrencyStatus: CryptoCurrencyStatus? = null

    private var balanceJobHolder = JobHolder()
    private var balanceHidingJobHolder = JobHolder()
    private var feeJobHolder = JobHolder()
    private var addressValidationJobHolder = JobHolder()
    private var memoValidationJobHolder = JobHolder()
    private var sendNotificationsJobHolder = JobHolder()
    private var feeNotificationsJobHolder = JobHolder()

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

    private fun subscribeOnQRScannerResult() {
        listenToQrScanningUseCase(SourceType.SEND)
            .getOrElse { emptyFlow() }
            .onEach(::onQrCodeScanned)
            .launchIn(viewModelScope)
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        viewModelScope.launch {
            getUserWalletUseCase(userWalletId).fold(
                ifRight = { wallet ->
                    userWallet = wallet
                    checkIfSubtractAvailable()
                    checkIfUtxoConsolidationAvailable()

                    val isSingleWalletWithToken = wallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
                    val isMultiCurrency = wallet.isMultiCurrency
                    getCurrenciesStatusUpdates(
                        isSingleWalletWithToken = isSingleWalletWithToken,
                        isMultiCurrency = isMultiCurrency,
                    )
                },
                ifLeft = {
                    showErrorAlert()
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
                uiState.value = stateFactory.getOnHideBalanceState(isBalanceHidden = it.isBalanceHidden)
            }
            .launchIn(viewModelScope)
            .saveIn(balanceHidingJobHolder)
    }

    private suspend fun getCurrenciesStatusUpdates(isSingleWalletWithToken: Boolean, isMultiCurrency: Boolean) {
        getCurrencyStatus(
            isSingleWalletWithToken = isSingleWalletWithToken,
            isMultiCurrency = isMultiCurrency,
        ).fold(
            ifRight = { cryptoCurrencyStatus ->
                onDataLoaded(
                    currencyStatus = cryptoCurrencyStatus,
                    feeCurrencyStatus = getFeeCurrencyStatusSync(cryptoCurrencyStatus, isMultiCurrency),
                )
            },
            ifLeft = { showErrorAlert() },
        )
    }

    private fun getTapHelpPreviewAvailability() {
        viewModelScope.launch {
            isTapHelpPreviewEnabled = isSendTapHelpEnabledUseCase().getOrElse { false }
        }
    }

    private suspend fun getCurrencyStatus(
        isSingleWalletWithToken: Boolean,
        isMultiCurrency: Boolean,
    ): Either<CurrencyStatusError, CryptoCurrencyStatus> {
        return if (isMultiCurrency) {
            getCryptoCurrencyStatusSyncUseCase(
                userWalletId = userWalletId,
                cryptoCurrencyId = cryptoCurrency.id,
                isSingleWalletWithTokens = isSingleWalletWithToken,
            )
        } else {
            getCryptoCurrencyStatusSyncUseCase(userWalletId = userWalletId)
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

    private fun onDataLoaded(currencyStatus: CryptoCurrencyStatus, feeCurrencyStatus: CryptoCurrencyStatus?) {
        cryptoCurrencyStatus = currencyStatus
        feeCryptoCurrencyStatus = feeCurrencyStatus
        subscribeOnQRScannerResult()
        when {
            uiState.value.sendState?.isSuccess == true -> return
            transactionId != null && amount != null && destinationAddress != null -> {
                loadFee()
                uiState.value = stateFactory.getReadyState(amount, destinationAddress, memo)
                stateRouter.showSend()
                updateNotifications()
            }
            else -> {
                uiState.value = stateFactory.getReadyState()
                getWalletsAndRecent()
                stateRouter.showRecipient()
                updateNotifications()
            }
        }
    }

    private fun getWalletsAndRecent() {
        getUserWallets()
        viewModelScope.launch {
            getTxHistory()
        }
    }

    private fun getUserWallets() {
        viewModelScope.launch {
            runCatching {
                waitForDelay(delay = RECENT_LOAD_DELAY) {
                    getWalletsUseCase.invokeSync()
                        .toAvailableWallets()
                }
            }.onSuccess { result ->
                userWallets = result
                uiState.value = recipientStateFactory.onLoadedWalletsList(wallets = userWallets)
            }.onFailure {
                uiState.value = recipientStateFactory.onLoadedWalletsList(wallets = emptyList())
            }
        }
    }

    private suspend fun List<UserWallet>.toAvailableWallets(): List<AvailableWallet> {
        return filterNot { it.isLocked }
            .mapNotNull { wallet ->
                val addresses = if (!wallet.isMultiCurrency) {
                    getCryptoCurrencyUseCase(wallet.walletId).getOrNull()?.let {
                        if (it.network.id == cryptoCurrency.network.id) {
                            getNetworkAddressesUseCase.invokeSync(wallet.walletId, it.network)
                        } else {
                            null
                        }
                    }
                } else {
                    getNetworkAddressesUseCase.invokeSync(wallet.walletId, cryptoCurrency.network)
                }
                addresses?.map { (cryptoCurrency, address) ->
                    AvailableWallet(
                        name = wallet.name,
                        address = address,
                        cryptoCurrency = cryptoCurrency,
                        userWalletId = wallet.walletId,
                    )
                }
            }.flatten()
    }

    private suspend fun getTxHistory() {
        val txHistoryList = waitForDelay(delay = RECENT_LOAD_DELAY) {
            getFixedTxHistoryItemsUseCase.getSync(
                userWalletId = userWalletId,
                currency = cryptoCurrency,
                pageSize = RECENT_TX_SIZE,
            ).getOrElse { emptyList() }
        }
        uiState.value = recipientStateFactory.onLoadedHistoryList(txHistory = txHistoryList)
    }

    private fun onStateActive() {
        stateRouter.currentState
            .onEach {
                when (it.type) {
                    SendUiStateType.Fee,
                    SendUiStateType.EditFee,
                    -> loadFee()
                    SendUiStateType.Send -> {
                        uiState.value = stateFactory.getIsAmountSubtractedState(isAmountSubtractAvailable)
                        sendIdleTimer = SystemClock.elapsedRealtime()
                    }
                    else -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updateNotifications() {
        sendNotificationFactory.create()
            .conflate()
            .distinctUntilChanged()
            .onEach { uiState.value = stateFactory.getSendNotificationState(notifications = it) }
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
            .saveIn(sendNotificationsJobHolder)
    }

    private fun updateFeeNotifications() {
        feeNotificationFactory.create()
            .conflate()
            .distinctUntilChanged()
            .onEach { uiState.value = feeStateFactory.getFeeNotificationState(notifications = it) }
            .flowOn(dispatchers.main)
            .launchIn(viewModelScope)
            .saveIn(feeNotificationsJobHolder)
    }

    // region screen state navigation
    override fun popBackStack() = stateRouter.popBackStack()
    override fun onBackClick() {
        cancelFeeRequest()
        stateRouter.onBackClick(isSuccess = uiState.value.sendState?.isSuccess == true)
    }

    override fun onCloseClick() {
        sendScreenAnalyticSender.sendOnClose()
        when (stateRouter.currentState.value.type) {
            SendUiStateType.EditAmount,
            SendUiStateType.EditFee,
            SendUiStateType.EditRecipient,
            -> onBackClick()
            else -> popBackStack()
        }
    }

    override fun onNextClick(isFromEdit: Boolean) {
        val currentState = stateRouter.currentState.value
        uiState.value = stateFactory.syncEditStates(isFromEdit = isFromEdit)
        sendScreenAnalyticSender.send(currentState.type, uiState.value)
        when (currentState.type) {
            SendUiStateType.Fee,
            SendUiStateType.EditFee,
            -> if (onFeeNext()) return
            SendUiStateType.Amount,
            SendUiStateType.EditAmount,
            -> loadFee()
            else -> Unit
        }

        stateRouter.onNextClick()
    }

    override fun onAmountNext() = onNextClick(stateRouter.isEditState)

    override fun onPrevClick() {
        cancelFeeRequest()
        stateRouter.onPrevClick()
    }

    override fun onQrCodeScanClick() {
        analyticsEventHandler.send(SendAnalyticEvents.QrCodeButtonClicked)
        innerRouter.openQrCodeScanner(cryptoCurrency.network.name)
    }

    override fun onFailedTxEmailClick(errorMessage: String) {
        val recipient = uiState.value.recipientState?.addressTextField?.value
        val feeValue = uiState.value.feeState?.fee?.amount?.value
        val amountValue = (uiState.value.amountState as? AmountState.Data)?.amountTextField?.cryptoAmount?.value

        val receivingAmount = if (amountValue != null && feeValue != null) {
            checkAndCalculateSubtractedAmount(
                isAmountSubtractAvailable = isAmountSubtractAvailable,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                amountValue = amountValue,
                feeValue = feeValue,
                reduceAmountBy = uiState.value.sendState?.reduceAmountBy ?: BigDecimal.ZERO,
            )
        } else {
            null
        }
        reduxStateHolder.dispatch(
            LegacyAction.SendEmailTransactionFailed(
                cryptoCurrency = cryptoCurrency,
                userWalletId = userWalletId,
                amount = receivingAmount,
                fee = feeValue,
                destinationAddress = recipient,
                errorMessage = errorMessage,
                scanResponse = userWallet.scanResponse,
            ),
        )
    }

    override fun onTokenDetailsClick(userWalletId: UserWalletId, currency: CryptoCurrency) =
        innerRouter.openTokenDetails(userWalletId, currency)

    private fun onFeeNext(): Boolean {
        val feeState = uiState.value.getFeeState(stateRouter.isEditState)
        val feeSelectorState = feeState?.feeSelectorState as? FeeSelectorState.Content ?: return false
        if (checkIfFeeTooLow(feeSelectorState)) {
            uiState.value = eventStateFactory.getFeeTooLowAlert(
                onConsume = { uiState.value = eventStateFactory.onConsumeEventState() },
            )
            return true
        }
        return checkIfFeeTooHigh(
            feeSelectorState = feeSelectorState,
            onShow = { diff ->
                uiState.value = eventStateFactory.getFeeTooHighAlert(
                    diff = diff,
                    onConsume = { uiState.value = eventStateFactory.onConsumeEventState() },
                )
            },
        )
    }

    private fun cancelFeeRequest() {
        viewModelScope.launch {
            feeJobHolder.cancel()
        }
    }

    private fun onQrCodeScanned(address: String) {
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
    }

// endregion

    // region amount state clicks
    override fun onCurrencyChangeClick(isFiat: Boolean) {
        uiState.value = amountStateFactory.getOnCurrencyChangedState(isFiat)
    }

    override fun onAmountValueChange(value: String) {
        uiState.value = amountStateFactory.getOnAmountValueChange(value)
    }

    override fun onMaxValueClick() {
        uiState.value = amountStateFactory.getOnMaxAmountClick()
        analyticsEventHandler.send(SendAnalyticEvents.MaxAmountButtonClicked)
    }

    override fun onAmountPasteTriggerDismiss() {
        uiState.value = amountStateFactory.getOnAmountPastedTriggerDismiss()
    }
// endregion

// region recipient state clicks

    override fun onRecipientAddressValueChange(value: String, type: EnterAddressSource?) {
        viewModelScope.launch {
            if (!checkIfXrpAddressValue(value)) {
                uiState.value = recipientStateFactory.onRecipientAddressValueChange(value, isValuePasted = type != null)
                uiState.value = recipientStateFactory.getOnRecipientAddressValidationStarted()
                val isValidAddress = validateAddress(value)
                uiState.value = recipientStateFactory.getOnRecipientAddressValidState(value, isValidAddress)
                type?.let {
                    analyticsEventHandler.send(
                        SendAnalyticEvents.AddressEntered(
                            it,
                            isValidAddress.isRight(),
                        ),
                    )
                }
                autoNextFromRecipient(type, isValidAddress.isRight())
            }
        }.saveIn(addressValidationJobHolder)
    }

    override fun onRecipientMemoValueChange(value: String, isValuePasted: Boolean) {
        viewModelScope.launch {
            if (!checkIfXrpAddressValue(value)) {
                uiState.value = recipientStateFactory.getOnRecipientMemoValueChange(value, isValuePasted)
                uiState.value = recipientStateFactory.getOnRecipientAddressValidationStarted()
                val recipientState = uiState.value.getRecipientState(stateRouter.isEditState)
                val maybeValidAddress = validateAddress(recipientState?.addressTextField?.value.orEmpty())
                uiState.value = recipientStateFactory.getOnRecipientMemoValidState(value, maybeValidAddress.isRight())
            }
        }.saveIn(memoValidationJobHolder)
    }

    private suspend fun validateAddress(value: String): Either<ValidateAddressError, Unit> = runCatching {
        val maybeValidAddress = validateWalletAddressUseCase(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
            address = value,
            currencyAddress = cryptoCurrencyStatus.value.networkAddress?.availableAddresses,
        )
        onEnteredValidAddress(maybeValidAddress.isLeft())
        maybeValidAddress
    }.getOrElse { ValidateAddressError.DataError(it).left() }

    private fun validateMemo(value: String?): Boolean {
        return value?.let { validateWalletMemoUseCase(cryptoCurrency.network, it).getOrElse { false } } ?: true
    }

    private suspend fun checkIfXrpAddressValue(value: String): Boolean {
        return BlockchainUtils.decodeRippleXAddress(value, cryptoCurrency.network.id.value)?.let { decodedAddress ->
            uiState.value =
                recipientStateFactory.onRecipientAddressValueChange(value, isXAddress = true, isValuePasted = true)
            uiState.value = recipientStateFactory.getOnXAddressMemoState()
            val isValidAddress = validateAddress(decodedAddress.address)
            uiState.value =
                recipientStateFactory.getOnRecipientAddressValidState(decodedAddress.address, isValidAddress)
            true
        } ?: false
    }

    private fun onEnteredValidAddress(isNotValid: Boolean) {
        uiState.value = recipientStateFactory.getHiddenRecentListState(isNotValid = isNotValid)
    }

    private fun autoNextFromRecipient(type: EnterAddressSource?, isValidAddress: Boolean) {
        val memo = uiState.value.getRecipientState(stateRouter.isEditState)?.memoTextField?.value
        val isValidMemo = validateMemo(memo)

        val isRecent = type == EnterAddressSource.RecentAddress
        if (isRecent && isValidAddress && isValidMemo) onNextClick(stateRouter.isEditState)
    }
// endregion

    // region fee
    override fun feeReload() = loadFee()

    override fun onFeeSelectorClick(feeType: FeeType) {
        uiState.value = feeStateFactory.onFeeSelectedState(feeType)
        updateFeeNotifications()
        if (feeType == FeeType.Custom) {
            analyticsEventHandler.send(SendAnalyticEvents.CustomFeeButtonClicked)
        }
    }

    override fun onCustomFeeValueChange(index: Int, value: String) {
        uiState.value = feeStateFactory.onCustomFeeValueChange(index, value)
        updateFeeNotifications()
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
        viewModelScope.launch {
            val isShowStatus = uiState.value.feeState?.fee == null
            if (isShowStatus) {
                uiState.value = feeStateFactory.onFeeOnLoadingState()
                updateNotifications()
            }
            val result = callFeeUseCase()?.fold(
                ifRight = {
                    uiState.value = feeStateFactory.onFeeOnLoadedState(it)
                    sendIdleTimer = SystemClock.elapsedRealtime()
                },
                ifLeft = { loadFeeError ->
                    onFeeLoadFailed(isShowStatus, loadFeeError)
                },
            )
            if (result == null) {
                onFeeLoadFailed(isShowStatus, null)
            }
            updateNotifications()
            updateFeeNotifications()
        }.saveIn(feeJobHolder)
    }

    private fun onFeeLoadFailed(isShowStatus: Boolean, loadFeeError: GetFeeError?) {
        if (isShowStatus) {
            uiState.value = feeStateFactory.onFeeOnErrorState(
                feeErrorHandler.getFeeError(loadFeeError, cryptoCurrency.name),
            )
        }
    }

    private suspend fun checkIfSubtractAvailable() {
        isAmountSubtractAvailable = isAmountSubtractAvailableUseCase(userWalletId, cryptoCurrency).fold(
            ifRight = { it },
            ifLeft = { false },
        )
    }

    private suspend fun checkIfUtxoConsolidationAvailable() {
        isUtxoConsolidationAvailable = isUtxoConsolidationAvailableUseCase.invokeSync(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
        )
    }

    private suspend fun callFeeUseCase(): Either<GetFeeError, TransactionFee>? {
        val isFromConfirmation = stateRouter.currentState.value.isFromConfirmation
        val amountState = uiState.value.getAmountState(isFromConfirmation) as? AmountState.Data ?: return null
        val recipientState = uiState.value.getRecipientState(isFromConfirmation) ?: return null
        val amount = amountState.amountTextField.cryptoAmount.value ?: return null

        return getFeeUseCase.invoke(
            amount = amount,
            destination = recipientState.addressTextField.value,
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrencyStatus.currency,
        )
    }
// endregion

    // region send state clicks
    override fun onSendClick() {
        val sendState = uiState.value.sendState ?: return
        if (sendState.isSuccess) popBackStack()

        uiState.value = stateFactory.getSendingStateUpdate(isSending = true)
        if (SystemClock.elapsedRealtime() - sendIdleTimer < CHECK_FEE_UPDATE_DELAY) {
            verifyAndSendTransaction()
        } else {
            onCheckFeeUpdate()
            sendIdleTimer = SystemClock.elapsedRealtime()
        }
    }

    override fun showAmount() {
        uiState.value = stateFactory.syncEditStates(isFromEdit = false)
        stateRouter.showAmount(isFromConfirmation = true)
        setNeverToShowTapHelp()
        analyticsEventHandler.send(SendAnalyticEvents.ScreenReopened(SendScreenSource.Amount))
    }

    override fun showRecipient() {
        uiState.value = stateFactory.syncEditStates(isFromEdit = false)
        stateRouter.showRecipient(isFromConfirmation = true)
        uiState.value = stateFactory.getHiddenTapHelpState()
        setNeverToShowTapHelp()
        analyticsEventHandler.send(SendAnalyticEvents.ScreenReopened(SendScreenSource.Address))
    }

    override fun showFee() {
        uiState.value = stateFactory.syncEditStates(isFromEdit = false)
        stateRouter.showFee(isFromConfirmation = true)
        setNeverToShowTapHelp()
        analyticsEventHandler.send(SendAnalyticEvents.ScreenReopened(SendScreenSource.Fee))
    }

    override fun showSend() {
        stateRouter.showSend()
    }

    override fun onExploreClick() {
        val sendState = uiState.value.sendState ?: return
        analyticsEventHandler.send(SendAnalyticEvents.ExploreButtonClicked)
        innerRouter.openUrl(sendState.txUrl)
    }

    override fun onShareClick() {
        analyticsEventHandler.send(SendAnalyticEvents.ShareButtonClicked)
    }

    override fun onAmountReduceClick(
        reduceAmountBy: BigDecimal?,
        reduceAmountByDiff: BigDecimal?,
        reduceAmountTo: BigDecimal?,
        clazz: Class<out SendNotification>,
    ) {
        uiState.value = when {
            reduceAmountBy != null && reduceAmountByDiff != null -> amountStateFactory.getOnAmountReduceByState(
                reduceAmountBy = reduceAmountBy,
                reduceAmountByDiff = reduceAmountByDiff,
            )
            reduceAmountTo != null -> amountStateFactory.getOnAmountReduceToState(reduceAmountTo)
            else -> return
        }

        uiState.value = sendNotificationFactory.dismissNotificationState(clazz)
        updateNotifications()
    }

    override fun onNotificationCancel(clazz: Class<out SendNotification>) {
        uiState.value = sendNotificationFactory.dismissNotificationState(clazz = clazz, isIgnored = true)
    }

    private fun verifyAndSendTransaction() {
        val recipient = uiState.value.recipientState?.addressTextField?.value ?: return
        val feeState = uiState.value.feeState ?: return
        val fee = feeState.fee ?: return
        val memo = uiState.value.recipientState?.memoTextField?.value
        val amountValue = (uiState.value.amountState as? AmountState.Data)
            ?.amountTextField?.cryptoAmount?.value
            ?: return
        val feeValue = fee.amount.value ?: return

        val receivingAmount = checkAndCalculateSubtractedAmount(
            isAmountSubtractAvailable = isAmountSubtractAvailable,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            amountValue = amountValue,
            feeValue = feeValue,
            reduceAmountBy = uiState.value.sendState?.reduceAmountBy ?: BigDecimal.ZERO,
        )

        viewModelScope.launch {
            createTransactionUseCase(
                amount = receivingAmount.convertToSdkAmount(cryptoCurrency),
                fee = fee,
                memo = memo,
                destination = recipient,
                userWalletId = userWalletId,
                network = cryptoCurrency.network,
            ).fold(
                ifLeft = {
                    Timber.e(it)
                    uiState.value = stateFactory.getSendingStateUpdate(isSending = false)
                    uiState.value = eventStateFactory.getGenericErrorState(
                        error = it,
                        onConsume = { uiState.value = eventStateFactory.onConsumeEventState() },
                    )
                },
                ifRight = { txData ->
                    sendTransaction(txData)
                },
            )
        }
    }

    private suspend fun sendTransaction(txData: TransactionData.Uncompiled) {
        val result = sendTransactionUseCase(
            txData = txData,
            userWallet = userWallet,
            network = cryptoCurrency.network,
        )

        uiState.value = stateFactory.getSendingStateUpdate(isSending = false)

        result.fold(
            ifLeft = { error ->
                uiState.value = eventStateFactory.getSendTransactionErrorState(
                    error = error,
                    onConsume = { uiState.value = eventStateFactory.onConsumeEventState() },
                )
                analyticsEventHandler.send(SendAnalyticEvents.TransactionError(cryptoCurrency.symbol))
            },
            ifRight = {
                updateTransactionStatus(txData)
                addTokenToWalletIfNeeded()
                scheduleUpdates()
                sendScreenAnalyticSender.sendTransaction()
            },
        )
    }

    private fun addTokenToWalletIfNeeded() {
        if (cryptoCurrency !is CryptoCurrency.Token) return

        val recipientState = uiState.value.getRecipientState(stateRouter.isEditState) ?: return
        val destinationAddress = recipientState.addressTextField.value

        val receivingUserWallet = userWallets.firstOrNull { it.address == destinationAddress } ?: return

        viewModelScope.launch {
            addCryptoCurrenciesUseCase(
                userWalletId = receivingUserWallet.userWalletId,
                cryptoCurrency = cryptoCurrency,
                network = receivingUserWallet.cryptoCurrency.network,
            )
        }
    }

    private suspend fun updateTransactionStatus(txData: TransactionData.Uncompiled) {
        val txUrl = getExplorerTransactionUrlUseCase(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
        ).getOrElse { "" }
        uiState.value = stateFactory.getTransactionSendState(txData, txUrl)
    }

    private fun scheduleUpdates() {
        coroutineScope.launch {
            // we should update network to find pending tx after 1 sec
            fetchPendingTransactionsUseCase(userWallet.walletId, setOf(cryptoCurrency.network))
            // we should update network for new balance
            updateDelayedCurrencyStatusUseCase(
                userWalletId = userWallet.walletId,
                network = cryptoCurrency.network,
                delayMillis = BALANCE_UPDATE_DELAY,
                refresh = true,
            )
        }
    }

    private fun onCheckFeeUpdate() {
        val sendState = uiState.value.sendState ?: return
        val isSuccess = sendState.isSuccess
        val noErrorNotifications = sendState.notifications.none { it is SendNotification.Error }

        if (!isSuccess && noErrorNotifications) {
            viewModelScope.launch {
                val feeUpdatedState = callFeeUseCase()?.fold(
                    ifRight = {
                        uiState.value = stateFactory.getSendingStateUpdate(isSending = false)
                        eventStateFactory.getFeeUpdatedAlert(
                            fee = it,
                            onConsume = { uiState.value = eventStateFactory.onConsumeEventState() },
                            onFeeNotIncreased = {
                                uiState.value = stateFactory.getSendingStateUpdate(isSending = true)
                                verifyAndSendTransaction()
                            },
                        )
                    },
                    ifLeft = {
                        uiState.value = stateFactory.getSendingStateUpdate(isSending = false)
                        eventStateFactory.getFeeUnreachableErrorState(
                            onConsume = { uiState.value = eventStateFactory.onConsumeEventState() },
                        )
                    },
                )

                uiState.value = if (feeUpdatedState != null) {
                    feeUpdatedState
                } else {
                    uiState.value = stateFactory.getSendingStateUpdate(isSending = false)
                    eventStateFactory.getFeeUnreachableErrorState(
                        onConsume = { uiState.value = eventStateFactory.onConsumeEventState() },
                    )
                }
            }
        }
    }

    private fun setNeverToShowTapHelp() {
        viewModelScope.launch {
            neverShowTapHelpUseCase()
        }
        uiState.value = stateFactory.getHiddenTapHelpState()
    }

    private fun showErrorAlert() {
        uiState.value = eventStateFactory.getGenericErrorState(
            onConsume = { uiState.value = eventStateFactory.onConsumeEventState() },
        )
    }
// endregion

    private companion object {
        const val CHECK_FEE_UPDATE_DELAY = 60_000L
        const val BALANCE_UPDATE_DELAY = 11_000L
        const val RECENT_LOAD_DELAY = 500L
        const val RECENT_TX_SIZE = 100

        const val RU_LOCALE = "ru"
        const val EN_LOCALE = "en"
        const val FEE_READ_MORE_URL_FIRST_PART = "https://tangem.com/"
        const val FEE_READ_MORE_URL_SECOND_PART = "/blog/post/what-is-a-transaction-fee-and-why-do-we-need-it/"
    }
}
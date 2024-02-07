package com.tangem.features.send.impl.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import androidx.paging.PagingData
import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.extensions.isZero
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.*
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
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.*
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.send.impl.navigation.InnerSendRouter
import com.tangem.features.send.impl.presentation.domain.AvailableWallet
import com.tangem.features.send.impl.presentation.state.*
import com.tangem.features.send.impl.presentation.state.fee.FeeNotificationFactory
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeStateFactory
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
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
    private val txHistoryItemsUseCase: GetTxHistoryItemsUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val validateWalletAddressUseCase: ValidateWalletAddressUseCase,
    private val parseSharedAddressUseCase: ParseSharedAddressUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    private val reduxStateHolder: ReduxStateHolder,
    private val isAmountSubtractAvailableUseCase: IsAmountSubtractAvailableUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
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

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    private var innerRouter: InnerSendRouter by Delegates.notNull()
    private var stateRouter: StateRouter by Delegates.notNull()

    private val stateFactory = SendStateFactory(
        clickIntents = this,
        currentStateProvider = Provider { uiState },
        userWalletProvider = Provider { userWallet },
        appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        validateWalletMemoUseCase = validateWalletMemoUseCase,
        getExplorerTransactionUrlUseCase = getExplorerTransactionUrlUseCase,
    )

    private val feeStateFactory = FeeStateFactory(
        clickIntents = this,
        currentStateProvider = Provider { uiState },
        coinCryptoCurrencyStatusProvider = Provider { coinCryptoCurrencyStatus },
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
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
        clickIntents = this,
        getBalanceNotEnoughForFeeWarningUseCase = getBalanceNotEnoughForFeeWarningUseCase,
    )

    private val sendNotificationFactory = SendNotificationFactory(
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        coinCryptoCurrencyStatusProvider = Provider { coinCryptoCurrencyStatus },
        currentStateProvider = Provider { uiState },
        userWalletProvider = Provider { userWallet },
        currencyChecksRepository = currencyChecksRepository,
        clickIntents = this,
    )

    // todo convert to StateFlow
    var uiState: SendUiState by mutableStateOf(stateFactory.getInitialState())
        private set

    private var userWallet: UserWallet by Delegates.notNull()
    private var isAmountSubtractAvailable: Boolean = false
    private var coinCryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    private var cryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()

    private var balanceJobHolder = JobHolder()
    private var recipientsJobHolder = JobHolder()
    private var feeJobHolder = JobHolder()
    private var addressValidationJobHolder = JobHolder()
    private var sendNotificationsJobHolder = JobHolder()
    private var feeNotificationsJobHolder = JobHolder()
    private var qrScannerJobHolder = JobHolder()

    private var sendIdleTimer = 0L

    override fun onCreate(owner: LifecycleOwner) {
        subscribeOnCurrencyStatusUpdates(owner)
        onStateActive()
        subscribeOnBalanceHidden(owner)
    }

    fun setRouter(router: InnerSendRouter, stateRouter: StateRouter) {
        innerRouter = router
        this.stateRouter = stateRouter
        uiState = uiState.copy(currentState = stateRouter.currentState)
    }

    private fun subscribeOnCurrencyStatusUpdates(owner: LifecycleOwner) {
        viewModelScope.launch(dispatchers.main) {
            getUserWalletUseCase(userWalletId).fold(
                ifRight = { wallet ->
                    userWallet = wallet
                    checkIfSubtractAvailable()
                    getCurrenciesStatusUpdates(owner, wallet)
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

    private fun subscribeOnBalanceHidden(owner: LifecycleOwner) {
        getBalanceHidingSettingsUseCase()
            .flowWithLifecycle(owner.lifecycle)
            .conflate()
            .distinctUntilChanged()
            .onEach {
                uiState = stateFactory.getOnHideBalanceState(isBalanceHidden = it.isBalanceHidden)
            }
            .launchIn(viewModelScope)
    }

    private fun getCurrenciesStatusUpdates(owner: LifecycleOwner, wallet: UserWallet) {
        val isSingleWallet = wallet.scanResponse.walletData?.token != null && !wallet.isMultiCurrency

        if (cryptoCurrency is CryptoCurrency.Coin) {
            getCurrencyStatusUpdates(isSingleWallet = isSingleWallet)
                .flowWithLifecycle(owner.lifecycle)
                .onEach { currencyStatus ->
                    currencyStatus.onRight {
                        onDataLoaded(
                            currencyStatus = it,
                            coinCurrencyStatus = it,
                        )
                    }
                }
                .flowOn(dispatchers.main)
                .launchIn(viewModelScope)
                .saveIn(balanceJobHolder)
        } else {
            combine(
                flow = getCoinCurrencyStatusUpdates(isSingleWallet = isSingleWallet),
                flow2 = getCurrencyStatusUpdates(isSingleWallet = isSingleWallet),
            ) { coinStatus, currencyStatus ->
                if (coinStatus.isRight() && currencyStatus.isRight()) {
                    onDataLoaded(
                        currencyStatus = currencyStatus.getOrElse { error("Currency status is unreachable") },
                        coinCurrencyStatus = coinStatus.getOrElse { error("Coin status is unreachable") },
                    )
                }
            }.flowWithLifecycle(owner.lifecycle)
                .flowOn(dispatchers.main)
                .launchIn(viewModelScope)
                .saveIn(balanceJobHolder)
        }
    }

    private fun getCoinCurrencyStatusUpdates(isSingleWallet: Boolean) = getNetworkCoinStatusUseCase(
        userWalletId = userWalletId,
        networkId = cryptoCurrency.network.id,
        derivationPath = cryptoCurrency.network.derivationPath,
        isSingleWalletWithTokens = isSingleWallet,
    ).conflate().distinctUntilChanged()

    private fun getCurrencyStatusUpdates(isSingleWallet: Boolean) = getCurrencyStatusUpdatesUseCase(
        userWalletId = userWalletId,
        currencyId = cryptoCurrency.id,
        isSingleWalletWithTokens = isSingleWallet,
    ).conflate().distinctUntilChanged()

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

    private fun onDataLoaded(currencyStatus: CryptoCurrencyStatus, coinCurrencyStatus: CryptoCurrencyStatus) {
        cryptoCurrencyStatus = currencyStatus
        coinCryptoCurrencyStatus = coinCurrencyStatus

        if (transactionId != null && amount != null && destinationAddress != null) {
            uiState = stateFactory.getReadyState(amount, destinationAddress)
            showFee()
        } else {
            getWalletsAndRecent()
            uiState = stateFactory.getReadyState()
            showRecipient()
        }
        updateNotifications()
    }

    private fun getWalletsAndRecent() {
        combine(
            flow = getUserWallets().conflate(),
            flow2 = getTxHistory().conflate(),
            flow3 = getTxHistoryCount().conflate(),
        ) { wallets, txHistory, txHistoryCount ->
            stateFactory.onLoadedRecipientList(
                wallets = wallets,
                txHistory = txHistory,
                txHistoryCount = txHistoryCount,
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
                                getCryptoCurrenciesUseCase(wallet.walletId)
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

    private fun getTxHistory(): Flow<PagingData<TxHistoryItem>> {
        return flow {
            txHistoryItemsUseCase(
                userWalletId = userWalletId,
                currency = cryptoCurrency,
            ).fold(
                ifRight = { emitAll(it.distinctUntilChanged()) },
                ifLeft = {},
            )
        }
    }

    private fun getTxHistoryCount(): Flow<Int> {
        return flow {
            txHistoryItemsCountUseCase(
                userWalletId = userWalletId,
                currency = cryptoCurrency,
            ).fold(
                ifRight = { emit(it) },
                ifLeft = { emit(0) },
            )
        }
    }

    private fun onStateActive() {
        uiState.currentState
            .onEach {
                when (it) {
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
    override fun onBackClick() = stateRouter.onBackClick(uiState.sendState.isSuccess)
    override fun onNextClick() = stateRouter.onNextClick()
    override fun onPrevClick() = stateRouter.onPrevClick()

    override fun onQrCodeScanClick() = innerRouter.openQrCodeScanner(cryptoCurrency.network.name)

    override fun onFailedTxEmailClick(errorMessage: String) {
        reduxStateHolder.dispatch(LegacyAction.SendEmailTransactionFailed(errorMessage))
    }

    override fun onTokenDetailsClick(userWalletId: UserWalletId, currency: CryptoCurrency) =
        innerRouter.openTokenDetails(userWalletId, currency)
    // endregion

    // region amount state clicks
    override fun onCurrencyChangeClick(isFiat: Boolean) {
        uiState = stateFactory.getOnCurrencyChangedState(isFiat)
    }

    override fun onAmountValueChange(value: String) {
        uiState = stateFactory.getOnAmountValueChange(value)
    }

    override fun onMaxValueClick() {
        val amountState = uiState.amountState ?: return
        val amountTextField = amountState.amountTextField
        val (amount, decimals) = if (amountTextField.isFiatValue) {
            cryptoCurrencyStatus.value.fiatAmount to amountTextField.fiatAmount.decimals
        } else {
            cryptoCurrencyStatus.value.amount to amountTextField.cryptoAmount.decimals
        }
        if (amount != null && !amount.isZero()) {
            onAmountValueChange(amount.parseBigDecimal(decimals))
        }
    }
    // endregion

    // region recipient state clicks
    fun onRecipientAddressScanned(address: String) {
        viewModelScope.launch(dispatchers.main) {
            parseSharedAddressUseCase(address, cryptoCurrency.network).fold(
                ifRight = { parsedCode ->
                    onRecipientAddressValueChange(parsedCode.address)
                    parsedCode.amount?.let { onAmountValueChange(it.toPlainString()) }
                    parsedCode.memo?.let { onRecipientMemoValueChange(it) }
                },
                ifLeft = {
                    Timber.w(it)
                },
            )
        }.saveIn(qrScannerJobHolder)
    }

    override fun onRecipientAddressValueChange(value: String) {
        viewModelScope.launch(dispatchers.main) {
            if (!checkIfXrpAddressValue(value)) {
                uiState = stateFactory.onRecipientAddressValueChange(value)
                uiState = stateFactory.getOnRecipientAddressValidationStarted()
                val isValidAddress = validateAddress(value)
                uiState = stateFactory.getOnRecipientAddressValidState(value, isValidAddress)
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
        }.saveIn(addressValidationJobHolder)
    }

    private suspend fun validateAddress(value: String): Boolean {
        return validateWalletAddressUseCase(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
            address = value,
        ).getOrElse { false }
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
    // endregion

    // region fee
    override fun feeReload() = loadFee()

    override fun onFeeSelectorClick(feeType: FeeType) {
        uiState = feeStateFactory.onFeeSelectedState(feeType)
        updateFeeNotifications()
    }

    override fun onCustomFeeValueChange(index: Int, value: String) {
        uiState = feeStateFactory.onCustomFeeValueChange(index, value)
        updateFeeNotifications()
    }

    override fun onSubtractSelect(value: Boolean) {
        uiState = feeStateFactory.onSubtractSelect(value)
        updateFeeNotifications()
    }

    private fun loadFee() {
        viewModelScope.launch(dispatchers.main) {
            uiState = feeStateFactory.onFeeOnLoadingState()
            uiState = callFeeUseCase()?.fold(
                ifRight = { fees ->
                    feeStateFactory.onFeeOnLoadedState(fees, isAmountSubtractAvailable)
                },
                ifLeft = {
                    feeStateFactory.onFeeOnErrorState()
                },
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
        val sendState = uiState.sendState
        if (sendState.isSuccess) popBackStack()

        uiState = stateFactory.getSendingStateUpdate(isSending = true)
        if (System.currentTimeMillis() - sendIdleTimer < CHECK_FEE_UPDATE_DELAY) {
            verifyAndSendTransaction()
        } else {
            onCheckFeeUpdate()
        }
        sendIdleTimer = System.currentTimeMillis()
    }

    override fun showAmount() = stateRouter.showAmount()

    override fun showRecipient() = stateRouter.showRecipient()

    override fun showFee() = stateRouter.showFee()

    override fun onExploreClick(txUrl: String) = innerRouter.openUrl(txUrl)

    override fun onAmountReduceClick(reducedAmount: String) {
        uiState = stateFactory.getOnAmountValueChange(reducedAmount)
        uiState = sendNotificationFactory.dismissHighFeeWarningState()
        loadFee()
    }

    override fun onAmountReduceIgnoreClick() {
        uiState = sendNotificationFactory.dismissHighFeeWarningState()
    }

    private fun verifyAndSendTransaction() {
        val recipient = uiState.recipientState?.addressTextField?.value ?: return
        val feeState = uiState.feeState ?: return
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return
        val memo = uiState.recipientState?.memoTextField?.value
        val fee = feeStateFactory.feeConverter.convert(feeSelectorState)
        val amountValue = uiState.amountState?.amountTextField?.cryptoAmount?.value ?: return
        val amountToSend = if (feeState.isSubtract && isAmountSubtractAvailable) {
            feeState.receivedAmountValue
        } else {
            amountValue
        }

        viewModelScope.launch(dispatchers.main) {
            createTransactionUseCase(
                amount = amountToSend.convertToAmount(cryptoCurrency),
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
            },
            ifRight = {
                uiState = stateFactory.getSendingStateUpdate(isSending = false)
                uiState = stateFactory.getTransactionSendState(txData)
                scheduleBalanceUpdate()
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
        val isSending = uiState.sendState.isSending
        val isSuccess = uiState.sendState.isSuccess
        val noErrorNotifications = uiState.sendState.notifications.none { it is SendNotification.Error }

        if (!isSending && !isSuccess && noErrorNotifications) {
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
    // endregion

    companion object {
        private const val CHECK_FEE_UPDATE_DELAY = 60_000L
        private const val BALANCE_UPDATE_DELAY = 10_000L
    }
}
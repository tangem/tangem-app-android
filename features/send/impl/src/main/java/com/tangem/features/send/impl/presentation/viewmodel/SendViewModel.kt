package com.tangem.features.send.impl.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import androidx.paging.PagingData
import arrow.core.getOrElse
import com.tangem.blockchain.blockchains.xrp.XrpAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.GetCryptoCurrenciesUseCase
import com.tangem.domain.tokens.GetCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.GetNetworkCoinStatusUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.utils.convertToAmount
import com.tangem.domain.transaction.usecase.CreateTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
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
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState
import com.tangem.features.send.impl.presentation.state.fee.FeeStateFactory
import com.tangem.features.send.impl.presentation.state.fee.FeeType
import com.tangem.features.send.impl.presentation.state.fee.getFee
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
@HiltViewModel
internal class SendViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
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
    getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    validateWalletMemoUseCase: ValidateWalletMemoUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), DefaultLifecycleObserver, SendClickIntents {

    private val userWalletId: UserWalletId = savedStateHandle.get<String>(SendRouter.USER_WALLET_ID_KEY)
        ?.let { stringValue -> UserWalletId(stringValue) }
        ?: error("This screen can't open without `UserWalletId`")

    private val cryptoCurrency: CryptoCurrency = savedStateHandle[SendRouter.CRYPTO_CURRENCY_KEY]
        ?: error("This screen can't open without `CryptoCurrency`")

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

    private val feeStateFactory by lazy {
        FeeStateFactory(
            clickIntents = this,
            currentStateProvider = Provider { uiState },
            coinCryptoCurrencyStatusProvider = Provider { coinCryptoCurrencyStatus },
            cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
            appCurrencyProvider = Provider(selectedAppCurrencyFlow::value),
            userWalletProvider = Provider { userWallet },
        )
    }

    private val eventStateFactory = SendEventStateFactory(
        clickIntents = this,
        currentStateProvider = Provider { uiState },
    )

    private val sendNotificationFactory = SendNotificationFactory(
        cryptoCurrencyStatusProvider = Provider { cryptoCurrencyStatus },
        coinCryptoCurrencyStatusProvider = Provider { coinCryptoCurrencyStatus },
        currentStateProvider = Provider { uiState },
        userWalletProvider = Provider { userWallet },
        walletManagersFacade = walletManagersFacade,
        clickIntents = this,
    )

    // todo convert to StateFlow
    var uiState: SendUiState by mutableStateOf(stateFactory.getInitialState())
        private set

    private var userWallet: UserWallet by Delegates.notNull()
    private var coinCryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    private var cryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()

    private var balanceJobHolder = JobHolder()
    private var recipientsJobHolder = JobHolder()
    private var feeJobHolder = JobHolder()
    private var addressValidationJobHolder = JobHolder()
    private var sendNotificationsJobHolder = JobHolder()
    private var qrScannerJobHolder = JobHolder()

    override fun onCreate(owner: LifecycleOwner) {
        subscribeOnCurrencyStatusUpdates(owner)
        onFeeStateActive()
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

    private fun getCurrenciesStatusUpdates(owner: LifecycleOwner, wallet: UserWallet) {
        val isSingleWallet = wallet.scanResponse.walletData?.token != null && !wallet.isMultiCurrency

        if (cryptoCurrency is CryptoCurrency.Coin) {
            getCurrencyStatusUpdates(isSingleWallet = isSingleWallet)
                .flowWithLifecycle(owner.lifecycle)
                .onEach { currencyStatus ->
                    currencyStatus.onRight {
                        cryptoCurrencyStatus = it
                        coinCryptoCurrencyStatus = it
                        getWalletsAndRecent()
                        uiState = stateFactory.getReadyState()
                        updateNotifications()
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
                    coinStatus.onRight { coinCryptoCurrencyStatus = it }
                    currencyStatus.onRight { cryptoCurrencyStatus = it }
                    getWalletsAndRecent()
                    uiState = stateFactory.getReadyState()
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
                                            return@fold AvailableWallet(
                                                name = wallet.name,
                                                address = addresses.first().value,
                                            )
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

    private fun onFeeStateActive() {
        uiState.currentState
            .filter { it == SendUiStateType.Fee }
            .onEach { loadFee() }
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

    // region screen state navigation
    override fun popBackStack() = stateRouter.popBackStack()
    override fun onBackClick() = stateRouter.onBackClick()
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
        val amount = if (amountState.isFiatValue) {
            amountState.cryptoCurrencyStatus.value.fiatAmount
        } else {
            amountState.cryptoCurrencyStatus.value.amount
        }
        onAmountValueChange(amount?.toPlainString() ?: DEFAULT_VALUE)
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
        uiState = stateFactory.onRecipientAddressValueChange(value)
        viewModelScope.launch(dispatchers.main) {
            uiState = stateFactory.getOnRecipientAddressValidationStarted()
            if (!checkIfXrpAddressValue(value)) {
                val isValidAddress = validateAddress(value)
                uiState = stateFactory.getOnRecipientAddressValidState(value, isValidAddress)
            }
        }.saveIn(addressValidationJobHolder)
    }

    override fun onRecipientMemoValueChange(value: String) {
        uiState = stateFactory.getOnRecipientMemoValueChange(value)
        viewModelScope.launch(dispatchers.main) {
            uiState = stateFactory.getOnRecipientAddressValidationStarted()
            if (!checkIfXrpAddressValue(value)) {
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

    private fun checkIfXrpAddressValue(value: String): Boolean {
        if (cryptoCurrency.network.id.value == Blockchain.XRP.id && value.firstOrNull() == XRP_X_ADDRESS) {
            viewModelScope.launch(dispatchers.io) {
                val result = XrpAddressService.decodeXAddress(value)
                onRecipientAddressValueChange(result?.address.orEmpty())
                onRecipientMemoValueChange(result?.destinationTag.toString())
            }
            return true
        }
        return false
    }
    // endregion

    // region fee
    override fun feeReload() = loadFee()

    override fun onFeeSelectorClick(feeType: FeeType) {
        uiState = feeStateFactory.onFeeSelectedState(feeType)
    }

    override fun onCustomFeeValueChange(index: Int, value: String) {
        uiState = feeStateFactory.onCustomFeeValueChange(index, value)
    }

    override fun onSubtractSelect(value: Boolean) {
        uiState = feeStateFactory.onSubtractSelect(value)
    }

    private fun loadFee() {
        viewModelScope.launch(dispatchers.main) {
            val amountState = uiState.amountState ?: return@launch
            val recipientState = uiState.recipientState ?: return@launch
            val amount = amountState.amountTextField.value.toBigDecimal()

            uiState = feeStateFactory.onFeeOnLoadingState()
            getFeeUseCase.invoke(
                amount = amount,
                destination = recipientState.addressTextField.value,
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
            ).fold(
                ifRight = {
                    uiState = feeStateFactory.onFeeOnLoadedState(it)
                },
                ifLeft = {
                    uiState = feeStateFactory.onFeeOnErrorState()
                },
            )
        }.saveIn(feeJobHolder)
    }
    // endregion

    // region send state clicks
    override fun onSendClick() {
        val sendState = uiState.sendState
        if (sendState.isSuccess) popBackStack()

        uiState = stateFactory.getSendingStateUpdate(isSending = true)
        verifyAndSendTransaction()
    }

    override fun showAmount() = stateRouter.showAmount(isFromSend = true)

    override fun showRecipient() = stateRouter.showRecipient(isFromSend = true)

    override fun showFee() = stateRouter.showFee(isFromSend = true)

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
        val fee = feeSelectorState.getFee()
        val amountValue = uiState.amountState?.amountValue ?: return

        val amountToSend = if (feeState.isSubtract) feeState.receivedAmountValue else amountValue

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
            },
        )
    }
    // endregion

    companion object {
        private const val XRP_X_ADDRESS = 'X'
        private const val DEFAULT_VALUE = "0.00"
    }
}
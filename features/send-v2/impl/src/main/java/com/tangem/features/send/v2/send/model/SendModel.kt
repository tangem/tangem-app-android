package com.tangem.features.send.v2.send.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.qrscanning.usecases.ParseQrCodeUseCase
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForGaslessUseCase
import com.tangem.domain.transaction.usecase.gasless.GetFeeForTokenUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.models.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents.SendScreenSource
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.entity.PredefinedValues
import com.tangem.features.send.v2.api.entity.isFromMainScreenQr
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.CommonSendRoute.*
import com.tangem.features.send.v2.common.SendConfirmAlertFactory
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.send.analytics.SendAnalyticEvents
import com.tangem.features.send.v2.send.confirm.SendConfirmComponent
import com.tangem.features.send.v2.send.success.SendConfirmSuccessComponent
import com.tangem.features.send.v2.send.ui.state.SendUM
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponent
import com.tangem.features.send.v2.subcomponents.amount.SendAmountUpdateTrigger
import com.tangem.features.send.v2.subcomponents.destination.model.transformers.SendDestinationInitialStateTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import kotlin.properties.Delegates

internal interface SendComponentCallback :
    SendAmountComponent.ModelCallback,
    SendDestinationComponent.ModelCallback,
    SendConfirmComponent.ModelCallback,
    SendConfirmSuccessComponent.ModelCallback

@Stable
@ModelScoped
@Suppress("LongParameterList", "LargeClass")
internal class SendModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
    private val parseQrCodeUseCase: ParseQrCodeUseCase,
    private val sendConfirmAlertFactory: SendConfirmAlertFactory,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val getFeeForGaslessUseCase: GetFeeForGaslessUseCase,
    private val getFeeForTokenUseCase: GetFeeForTokenUseCase,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val sendAmountUpdateTrigger: SendAmountUpdateTrigger,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model(), SendComponentCallback {

    private val params: SendComponent.Params = paramsContainer.require()
    private val cryptoCurrency = params.currency

    private var isEntryTypeConsumed = false

    val analyticCategoryName = CommonSendAnalyticEvents.SEND_CATEGORY
    val analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Send

    val uiState: StateFlow<SendUM>
        field = MutableStateFlow(initialState())

    val isBalanceHiddenFlow: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val initialRoute = if (params.amount == null) {
        Amount(isEditMode = false)
    } else {
        Empty
    }

    val currentRoute = MutableStateFlow(initialRoute)

    val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                params.currency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )

    val feeCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                params.currency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )

    val isAvailableForSend: Boolean
        get() {
            val cryptoCurrencyStatus = cryptoCurrencyStatusFlow.value
            val feeCryptoCurrencyStatus = feeCryptoCurrencyStatusFlow.value

            return cryptoCurrencyStatus.isAvailableForSend() && feeCryptoCurrencyStatus.isAvailableForSend()
        }

    val isUnavailableForSend: Boolean
        get() {
            val cryptoCurrencyStatus = cryptoCurrencyStatusFlow.value
            val feeCryptoCurrencyStatus = feeCryptoCurrencyStatusFlow.value

            return cryptoCurrencyStatus.isUnavailableForSend() || feeCryptoCurrencyStatus.isUnavailableForSend()
        }

    val accountFlow: StateFlow<Account?>
        field = MutableStateFlow(null)
    val isAccountModeFlow: StateFlow<Boolean>
        field = MutableStateFlow(false)

    var userWallet: UserWallet by Delegates.notNull()
    var appCurrency: AppCurrency = AppCurrency.Default
    var predefinedValues: PredefinedValues = PredefinedValues.Empty

    private val balanceHidingJobHolder = JobHolder()

    init {
        subscribeOnBalanceHidden()
        subscribeOnQRScannerResult()
        subscribeOnCurrencyStatusUpdates()
        initPredefinedValues()
        if (predefinedValues is PredefinedValues.Content) {
            subscribeOnStatusForInitialNavigation()
        }
        initAppCurrency()
    }

    override fun onNavigationResult(navigationUM: NavigationUM) {
        uiState.update { it.copy(navigationUM = navigationUM) }
    }

    override fun onDestinationResult(destinationUM: DestinationUM) {
        uiState.update { it.copy(destinationUM = destinationUM) }
    }

    override fun onAmountResult(amountUM: AmountState, isResetPredefined: Boolean) {
        if (isResetPredefined) resetPredefinedAmount()
        uiState.update { it.copy(amountUM = amountUM) }
    }

    override fun onResult(sendUM: SendUM) {
        uiState.update { sendUM }
    }

    override fun onBackClick() {
        when (val route = currentRoute.value) {
            is Amount -> if (!route.isEditMode) {
                analyticsEventHandler.send(
                    CommonSendAnalyticEvents.CloseButtonClicked(
                        categoryName = analyticCategoryName,
                        source = SendScreenSource.Amount,
                        isFromSummary = false,
                        isValid = uiState.value.amountUM.isPrimaryButtonEnabled,
                    ),
                )
            }
            is Destination -> if (!route.isEditMode) {
                analyticsEventHandler.send(
                    CommonSendAnalyticEvents.CloseButtonClicked(
                        categoryName = analyticCategoryName,
                        source = SendScreenSource.Address,
                        isFromSummary = false,
                        isValid = uiState.value.amountUM.isPrimaryButtonEnabled,
                    ),
                )
            }
            else -> Unit
        }

        router.pop()
    }

    override fun onNextClick() {
        if (currentRoute.value.isEditMode) {
            onBackClick()
        } else {
            when (currentRoute.value) {
                is Amount -> {
                    val nextRoute = if (predefinedValues.isFromMainScreenQr) {
                        Confirm
                    } else {
                        Destination(isEditMode = false)
                    }
                    router.push(nextRoute)
                }
                is Destination -> router.push(Confirm)
                Confirm -> router.push(ConfirmSuccess)
                else -> onBackClick()
            }
        }
    }

    override fun onConvertToAnotherToken(lastAmount: String, isEnterInFiatSelected: Boolean) {
        analyticsEventHandler.send(
            SendAnalyticEvents.ConvertTokenButtonClicked(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            ),
        )
        params.callback?.onConvertToAnotherToken(lastAmount = lastAmount, isEnterInFiatSelected = isEnterInFiatSelected)
    }

    override fun resetSendNavigation() {
        uiState.update { state ->
            state.copy(
                destinationUM = SendDestinationInitialStateTransformer(
                    cryptoCurrency = cryptoCurrency,
                ).transform(DestinationUM.Empty()),
                feeSelectorUM = FeeSelectorUM.Loading,
                confirmUM = ConfirmUM.Empty,
                confirmData = null,
                navigationUM = NavigationUM.Empty,
            )
        }
        router.popTo(Amount(isEditMode = false))
    }

    override fun onError(error: GetUserWalletError) {
        TangemLogger.w(error.toString())
        showAlertError()
    }

    fun consumeEntryType(): CommonSendAnalyticEvents.SendEntryType {
        if (isEntryTypeConsumed) return CommonSendAnalyticEvents.SendEntryType.Manual
        isEntryTypeConsumed = true
        return when (params.entryType) {
            SendComponent.EntryType.QR -> CommonSendAnalyticEvents.SendEntryType.QR
            SendComponent.EntryType.Manual -> CommonSendAnalyticEvents.SendEntryType.Manual
        }
    }

    private suspend fun prepareTransferTransaction(): Either<Throwable, TransactionData> {
        val predefinedValues = predefinedValues
        val cryptoCurrencyStatus = cryptoCurrencyStatusFlow.value
        return when (predefinedValues) {
            is PredefinedValues.Content.Deeplink -> {
                val predefinedAmount = predefinedValues.amount.parseBigDecimalOrNull()
                createTransferTransactionUseCase(
                    amount = predefinedAmount?.convertToSdkAmount(cryptoCurrencyStatus)
                        ?: error("Invalid amount"),
                    memo = predefinedValues.memo,
                    destination = predefinedValues.address,
                    userWalletId = userWallet.walletId,
                    network = cryptoCurrency.network,
                )
            }
            is PredefinedValues.Content.QrCode -> {
                val predefinedAmount = predefinedValues.amount?.parseBigDecimalOrNull()
                val amount = predefinedAmount
                    ?: (uiState.value.amountUM as? AmountState.Data)?.amountTextField?.cryptoAmount?.value
                    ?: error("Invalid amount")
                createTransferTransactionUseCase(
                    amount = amount.convertToSdkAmount(cryptoCurrencyStatus),
                    memo = predefinedValues.memo,
                    destination = predefinedValues.address,
                    userWalletId = userWallet.walletId,
                    network = cryptoCurrency.network,
                )
            }
            PredefinedValues.Empty -> {
                val destinationUM = uiState.value.destinationUM as? DestinationUM.Content
                    ?: error("Invalid destination")
                val amountUM = uiState.value.amountUM as? AmountState.Data ?: error("Invalid amount")
                val enteredDestinationAddress = destinationUM.addressTextField.actualAddress
                val enteredMemo = destinationUM.memoTextField?.value
                val enteredAmount = amountUM.amountTextField.cryptoAmount.value ?: error("Invalid amount")

                createTransferTransactionUseCase(
                    amount = enteredAmount.convertToSdkAmount(cryptoCurrencyStatus),
                    memo = enteredMemo,
                    destination = enteredDestinationAddress,
                    userWalletId = userWallet.walletId,
                    network = cryptoCurrency.network,
                )
            }
        }
    }

    suspend fun loadFee(): Either<GetFeeError, TransactionFee> {
        val transferTransaction = prepareTransferTransaction()
            .getOrElse { return GetFeeError.DataError(it).left() }

        return getFeeUseCase(
            transactionData = transferTransaction,
            userWallet = userWallet,
            network = params.currency.network,
        )
    }

    suspend fun loadFeeExtended(maybeToken: CryptoCurrencyStatus?): Either<GetFeeError, TransactionFeeExtended> {
        val transferTransaction = prepareTransferTransaction()
            .getOrElse { return GetFeeError.DataError(it).left() }

        return if (maybeToken == null) {
            getFeeForGaslessUseCase(
                transactionData = transferTransaction,
                userWallet = userWallet,
                network = params.currency.network,
            )
        } else {
            getFeeForTokenUseCase(
                transactionData = transferTransaction,
                userWallet = userWallet,
                token = maybeToken.currency,
            )
        }
    }

    fun showAlertError() {
        sendConfirmAlertFactory.getGenericErrorState(
            onFailedTxEmailClick = ::onFailedTxEmailClick,
            popBack = router::pop,
        )
    }

    private fun initAppCurrency() {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
        }
    }

    private fun initPredefinedValues() {
        val predefinedAmount = params.amount
        val predefinedTxId = params.transactionId
        val predefinedAddress = params.destinationAddress

        predefinedValues = if (predefinedAmount != null && predefinedTxId != null && predefinedAddress != null) {
            PredefinedValues.Content.Deeplink(
                amount = predefinedAmount,
                address = predefinedAddress,
                memo = params.tag,
                transactionId = predefinedTxId,
            )
        } else if (predefinedAddress != null) {
            PredefinedValues.Content.QrCode(
                amount = predefinedAmount,
                address = predefinedAddress,
                memo = params.tag, source = PredefinedValues.Source.MAIN_SCREEN,
            )
        } else {
            PredefinedValues.Empty
        }
    }

    private fun resetPredefinedAmount() {
        // reset predefined amount
        val internalPredefinedValues = predefinedValues
        predefinedValues = when (internalPredefinedValues) {
            is PredefinedValues.Content.QrCode -> internalPredefinedValues.copy(amount = null)
            is PredefinedValues.Content.Deeplink,
            PredefinedValues.Empty,
            -> internalPredefinedValues
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        modelScope.launch {
            getUserWalletUseCase(params.userWalletId).fold(
                ifRight = { wallet ->
                    userWallet = wallet

                    getAccountCurrencyStatusUseCase(
                        userWalletId = params.userWalletId,
                        currency = cryptoCurrency,
                    ).onEach { (account, cryptoCurrencyStatus) ->
                        isAccountModeFlow.value = isAccountsModeEnabledUseCase.invokeSync()
                        accountFlow.value = account

                        cryptoCurrencyStatusFlow.value = cryptoCurrencyStatus
                        feeCryptoCurrencyStatusFlow.value = getFeePaidCryptoCurrencyStatusSyncUseCase(
                            userWalletId = params.userWalletId,
                            cryptoCurrencyStatus = cryptoCurrencyStatus,
                        ).getOrNull() ?: cryptoCurrencyStatus
                    }.flowOn(dispatchers.default)
                        .launchIn(modelScope)
                },
                ifLeft = { error ->
                    TangemLogger.w(error.toString())
                    showAlertError()
                    return@launch
                },
            )
        }
    }

    private fun subscribeOnStatusForInitialNavigation() {
        combine(
            cryptoCurrencyStatusFlow,
            feeCryptoCurrencyStatusFlow,
        ) { cryptoCurrencyStatus, _ ->
            if (!isAvailableForSend || currentRoute.value != initialRoute) {
                if (isUnavailableForSend) showAlertError()
                return@combine
            }
            val route = resolveInitialRoute(cryptoCurrencyStatus) ?: return@combine
            router.replaceAll(route)
        }.launchIn(modelScope)
    }

    private fun resolveInitialRoute(cryptoCurrencyStatus: CryptoCurrencyStatus): CommonSendRoute? {
        return when (predefinedValues) {
            is PredefinedValues.Content.Deeplink -> Confirm
            is PredefinedValues.Content.QrCode -> {
                if (!predefinedValues.isFromMainScreenQr) return null
                val amount = (predefinedValues as PredefinedValues.Content.QrCode).amount ?: return null
                if (isPredefinedAmountExceedsBalance(amount, cryptoCurrencyStatus)) {
                    Amount(isEditMode = false)
                } else {
                    Confirm
                }
            }
            PredefinedValues.Empty -> null
        }
    }

    private fun isPredefinedAmountExceedsBalance(amount: String, cryptoCurrencyStatus: CryptoCurrencyStatus): Boolean {
        val predefinedAmount = amount.parseBigDecimalOrNull() ?: return false
        val balance = cryptoCurrencyStatus.value.amount ?: return false
        return predefinedAmount > balance
    }

    private fun CryptoCurrencyStatus.hasAvailableStatus(): Boolean = this.value is CryptoCurrencyStatus.Loaded ||
        this.value is CryptoCurrencyStatus.Custom ||
        this.value is CryptoCurrencyStatus.NoQuote

    private fun CryptoCurrencyStatus.hasUnavailableStatus(): Boolean = this.value is CryptoCurrencyStatus.Unreachable ||
        this.value is CryptoCurrencyStatus.NoAmount ||
        this.value is CryptoCurrencyStatus.MissedDerivation ||
        this.value is CryptoCurrencyStatus.NoAccount

    private fun CryptoCurrencyStatus.isAvailableForSend(): Boolean =
        this.hasAvailableStatus() && this.value.sources.networkSource.isActual()

    private fun CryptoCurrencyStatus.isUnavailableForSend(): Boolean =
        this.hasUnavailableStatus() && this.value.sources.networkSource.isActual()

    private fun subscribeOnBalanceHidden() {
        getBalanceHidingSettingsUseCase()
            .conflate()
            .distinctUntilChanged()
            .onEach {
                isBalanceHiddenFlow.value = it.isBalanceHidden
            }
            .launchIn(modelScope)
            .saveIn(balanceHidingJobHolder)
    }

    private fun subscribeOnQRScannerResult() {
        listenToQrScanningUseCase(SourceType.SEND)
            .getOrElse { emptyFlow() }
            .onEach(::onQrCodeScanned)
            .launchIn(modelScope)
    }

    private fun onQrCodeScanned(address: String) {
        val parsedQrCode = parseQrCodeUseCase(address, cryptoCurrency).getOrNull()
        // Decompose component can be active or inactive depending on its state and navigation stack
        // If it is in inactive state use parameter to pass value to amount component
        val amount = parsedQrCode?.amount?.parseBigDecimal(cryptoCurrency.decimals)
        predefinedValues = PredefinedValues.Content.QrCode(
            amount = amount.orEmpty(),
            address = parsedQrCode?.address.orEmpty(),
            memo = parsedQrCode?.memo,
            source = PredefinedValues.Source.SEND_SCREEN,
        )
        // If it is in active state use flow to update value in amount component
        modelScope.launch {
            amount?.let { sendAmountUpdateTrigger.triggerUpdateAmount(it, null) }
        }
    }

    private fun onFailedTxEmailClick(errorMessage: String? = null) {
        saveBlockchainErrorUseCase(
            error = BlockchainErrorInfo(
                errorMessage = errorMessage.orEmpty(),
                blockchainId = cryptoCurrency.network.rawId,
                derivationPath = cryptoCurrency.network.derivationPath.value,
                destinationAddress = "",
                tokenSymbol = "",
                amount = "",
                fee = "",
            ),
        )

        modelScope.launch {
            val metaInfo = getWalletMetaInfoUseCase(userWallet.walletId).getOrNull() ?: return@launch
            analyticsEventHandler.send(Basic.ButtonSupport(source = AnalyticsParam.ScreensSources.Send))
            sendFeedbackEmailUseCase(type = FeedbackEmailType.TransactionSendingProblem(walletMetaInfo = metaInfo))
        }
    }

    private fun initialState(): SendUM = SendUM(
        amountUM = AmountState.Empty,
        destinationUM = SendDestinationInitialStateTransformer(
            cryptoCurrency = cryptoCurrency,
        ).transform(DestinationUM.Empty()),
        confirmUM = ConfirmUM.Empty,
        navigationUM = NavigationUM.Empty,
        confirmData = null,
        feeSelectorUM = FeeSelectorUM.Loading,
    )
}
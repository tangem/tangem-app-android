package com.tangem.features.send.v2.send.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.qrscanning.usecases.ParseQrCodeUseCase
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.isMultiCurrency
import com.tangem.domain.wallets.models.requireColdWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.api.SendFeatureToggles
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.PredefinedValues
import com.tangem.features.send.v2.common.SendConfirmAlertFactory
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.common.ui.state.NavigationUM
import com.tangem.features.send.v2.send.confirm.SendConfirmComponent
import com.tangem.features.send.v2.send.ui.state.SendUM
import com.tangem.features.send.v2.subcomponents.amount.SendAmountComponent
import com.tangem.features.send.v2.subcomponents.amount.SendAmountUpdateQRTrigger
import com.tangem.features.send.v2.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.v2.subcomponents.destination.model.transformers.SendDestinationInitialStateTransformer
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponent
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

internal interface SendComponentCallback :
    SendAmountComponent.ModelCallback,
    SendFeeComponent.ModelCallback,
    SendDestinationComponent.ModelCallback,
    SendConfirmComponent.ModelCallback

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class SendModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
    private val parseQrCodeUseCase: ParseQrCodeUseCase,
    private val sendConfirmAlertFactory: SendConfirmAlertFactory,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val createTransferTransactionUseCase: CreateTransferTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val sendAmountUpdateQRTrigger: SendAmountUpdateQRTrigger,
    private val sendFeatureToggles: SendFeatureToggles,
) : Model(), SendComponentCallback {

    private val params: SendComponent.Params = paramsContainer.require()
    private val cryptoCurrency = params.currency

    private val _uiState = MutableStateFlow(initialState())
    val uiState = _uiState.asStateFlow()

    private val _isBalanceHiddenFlow = MutableStateFlow(false)
    val isBalanceHiddenFlow = _isBalanceHiddenFlow.asStateFlow()

    private val _cryptoCurrencyStatusFlow = MutableStateFlow(
        CryptoCurrencyStatus(
            params.currency,
            value = CryptoCurrencyStatus.Loading,
        ),
    )
    val cryptoCurrencyStatusFlow = _cryptoCurrencyStatusFlow.asStateFlow()

    private val _feeCryptoCurrencyStatusFlow = MutableStateFlow(
        CryptoCurrencyStatus(
            params.currency,
            value = CryptoCurrencyStatus.Loading,
        ),
    )
    val feeCryptoCurrencyStatusFlow = _feeCryptoCurrencyStatusFlow.asStateFlow()

    var userWallet: UserWallet by Delegates.notNull()
    var appCurrency: AppCurrency = AppCurrency.Default
    var predefinedValues: PredefinedValues = PredefinedValues.Empty

    private var balanceHidingJobHolder = JobHolder()

    init {
        subscribeOnBalanceHidden()
        subscribeOnQRScannerResult()
        subscribeOnCurrencyStatusUpdates()
        initAppCurrency()
        initPredefinedValues()
    }

    override fun onNavigationResult(navigationUM: NavigationUM) {
        _uiState.update { it.copy(navigationUM = navigationUM) }
    }

    override fun onDestinationResult(destinationUM: DestinationUM) {
        _uiState.update { it.copy(destinationUM = destinationUM) }
    }

    override fun onAmountResult(amountUM: AmountState, isResetPredefined: Boolean) {
        if (isResetPredefined) resetPredefinedAmount()
        _uiState.update { it.copy(amountUM = amountUM) }
    }

    override fun onFeeResult(feeUM: FeeUM) {
        _uiState.update { it.copy(feeUM = feeUM) }
    }

    override fun onResult(sendUM: SendUM) {
        _uiState.update { sendUM }
    }

    suspend fun loadFee(): Either<GetFeeError, TransactionFee> {
        val predefinedValues = predefinedValues
        val transferTransaction = if (predefinedValues is PredefinedValues.Content.Deeplink) {
            val predefinedAmount = predefinedValues.amount.parseBigDecimalOrNull()?.convertToSdkAmount(cryptoCurrency)
            createTransferTransactionUseCase(
                amount = predefinedAmount ?: error("Invalid amount"),
                memo = predefinedValues.memo,
                destination = predefinedValues.address,
                userWalletId = userWallet.walletId,
                network = cryptoCurrency.network,
            )
        } else {
            val destinationUM = uiState.value.destinationUM as? DestinationUM.Content ?: error("Invalid destination")
            val amountUM = uiState.value.amountUM as? AmountState.Data ?: error("Invalid amount")
            val enteredDestinationAddress = destinationUM.addressTextField.actualAddress
            val enteredMemo = destinationUM.memoTextField?.value
            val enteredAmount = amountUM.amountTextField.cryptoAmount.value ?: error("Invalid amount")

            createTransferTransactionUseCase(
                amount = enteredAmount.convertToSdkAmount(cryptoCurrency),
                memo = enteredMemo,
                destination = enteredDestinationAddress,
                userWalletId = userWallet.walletId,
                network = cryptoCurrency.network,
            )
        }.getOrElse {
            return GetFeeError.DataError(it).left()
        }

        return getFeeUseCase(
            transactionData = transferTransaction,
            userWallet = userWallet,
            network = params.currency.network,
        )
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

                    val isSingleWalletWithToken = wallet is UserWallet.Cold &&
                        wallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
                    val isMultiCurrency = wallet.isMultiCurrency
                    getCurrenciesStatusUpdates(
                        isSingleWalletWithToken = isSingleWalletWithToken,
                        isMultiCurrency = isMultiCurrency,
                    )
                },
                ifLeft = {
                    Timber.w(it.toString())
                    showAlertError()
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
                _isBalanceHiddenFlow.value = it.isBalanceHidden
            }
            .launchIn(modelScope)
            .saveIn(balanceHidingJobHolder)
    }

    private fun getCurrenciesStatusUpdates(isSingleWalletWithToken: Boolean, isMultiCurrency: Boolean) {
        getCurrencyStatus(
            isSingleWalletWithToken = isSingleWalletWithToken,
            isMultiCurrency = isMultiCurrency,
        ).onEach { maybeCryptoCurrency ->
            maybeCryptoCurrency.fold(
                ifRight = { cryptoCurrencyStatus ->
                    onDataLoaded(
                        currencyStatus = cryptoCurrencyStatus,
                        feeCurrencyStatus = getFeeCurrencyStatus(cryptoCurrencyStatus, isMultiCurrency),
                    )
                },
                ifLeft = {
                    sendConfirmAlertFactory.getGenericErrorState(
                        onFailedTxEmailClick = {
                            onFailedTxEmailClick(it.toString())
                        },
                        popBack = router::pop,
                    )
                },
            )
        }.launchIn(modelScope)
    }

    private fun getCurrencyStatus(
        isSingleWalletWithToken: Boolean,
        isMultiCurrency: Boolean,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return when {
            isSingleWalletWithToken -> getSingleCryptoCurrencyStatusUseCase.invokeMultiWallet(
                userWalletId = params.userWalletId,
                currencyId = cryptoCurrency.id,
                isSingleWalletWithTokens = true,
            )
            isMultiCurrency -> getSingleCryptoCurrencyStatusUseCase.invokeMultiWallet(
                userWalletId = params.userWalletId,
                currencyId = cryptoCurrency.id,
                isSingleWalletWithTokens = false,
            )
            else -> getSingleCryptoCurrencyStatusUseCase.invokeSingleWallet(userWalletId = params.userWalletId)
        }
    }

    private suspend fun getFeeCurrencyStatus(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        isMultiCurrency: Boolean,
    ): CryptoCurrencyStatus {
        return if (isMultiCurrency) {
            getFeePaidCryptoCurrencyStatusSyncUseCase(
                userWalletId = params.userWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            ).getOrNull() ?: cryptoCurrencyStatus
        } else {
            cryptoCurrencyStatus
        }
    }

    private fun onDataLoaded(currencyStatus: CryptoCurrencyStatus, feeCurrencyStatus: CryptoCurrencyStatus) {
        _cryptoCurrencyStatusFlow.value = currencyStatus
        _feeCryptoCurrencyStatusFlow.value = feeCurrencyStatus

        if (params.amount != null) {
            router.replaceAll(CommonSendRoute.Confirm)
        }
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
        )
        // If it is in active state use flow to update value in amount component
        modelScope.launch {
            amount?.let { sendAmountUpdateQRTrigger.triggerUpdateAmount(it) }
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

        val cardInfo =
            getCardInfoUseCase(userWallet.requireColdWallet().scanResponse).getOrNull() ?: return // TODO [REDACTED_TASK_KEY]

        modelScope.launch {
            sendFeedbackEmailUseCase(type = FeedbackEmailType.TransactionSendingProblem(cardInfo = cardInfo))
        }
    }

    private fun initialState(): SendUM = SendUM(
        amountUM = AmountState.Empty(isRedesignEnabled = sendFeatureToggles.isSendRedesignEnabled),
        destinationUM = SendDestinationInitialStateTransformer(
            cryptoCurrency = cryptoCurrency,
            isRedesignEnabled = sendFeatureToggles.isSendRedesignEnabled,
        ).transform(DestinationUM.Empty()),
        feeUM = FeeUM.Empty(),
        confirmUM = ConfirmUM.Empty,
        navigationUM = NavigationUM.Empty,
        isRedesignEnabled = sendFeatureToggles.isSendRedesignEnabled,
    )
}
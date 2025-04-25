package com.tangem.features.send.v2.send.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.utils.parseBigDecimal
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
import com.tangem.domain.tokens.GetCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.GetTransferFeeUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.send.v2.api.SendComponent
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.PredefinedValues
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.common.ui.state.NavigationUM
import com.tangem.features.send.v2.send.confirm.SendConfirmComponent
import com.tangem.features.send.v2.common.SendConfirmAlertFactory
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
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase,
    private val parseQrCodeUseCase: ParseQrCodeUseCase,
    private val sendConfirmAlertFactory: SendConfirmAlertFactory,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getTransferFeeUseCase: GetTransferFeeUseCase,
    private val sendAmountUpdateQRTrigger: SendAmountUpdateQRTrigger,
) : Model(), SendComponentCallback {

    private val params: SendComponent.Params = paramsContainer.require()
    private val userWalletId = params.userWalletId
    private val cryptoCurrency = params.currency

    private val _uiState = MutableStateFlow(initialState())
    val uiState = _uiState.asStateFlow()

    private val _isBalanceHiddenFlow = MutableStateFlow(false)
    val isBalanceHiddenFlow = _isBalanceHiddenFlow.asStateFlow()

    var userWallet: UserWallet by Delegates.notNull()
    var cryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    var feeCryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    var appCurrency: AppCurrency = AppCurrency.Default
    var predefinedValues: PredefinedValues = PredefinedValues.Empty

    private var balanceHidingJobHolder = JobHolder()

    init {
        subscribeOnBalanceHidden()
        subscribeOnQRScannerResult()
        subscribeOnCurrencyStatusUpdates()
        initAppCurrency()
    }

    override fun onNavigationResult(navigationUM: NavigationUM) {
        _uiState.update { it.copy(navigationUM = navigationUM) }
    }

    override fun onDestinationResult(destinationUM: DestinationUM) {
        _uiState.update { it.copy(destinationUM = destinationUM) }
    }

    override fun onAmountResult(amountUM: AmountState) {
        resetPredefinedAmount()
        _uiState.update { it.copy(amountUM = amountUM) }
    }

    override fun onFeeResult(feeUM: FeeUM) {
        _uiState.update { it.copy(feeUM = feeUM) }
    }

    override fun onResult(sendUM: SendUM) {
        resetPredefinedAmount()
        _uiState.update { sendUM }
    }

    suspend fun loadFee(): Either<GetFeeError, TransactionFee> {
        val destinationUM = uiState.value.destinationUM as? DestinationUM.Content ?: error("Invalid destination")
        val amountUM = uiState.value.amountUM as? AmountState.Data ?: error("Invalid amount")
        val enteredDestinationAddress = destinationUM.addressTextField.value
        val enteredAmount = amountUM.amountTextField.cryptoAmount.value ?: error("Invalid amount")

        return getTransferFeeUseCase(
            destination = enteredDestinationAddress,
            amount = enteredAmount,
            userWallet = userWallet,
            cryptoCurrency = params.currency,
        )
    }

    private fun resetPredefinedAmount() {
        // reset predefined amount
        val internalPredefinedValues = predefinedValues
        predefinedValues = when (internalPredefinedValues) {
            is PredefinedValues.Content.Deeplink -> internalPredefinedValues.copy(amount = "")
            is PredefinedValues.Content.QrCode -> internalPredefinedValues.copy(amount = "")
            PredefinedValues.Empty -> internalPredefinedValues
        }
    }

    private fun initAppCurrency() {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        modelScope.launch {
            getUserWalletUseCase(params.userWalletId).fold(
                ifRight = { wallet ->
                    userWallet = wallet

                    val isSingleWalletWithToken = wallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
                    val isMultiCurrency = wallet.isMultiCurrency
                    getCurrenciesStatusUpdates(
                        isSingleWalletWithToken = isSingleWalletWithToken,
                        isMultiCurrency = isMultiCurrency,
                    )
                },
                ifLeft = {
                    sendConfirmAlertFactory.getGenericErrorState(::onFailedTxEmailClick)
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
                    sendConfirmAlertFactory.getGenericErrorState {
                        onFailedTxEmailClick(it.toString())
                    }
                },
            )
        }.launchIn(modelScope)
    }

    private fun getCurrencyStatus(
        isSingleWalletWithToken: Boolean,
        isMultiCurrency: Boolean,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return if (isMultiCurrency) {
            getCurrencyStatusUpdatesUseCase(
                userWalletId = userWalletId,
                currencyId = cryptoCurrency.id,
                isSingleWalletWithTokens = isSingleWalletWithToken,
            )
        } else {
            getPrimaryCurrencyStatusUpdatesUseCase(userWalletId = userWalletId)
        }
    }

    private suspend fun getFeeCurrencyStatus(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        isMultiCurrency: Boolean,
    ): CryptoCurrencyStatus {
        return if (isMultiCurrency) {
            getFeePaidCryptoCurrencyStatusSyncUseCase(
                userWalletId = userWalletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            ).getOrNull() ?: cryptoCurrencyStatus
        } else {
            cryptoCurrencyStatus
        }
    }

    private fun onDataLoaded(currencyStatus: CryptoCurrencyStatus, feeCurrencyStatus: CryptoCurrencyStatus) {
        cryptoCurrencyStatus = currencyStatus
        feeCryptoCurrencyStatus = feeCurrencyStatus

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
                blockchainId = cryptoCurrency.network.id.value,
                derivationPath = cryptoCurrency.network.derivationPath.value,
                destinationAddress = "",
                tokenSymbol = "",
                amount = "",
                fee = "",
            ),
        )

        val cardInfo = getCardInfoUseCase(userWallet.scanResponse).getOrNull() ?: return

        modelScope.launch {
            sendFeedbackEmailUseCase(type = FeedbackEmailType.TransactionSendingProblem(cardInfo = cardInfo))
        }
    }

    private fun initialState(): SendUM = SendUM(
        amountUM = AmountState.Empty(),
        destinationUM = SendDestinationInitialStateTransformer(
            cryptoCurrency = cryptoCurrency,
        ).transform(DestinationUM.Empty()),
        feeUM = FeeUM.Empty(),
        confirmUM = ConfirmUM.Empty,
        navigationUM = NavigationUM.Empty,
    )
}
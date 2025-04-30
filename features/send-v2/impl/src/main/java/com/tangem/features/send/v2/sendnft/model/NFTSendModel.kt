package com.tangem.features.send.v2.sendnft.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.datasource.local.nft.converter.NFTSdkAssetConverter
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.tokens.GetCryptoCurrenciesUseCase
import com.tangem.domain.tokens.GetCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateNFTTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.send.v2.api.NFTSendComponent
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.SendConfirmAlertFactory
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.common.ui.state.NavigationUM
import com.tangem.features.send.v2.sendnft.confirm.NFTSendConfirmComponent
import com.tangem.features.send.v2.sendnft.ui.state.NFTSendUM
import com.tangem.features.send.v2.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import com.tangem.features.send.v2.subcomponents.fee.SendFeeComponent
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.properties.Delegates

internal interface SendNFTComponentCallback :
    SendFeeComponent.ModelCallback,
    SendDestinationComponent.ModelCallback,
    NFTSendConfirmComponent.ModelCallback

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class NFTSendModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getCryptoCurrenciesUseCase: GetCryptoCurrenciesUseCase,
    private val getCurrencyStatusUpdatesUseCase: GetCurrencyStatusUpdatesUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val createNFTTransferTransactionUseCase: CreateNFTTransferTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val alertFactory: SendConfirmAlertFactory,
) : Model(), SendNFTComponentCallback {

    val params: NFTSendComponent.Params = paramsContainer.require()
    private val userWalletId = params.userWalletId
    private val nftAsset = params.nftAsset

    private val _uiState = MutableStateFlow(initialState())
    val uiState = _uiState.asStateFlow()

    private val _isBalanceHiddenFlow = MutableStateFlow(false)
    val isBalanceHiddenFlow = _isBalanceHiddenFlow.asStateFlow()

    var cryptoCurrency: CryptoCurrency by Delegates.notNull()
    var userWallet: UserWallet by Delegates.notNull()
    var cryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    var feeCryptoCurrencyStatus: CryptoCurrencyStatus by Delegates.notNull()
    var appCurrency: AppCurrency = AppCurrency.Default

    init {
        subscribeOnCurrencyStatusUpdates()
        initAppCurrency()
    }

    override fun onNavigationResult(navigationUM: NavigationUM) {
        _uiState.update { it.copy(navigationUM = navigationUM) }
    }

    override fun onResult(nftSendUM: NFTSendUM) {
        _uiState.value = nftSendUM
    }

    override fun onDestinationResult(destinationUM: DestinationUM) {
        _uiState.update { it.copy(destinationUM = destinationUM) }
    }

    override fun onFeeResult(feeUM: FeeUM) {
        _uiState.update { it.copy(feeUM = feeUM) }
    }

    suspend fun loadFee(): Either<GetFeeError, TransactionFee> {
        val destinationUM = uiState.value.destinationUM as? DestinationUM.Content ?: error("Invalid destination")
        val ownerAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
            ?: error("Invalid owner address")
        val enteredDestinationAddress = destinationUM.addressTextField.value
        val enteredMemo = destinationUM.memoTextField?.value
        val nftAsset = NFTSdkAssetConverter.convertBack(params.nftAsset).second

        val nftSendTransaction = createNFTTransferTransactionUseCase(
            ownerAddress = ownerAddress,
            nftAsset = nftAsset,
            memo = enteredMemo,
            destinationAddress = enteredDestinationAddress,
            userWalletId = userWallet.walletId,
            network = cryptoCurrency.network,
        ).getOrElse {
            return GetFeeError.DataError(it).left()
        }

        return getFeeUseCase(
            transactionData = nftSendTransaction,
            network = cryptoCurrency.network,
            userWallet = userWallet,
        )
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

                    cryptoCurrency = getCryptoCurrenciesUseCase(userWalletId).getOrNull()
                        ?.filterIsInstance<CryptoCurrency.Coin>()
                        ?.firstOrNull { it.network == nftAsset.network }
                        ?: return@launch

                    getCurrenciesStatusUpdates(
                        isSingleWalletWithToken = wallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
                    )
                },
                ifLeft = {
                    alertFactory.getGenericErrorState(::onFailedTxEmailClick)
                    return@launch
                },
            )
        }
    }

    private fun onFailedTxEmailClick(errorMessage: String? = null) {
        saveBlockchainErrorUseCase(
            error = BlockchainErrorInfo(
                errorMessage = errorMessage.orEmpty(),
                blockchainId = cryptoCurrency.network.id.value,
                derivationPath = cryptoCurrency.network.derivationPath.value,
                destinationAddress = "",
                tokenSymbol = null,
                amount = "",
                fee = "",
            ),
        )

        val cardInfo = getCardInfoUseCase(userWallet.scanResponse).getOrNull() ?: return

        modelScope.launch {
            sendFeedbackEmailUseCase(type = FeedbackEmailType.TransactionSendingProblem(cardInfo = cardInfo))
        }
    }

    private fun getCurrenciesStatusUpdates(isSingleWalletWithToken: Boolean) {
        getCurrencyStatusUpdatesUseCase(
            userWalletId = userWalletId,
            currencyId = cryptoCurrency.id,
            isSingleWalletWithTokens = isSingleWalletWithToken,
        ).onEach { maybeCryptoCurrency ->
            maybeCryptoCurrency.fold(
                ifRight = { cryptoStatus ->
                    cryptoCurrencyStatus = cryptoStatus
                    feeCryptoCurrencyStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(
                        userWalletId = userWalletId,
                        cryptoCurrencyStatus = cryptoStatus,
                    ).getOrNull() ?: cryptoStatus

                    if (uiState.value.destinationUM is DestinationUM.Empty) {
                        router.replaceAll(CommonSendRoute.Destination(isEditMode = false))
                    }
                },
                ifLeft = {
                    alertFactory.getGenericErrorState {
                        onFailedTxEmailClick(it.toString())
                    }
                },
            )
        }.launchIn(modelScope)
    }

    private fun initialState(): NFTSendUM = NFTSendUM(
        destinationUM = DestinationUM.Empty(),
        feeUM = FeeUM.Empty(),
        confirmUM = ConfirmUM.Empty,
        navigationUM = NavigationUM.Empty,
    )
}
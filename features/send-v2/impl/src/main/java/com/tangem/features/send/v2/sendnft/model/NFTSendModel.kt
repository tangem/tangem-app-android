package com.tangem.features.send.v2.sendnft.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.datasource.local.nft.converter.NFTSdkAssetConverter
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.BlockchainErrorInfo
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.requireColdWallet
import com.tangem.domain.tokens.*
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.CreateNFTTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.nft.entity.NFTSendSuccessTrigger
import com.tangem.features.send.v2.api.NFTSendComponent
import com.tangem.features.send.v2.api.SendFeatureToggles
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.common.CommonSendRoute
import com.tangem.features.send.v2.common.CommonSendRoute.*
import com.tangem.features.send.v2.common.SendConfirmAlertFactory
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.sendnft.confirm.NFTSendConfirmComponent
import com.tangem.features.send.v2.sendnft.success.NFTSendSuccessComponent
import com.tangem.features.send.v2.sendnft.ui.state.NFTSendUM
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
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val createNFTTransferTransactionUseCase: CreateNFTTransferTransactionUseCase,
    private val getFeeUseCase: GetFeeUseCase,
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val alertFactory: SendConfirmAlertFactory,
    private val sendFeatureToggles: SendFeatureToggles,
    private val nftSendSuccessTrigger: NFTSendSuccessTrigger,
) : Model(), SendNFTComponentCallback, NFTSendSuccessComponent.ModelCallback {

    val params: NFTSendComponent.Params = paramsContainer.require()

    val initialRoute = Empty

    val currentRouteFlow = MutableStateFlow<CommonSendRoute>(initialRoute)

    private val userWalletId = params.userWalletId
    private val nftAsset = params.nftAsset

    val uiState: StateFlow<NFTSendUM>
    field = MutableStateFlow(initialState())

    val isBalanceHiddenFlow: StateFlow<Boolean>
    field = MutableStateFlow(false)

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
        uiState.update { it.copy(navigationUM = navigationUM) }
    }

    override fun onResult(nftSendUM: NFTSendUM) {
        uiState.value = nftSendUM
    }

    override fun onDestinationResult(destinationUM: DestinationUM) {
        uiState.update { it.copy(destinationUM = destinationUM) }
    }

    override fun onFeeResult(feeUM: FeeUM) {
        uiState.update { it.copy(feeUM = feeUM) }
    }

    override fun onBackClick() {
        if (currentRouteFlow.value == ConfirmSuccess) {
            modelScope.launch {
                nftSendSuccessTrigger.triggerSuccessNFTSend()
            }
        }
        router.pop()
    }

    override fun onNextClick() {
        if (currentRouteFlow.value.isEditMode) {
            onBackClick()
        } else {
            when (currentRouteFlow.value) {
                is Destination -> router.push(Confirm)
                Confirm -> router.replaceAll(ConfirmSuccess)
                else -> onBackClick()
            }
        }
    }

    suspend fun loadFee(): Either<GetFeeError, TransactionFee> {
        val destinationUM = uiState.value.destinationUM as? DestinationUM.Content ?: error("Invalid destination")
        val ownerAddress = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
            ?: error("Invalid owner address")
        val enteredDestinationAddress = destinationUM.addressTextField.actualAddress
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

                    cryptoCurrency = if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
                        multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                            params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
                        )
                            ?.firstOrNull { it is CryptoCurrency.Coin && it.network == nftAsset.network }
                    } else {
                        getCryptoCurrenciesUseCase(userWalletId).getOrNull()
                            ?.filterIsInstance<CryptoCurrency.Coin>()
                            ?.firstOrNull { it.network == nftAsset.network }
                    }
                        ?: return@launch

                    getCurrenciesStatusUpdates(
                        isSingleWalletWithToken = wallet is UserWallet.Cold &&
                            wallet.scanResponse.cardTypesResolver.isSingleWalletWithToken(),
                    )
                },
                ifLeft = {
                    alertFactory.getGenericErrorState(::onFailedTxEmailClick)
                    return@launch
                },
            )
        }
    }

    fun showAlertError() {
        alertFactory.getGenericErrorState(
            onFailedTxEmailClick = ::onFailedTxEmailClick,
            popBack = router::pop,
        )
    }

    private fun onFailedTxEmailClick(errorMessage: String? = null) {
        saveBlockchainErrorUseCase(
            error = BlockchainErrorInfo(
                errorMessage = errorMessage.orEmpty(),
                blockchainId = cryptoCurrency.network.rawId,
                derivationPath = cryptoCurrency.network.derivationPath.value,
                destinationAddress = "",
                tokenSymbol = null,
                amount = "",
                fee = "",
            ),
        )

        if (userWallet is UserWallet.Hot) {
            return // TODO [REDACTED_TASK_KEY] [Hot Wallet] Email feedback flow
        }

        val cardInfo =
            getCardInfoUseCase(userWallet.requireColdWallet().scanResponse).getOrNull() ?: return

        modelScope.launch {
            sendFeedbackEmailUseCase(type = FeedbackEmailType.TransactionSendingProblem(cardInfo = cardInfo))
        }
    }

    private fun getCurrenciesStatusUpdates(isSingleWalletWithToken: Boolean) {
        getSingleCryptoCurrencyStatusUseCase.invokeMultiWallet(
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
                    alertFactory.getGenericErrorState(
                        onFailedTxEmailClick = { onFailedTxEmailClick(it.toString()) },
                        popBack = { router.pop() },
                    )
                },
            )
        }.launchIn(modelScope)
    }

    private fun initialState(): NFTSendUM = NFTSendUM(
        destinationUM = DestinationUM.Empty(),
        feeUM = FeeUM.Empty(),
        feeSelectorUM = FeeSelectorUM.Loading,
        confirmUM = ConfirmUM.Empty,
        navigationUM = NavigationUM.Empty,
        isRedesignEnabled = sendFeatureToggles.isNFTSendRedesignEnabled,
    )
}
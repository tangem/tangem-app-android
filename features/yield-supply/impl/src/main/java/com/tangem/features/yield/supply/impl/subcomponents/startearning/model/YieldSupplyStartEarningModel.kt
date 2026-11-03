package com.tangem.features.yield.supply.impl.subcomponents.startearning.model

import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.EthereumYieldSupplyEnterCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.unwrapYieldModuleUpgrade
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.YieldModuleSameTokenRule
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.isGasless
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForGaslessBatchUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldSupplyFee
import com.tangem.domain.yield.supply.models.YieldSupplyMaxFee
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import com.tangem.domain.yield.supply.usecase.*
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.YieldSupplyAlertFactory
import com.tangem.features.yield.supply.impl.common.YieldSupplyFeeLoads
import com.tangem.features.yield.supply.impl.common.YieldSupplySmartGas
import com.tangem.features.yield.supply.impl.common.YieldSupplySmartGasFee
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.common.entity.transformer.YieldSupplyTransactionInProgressTransformer
import com.tangem.features.yield.supply.impl.common.entity.transformer.YieldSupplyTransactionReadyTransformer
import com.tangem.features.yield.supply.impl.common.feeAssetType
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsComponent
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsUpdateTrigger
import com.tangem.features.yield.supply.impl.subcomponents.notifications.entity.YieldSupplyNotificationData
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningComponent
import com.tangem.features.yield.supply.impl.subcomponents.startearning.model.transformers.YieldSupplyStartEarningFeeContentTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class YieldSupplyStartEarningModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val analytics: AnalyticsEventHandler,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val yieldSupplyStartEarningUseCase: YieldSupplyStartEarningUseCase,
    private val yieldSupplyEstimateEnterFeeUseCase: YieldSupplyEstimateEnterFeeUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val yieldSupplyNotificationsUpdateTrigger: YieldSupplyNotificationsUpdateTrigger,
    private val yieldSupplyAlertFactory: YieldSupplyAlertFactory,
    private val yieldSupplyMinAmountUseCase: YieldSupplyMinAmountUseCase,
    private val yieldSupplyGetMaxFeeUseCase: YieldSupplyGetMaxFeeUseCase,
    private val yieldSupplyGetCurrentFeeUseCase: YieldSupplyGetCurrentFeeUseCase,
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val yieldSupplyPendingTracker: YieldSupplyPendingTracker,
    private val appsFlyerRepository: AppsFlyerRepository,
    private val yieldSupplyFeatureToggles: YieldSupplyFeatureToggles,
    private val estimateFeeForGaslessBatchUseCase: EstimateFeeForGaslessBatchUseCase,
    private val yieldSupplyApplyEnterAmountUseCase: YieldSupplyApplyEnterAmountUseCase,
    private val yieldSupplySmartGas: YieldSupplySmartGas,
) : Model(), YieldSupplyNotificationsComponent.ModelCallback {

    private val params: YieldSupplyStartEarningComponent.Params = paramsContainer.require()

    private val cryptoCurrency = params.cryptoCurrency
    private val userWalletId = params.userWalletId
    private var minAmount: BigDecimal by Delegates.notNull()
    var userWallet: UserWallet by Delegates.notNull()

    val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                currency = cryptoCurrency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )
    val feeCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                currency = cryptoCurrency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )

    val uiState: StateFlow<YieldSupplyActionUM>
        field: MutableStateFlow<YieldSupplyActionUM> = MutableStateFlow(
            YieldSupplyActionUM(
                title = resourceReference(R.string.yield_module_start_earning),
                subtitle = resourceReference(
                    R.string.yield_module_start_earning_sheet_description,
                    wrappedList(cryptoCurrency.symbol),
                ),
                footer = resourceReference(
                    R.string.yield_module_start_earning_sheet_next_deposits_v2,
                    wrappedList(cryptoCurrency.symbol),
                ),
                footerLink = resourceReference(R.string.yield_module_start_earning_sheet_fee_policy),
                currencyIconState = CryptoCurrencyToIconStateConverter().convert(params.cryptoCurrency),
                yieldSupplyFeeUM = YieldSupplyFeeUM.Loading,
                isPrimaryButtonEnabled = false,
                isTransactionSending = false,
                isHoldToConfirmEnabled = false,
            ),
        )

    private val cryptoCurrencyStatus
        get() = cryptoCurrencyStatusFlow.value
    private var nativeFeeCurrencyStatus: CryptoCurrencyStatus = feeCryptoCurrencyStatusFlow.value
    private var appCurrency = AppCurrency.Default
    private val feeLoads = YieldSupplyFeeLoads()

    init {
        analytics.send(
            YieldSupplyAnalytics.StartEarningScreen(
                token = params.cryptoCurrency.symbol,
                blockchain = params.cryptoCurrency.network.name,
            ),
        )
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
            subscribeOnCurrencyStatusUpdates()
            subscribeOnNotificationsErrors()
        }
    }

    private suspend fun calculateMinAmount(cryptoCurrencyStatus: CryptoCurrencyStatus): BigDecimal {
        return yieldSupplyMinAmountUseCase(userWalletId, cryptoCurrencyStatus).getOrElse { BigDecimal.ZERO }
    }

    private suspend fun onLoadFee(load: Int) {
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Loading || uiState.value.isTransactionSending) return
        if (!feeLoads.takeOver(load, currentCoroutineContext().job)) return

        uiState.update {
            it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Loading)
        }

        val maxFee = yieldSupplyGetMaxFeeUseCase(userWalletId, cryptoCurrencyStatus).getOrNull()
        val estimatedFee = yieldSupplyGetCurrentFeeUseCase(userWalletId, cryptoCurrencyStatus).getOrNull()

        if (estimatedFee == null || maxFee == null) {
            showFeeUnknownError()
            return
        }

        val transactionListData = yieldSupplyStartEarningUseCase(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxNetworkFee = maxFee.tokenMaxFee,
        ).getOrNull()

        if (transactionListData == null) {
            showFeeUnknownError()
            return
        }

        yieldSupplyEstimateEnterFeeUseCase.invoke(
            userWallet = userWallet,
            cryptoCurrency = nativeFeeCurrencyStatus.currency,
            transactionDataList = transactionListData,
        ).fold(
            ifLeft = { feeError ->
                feeCryptoCurrencyStatusFlow.update { nativeFeeCurrencyStatus }
                yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
                    data = YieldSupplyNotificationData(
                        feeValue = null,
                        feeError = feeError,
                    ),
                )
                uiState.update {
                    it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Error)
                }
            },
            ifRight = { updatedTransactionList ->
                onEnterFeeEstimated(updatedTransactionList, maxFee, estimatedFee)
            },
        )
    }

    private suspend fun onEnterFeeEstimated(
        updatedTransactionList: List<TransactionData.Uncompiled>,
        maxFee: YieldSupplyMaxFee,
        estimatedFee: YieldSupplyFee,
    ) {
        val smartGasFee = if (yieldSupplyFeatureToggles.isSmartGasEnabled) {
            resolveSmartGasFee(updatedTransactionList)
        } else {
            null
        }
        val feeSum = smartGasFee?.feeAmount ?: updatedTransactionList.sumOf {
            it.fee?.amount?.value.orZero()
        }
        feeCryptoCurrencyStatusFlow.update { smartGasFee?.feeTokenStatus ?: nativeFeeCurrencyStatus }

        uiState.update(
            YieldSupplyStartEarningFeeContentTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatusFlow.value,
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatusFlow.value,
                appCurrency = appCurrency,
                updatedTransactionList = smartGasFee?.transactions ?: updatedTransactionList,
                feeValue = feeSum,
                maxNetworkFee = maxFee,
                estimatedFeeValueInTokenCurrency = estimatedFee.value,
                minAmount = minAmount,
                gaslessFee = smartGasFee?.fee,
            ),
        )
        yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
            data = YieldSupplyNotificationData(
                feeValue = feeSum,
                feeError = null,
                shouldShowHighFeeNotification = estimatedFee.isHighFee,
            ),
        )
    }

    /**
     * Returns null — and the native fee stays — whenever Smart Gas can't pay: the native coin covers the batch, the
     * fee token isn't in the portfolio, or the enter can't leave a same-token fee on the wallet and no other token
     * can pay.
     */
    private suspend fun resolveSmartGasFee(calls: List<TransactionData.Uncompiled>): YieldSupplySmartGasFee? {
        val operationTokenStatus = cryptoCurrencyStatus
        val fee = estimateSmartGasFee(calls, isOperationTokenExcluded = false) ?: return null
        val feeAmount = fee.feeAmount() ?: return null

        if (fee.feeTokenId != cryptoCurrency.id) return otherTokenSmartGasFee(fee, feeAmount, calls)

        val transactions = yieldSupplyApplyEnterAmountUseCase(
            userWalletId = userWalletId,
            cryptoCurrencyStatus = operationTokenStatus,
            calls = calls,
            feeAmount = feeAmount,
        ).getOrElse { error ->
            TangemLogger.e("Yield Mode enter can't leave the Smart Gas fee on the wallet", error)
            val otherTokenFee = estimateSmartGasFee(calls, isOperationTokenExcluded = true) ?: return null
            if (otherTokenFee.feeTokenId == cryptoCurrency.id) return null
            val otherTokenFeeAmount = otherTokenFee.feeAmount() ?: return null
            return otherTokenSmartGasFee(otherTokenFee, otherTokenFeeAmount, calls)
        }
        return YieldSupplySmartGasFee(
            fee = fee,
            feeTokenStatus = operationTokenStatus,
            feeAmount = feeAmount,
            transactions = transactions,
        )
    }

    /** Returns only a token-paid fee: null when the estimate fails, the network isn't gasless or the coin pays. */
    private suspend fun estimateSmartGasFee(
        calls: List<TransactionData.Uncompiled>,
        isOperationTokenExcluded: Boolean,
    ): TransactionFeeExtended? {
        val fee = estimateFeeForGaslessBatchUseCase(
            userWallet = userWallet,
            operationTokenStatus = cryptoCurrencyStatus,
            calls = calls,
            sameTokenRule = YieldModuleSameTokenRule.Enter,
            isOperationTokenExcluded = isOperationTokenExcluded,
        ).getOrElse { error ->
            TangemLogger.e("Yield Mode Smart Gas fee estimate failed: $error")
            return null
        }
        return fee?.takeIf { it.isGasless }
    }

    private fun TransactionFeeExtended.feeAmount(): BigDecimal? {
        return transactionFee.normal.amount.value
            ?: null.also { TangemLogger.e("Yield Mode Smart Gas fee has no amount") }
    }

    private suspend fun otherTokenSmartGasFee(
        fee: TransactionFeeExtended,
        feeAmount: BigDecimal,
        calls: List<TransactionData.Uncompiled>,
    ): YieldSupplySmartGasFee? {
        val feeTokenStatus = yieldSupplySmartGas.getFeeTokenStatus(
            userWalletId = userWalletId,
            feeTokenId = fee.feeTokenId,
            network = cryptoCurrency.network,
        )
        if (feeTokenStatus == null) {
            TangemLogger.e("Yield Mode Smart Gas fee token ${fee.feeTokenId} is not in the portfolio")
            return null
        }
        return YieldSupplySmartGasFee(
            fee = fee,
            feeTokenStatus = feeTokenStatus,
            feeAmount = feeAmount,
            transactions = calls,
        )
    }

    fun onClick() {
        val yieldSupplyFeeUM = uiState.value.yieldSupplyFeeUM as? YieldSupplyFeeUM.Content ?: return
        val feeCurrency = yieldSupplyFeeUM.feeCurrency ?: feeCryptoCurrencyStatusFlow.value.currency
        analytics.send(
            YieldSupplyAnalytics.ButtonStartEarning(
                token = params.cryptoCurrency.symbol,
                blockchain = params.cryptoCurrency.network.name,
            ),
        )

        uiState.update(YieldSupplyTransactionInProgressTransformer)
        modelScope.launch(dispatchers.default) {
            yieldSupplySmartGas.sendTransactions(
                userWallet = userWallet,
                network = cryptoCurrency.network,
                yieldSupplyFeeUM = yieldSupplyFeeUM,
            ).fold(
                ifLeft = { error ->
                    TangemLogger.e(error.toString())
                    uiState.update(YieldSupplyTransactionReadyTransformer)
                    analytics.send(
                        YieldSupplyAnalytics.EarnErrors(
                            action = YieldSupplyAnalytics.Action.Start,
                            errorDescription = error.getAnalyticsDescription(),
                        ),
                    )
                    yieldSupplyAlertFactory.getSendTransactionErrorState(
                        error = error,
                        popBack = params.callback::onBackClick,
                        onFailedTxEmailClick = { errorMessage ->
                            modelScope.launch(dispatchers.default) {
                                yieldSupplyAlertFactory.onFailedTxEmailClick(
                                    userWallet = userWallet,
                                    cryptoCurrency = cryptoCurrency,
                                    errorMessage = errorMessage,
                                )
                            }
                        },
                    )
                },
                ifRight = { txsData ->
                    onStartEarningTransactionSuccess(yieldSupplyFeeUM, feeCurrency, txsData)
                },
            )
        }
    }

    private suspend fun onStartEarningTransactionSuccess(
        yieldSupplyFeeUM: YieldSupplyFeeUM.Content,
        feeCurrency: CryptoCurrency,
        txsData: List<String>,
    ) {
        yieldSupplyRepository.saveTokenProtocolPendingStatus(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
            yieldSupplyPendingStatus = YieldSupplyPendingStatus.Enter(txsData),
        )
        val event = AnalyticsParam.TxSentFrom.Earning(
            blockchain = cryptoCurrency.network.name,
            token = cryptoCurrency.symbol,
            feeType = AnalyticsParam.FeeType.Normal,
            feeToken = feeCurrency.symbol,
            feeAssetType = yieldSupplyFeeUM.feeAssetType(feeCurrency),
        )
        analytics.send(
            YieldSupplyAnalytics.FundsEarned(
                blockchain = cryptoCurrency.network.name,
                token = cryptoCurrency.symbol,
                referralId = appsFlyerRepository.getConversionData()?.refcode,
            ),
        )
        // A Smart Gas batch goes on-chain as one meta-transaction.
        val sentTransactionsCount = if (yieldSupplyFeeUM.gaslessFee != null) {
            1
        } else {
            yieldSupplyFeeUM.transactionDataList.size
        }
        repeat(sentTransactionsCount) {
            analytics.send(
                Basic.TransactionSent(
                    sentFrom = event,
                    memoType = Basic.TransactionSent.MemoType.Null,
                    isCustomToken = cryptoCurrency.isCustom,
                    isCustomFeeToken = feeCurrency.isCustom,
                ),
            )
        }

        // Auto top-up enrolment is deliberately NOT sent here. It dispatches the relayer, which performs the
        // same deposit as our own enter transaction, so announcing it before ours has settled makes the two
        // race and the loser reverts with ZeroAmount(). The tracker sends it once the outcome is known — or
        // straight away when this batch carries no deposit, since then there is nothing to race.
        val address = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
        val depositContext = address?.let { ownerAddress ->
            YieldSupplyPendingTracker.DepositContext(
                ownerAddress = ownerAddress,
                enterTransactionHash = enterTransactionHash(yieldSupplyFeeUM, txsData),
            )
        }

        modelScope.launch {
            yieldSupplyPendingTracker.addPending(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
                txIds = txsData,
                depositContext = depositContext,
            )
            params.callback.onTransactionSent()
        }
    }

    private fun enterTransactionHash(yieldSupplyFeeUM: YieldSupplyFeeUM.Content, txHashes: List<String>): String? {
        if (yieldSupplyFeeUM.gaslessFee != null) return txHashes.singleOrNull()

        val index = yieldSupplyFeeUM.transactionDataList.indexOfFirst { transactionData ->
            val callData = (transactionData.extras as? EthereumTransactionExtras)?.callData
                ?.unwrapYieldModuleUpgrade()
            callData is EthereumYieldSupplyEnterCallData
        }

        return txHashes.getOrNull(index)
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        modelScope.launch {
            getUserWalletUseCase(userWalletId).fold(
                ifRight = { wallet ->
                    userWallet = wallet
                    uiState.update {
                        it.copy(
                            isHoldToConfirmEnabled = wallet.isHotWallet,
                        )
                    }
                    getCurrenciesStatusUpdates()
                },
                ifLeft = { error ->
                    TangemLogger.w(error.toString())
                    showAlertError()
                },
            )
        }
    }

    private fun subscribeOnNotificationsErrors() {
        yieldSupplyNotificationsUpdateTrigger.hasErrorFlow
            .onEach { hasError ->
                uiState.update {
                    it.copy(isPrimaryButtonEnabled = !hasError)
                }
            }.launchIn(modelScope)
    }

    override fun onFeeReload() {
        val load = feeLoads.start()
        modelScope.launch { onLoadFee(load) }
    }

    private fun getCurrenciesStatusUpdates() {
        singleAccountStatusListSupplier(params.userWalletId)
            .map { it.getCryptoCurrencyStatus(currency = cryptoCurrency) }
            .distinctUntilChanged()
            .onEach { maybeCryptoCurrency ->
                maybeCryptoCurrency.fold(
                    ifSome = { cryptoCurrencyStatus ->
                        onDataLoaded(
                            currencyStatus = cryptoCurrencyStatus,
                            feeCurrencyStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(
                                userWalletId = userWalletId,
                                cryptoCurrencyStatus = cryptoCurrencyStatus,
                            ).getOrNull() ?: cryptoCurrencyStatus,
                        )
                    },
                    ifEmpty = {
                        TangemLogger.w("Unable to get crypto currency status: ${cryptoCurrency.id}")
                        showAlertError()
                    },
                )
            }
            .launchIn(modelScope)
    }

    private fun onDataLoaded(currencyStatus: CryptoCurrencyStatus, feeCurrencyStatus: CryptoCurrencyStatus) {
        cryptoCurrencyStatusFlow.update { currencyStatus }
        nativeFeeCurrencyStatus = feeCurrencyStatus

        val load = feeLoads.start()
        modelScope.launch {
            val calculatedMinAmount = calculateMinAmount(currencyStatus)
            if (feeLoads.isSuperseded(load)) return@launch
            minAmount = calculatedMinAmount
            onLoadFee(load)
        }
    }

    private fun showAlertError() {
        yieldSupplyAlertFactory.getGenericErrorState(
            onFailedTxEmailClick = {
                modelScope.launch(dispatchers.default) {
                    yieldSupplyAlertFactory.onFailedTxEmailClick(
                        userWallet,
                        cryptoCurrency,
                        null,
                    )
                }
            },
            popBack = params.callback::onBackClick,
        )
    }

    private suspend fun showFeeUnknownError() {
        feeCryptoCurrencyStatusFlow.update { nativeFeeCurrencyStatus }
        uiState.update {
            it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Error)
        }
        yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
            data = YieldSupplyNotificationData(
                feeValue = null,
                feeError = GetFeeError.UnknownError,
            ),
        )
    }
}
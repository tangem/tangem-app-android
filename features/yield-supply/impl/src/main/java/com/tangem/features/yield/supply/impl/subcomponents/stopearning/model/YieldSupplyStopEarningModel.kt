package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.MerklClaimCallData
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.unwrapYieldModuleUpgrade
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.YieldModuleSameTokenRule
import com.tangem.domain.transaction.models.isGasless
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.gasless.EstimateFeeForGaslessBatchUseCase
import com.tangem.domain.transaction.usecase.gasless.IsEvmGaslessFeeSupportedForNetwork
import com.tangem.domain.yield.supply.INCREASE_GAS_LIMIT_FOR_SUPPLY
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.increaseGasLimitBy
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOrigin
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyDeactivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyPendingTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyStopEarningUseCase
import com.tangem.features.send.api.subcomponents.feeSelector.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.api.subcomponents.feeSelector.params.FeeSelectorParams
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.YieldSupplyAlertFactory
import com.tangem.features.yield.supply.impl.common.YieldSupplyFeeCurrencyChoice
import com.tangem.features.yield.supply.impl.common.YieldSupplyFeeLoads
import com.tangem.features.yield.supply.impl.common.YieldSupplySmartGas
import com.tangem.features.yield.supply.impl.common.YieldSupplySmartGasFee
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.common.entity.transformer.YieldSupplyTransactionInProgressTransformer
import com.tangem.features.yield.supply.impl.common.entity.transformer.YieldSupplyTransactionReadyTransformer
import com.tangem.features.yield.supply.impl.common.feeAssetType
import com.tangem.features.yield.supply.impl.common.toPickedFeeCurrencyError
import com.tangem.features.yield.supply.impl.incentives.claim.model.YieldSupplyClaimPreparer
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsComponent
import com.tangem.features.yield.supply.impl.subcomponents.notifications.YieldSupplyNotificationsUpdateTrigger
import com.tangem.features.yield.supply.impl.subcomponents.notifications.entity.YieldSupplyNotificationData
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.YieldSupplyStopEarningComponent
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.model.transformer.YieldSupplyStopEarningFeeContentTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.logging.TangemLogger
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class YieldSupplyStopEarningModel @Inject constructor(
    private val tangemBlogUrlBuilder: TangemBlogUrlBuilder,
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val analytics: AnalyticsEventHandler,
    private val getFeeUseCase: GetFeeUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val yieldSupplyStopEarningUseCase: YieldSupplyStopEarningUseCase,
    private val urlOpener: UrlOpener,
    private val yieldSupplyNotificationsUpdateTrigger: YieldSupplyNotificationsUpdateTrigger,
    private val yieldSupplyAlertFactory: YieldSupplyAlertFactory,
    private val yieldSupplyDeactivateUseCase: YieldSupplyDeactivateUseCase,
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val yieldSupplyPendingTracker: YieldSupplyPendingTracker,
    private val appsFlyerRepository: AppsFlyerRepository,
    private val yieldSupplyWithdrawalClaimPreparer: YieldSupplyWithdrawalClaimPreparer,
    private val yieldSupplyClaimTracker: YieldSupplyClaimTracker,
    private val yieldSupplyFeatureToggles: YieldSupplyFeatureToggles,
    private val estimateFeeForGaslessBatchUseCase: EstimateFeeForGaslessBatchUseCase,
    private val yieldSupplySmartGas: YieldSupplySmartGas,
    isEvmGaslessFeeSupportedForNetwork: IsEvmGaslessFeeSupportedForNetwork,
) : Model(), YieldSupplyNotificationsComponent.ModelCallback {

    private val params: YieldSupplyStopEarningComponent.Params = paramsContainer.require()

    private val cryptoCurrencyStatus
        get() = params.cryptoCurrencyStatusFlow.value
    private val cryptoCurrency = cryptoCurrencyStatus.currency
    private val userWallet = params.userWallet

    val feeCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                currency = cryptoCurrencyStatus.currency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )

    private var appCurrency = AppCurrency.Default
    private val feeLoads = YieldSupplyFeeLoads()

    private var withdrawalClaim: YieldPreClaim.Ready? = null
    private var pickedFeeCurrencyStatus: CryptoCurrencyStatus? = null
    private var feeCurrencyChoice: YieldSupplyFeeCurrencyChoice? = null
    private var nativeFeeCurrencyStatus: CryptoCurrencyStatus = feeCryptoCurrencyStatusFlow.value
    private val isSmartGasNetwork = yieldSupplyFeatureToggles.isSmartGasEnabled &&
        isEvmGaslessFeeSupportedForNetwork(cryptoCurrency.network)

    val uiState: StateFlow<YieldSupplyActionUM>
        field: MutableStateFlow<YieldSupplyActionUM> = MutableStateFlow(
            YieldSupplyActionUM(
                title = resourceReference(R.string.yield_module_stop_earning_sheet_title),
                subtitle = resourceReference(
                    id = R.string.yield_module_stop_earning_sheet_description,
                    formatArgs = wrappedList(cryptoCurrency.symbol),
                ),
                footer = resourceReference(R.string.yield_module_stop_earning_sheet_fee_note),
                footerLink = resourceReference(R.string.common_read_more),
                currencyIconState = CryptoCurrencyToIconStateConverter().convert(cryptoCurrency),
                yieldSupplyFeeUM = YieldSupplyFeeUM.Loading,
                isPrimaryButtonEnabled = false,
                isTransactionSending = false,
                isHoldToConfirmEnabled = params.userWallet.isHotWallet,
            ),
        )

    init {
        val currency = params.cryptoCurrencyStatusFlow.value.currency
        analytics.send(
            YieldSupplyAnalytics.StopEarningScreen(
                token = currency.symbol,
                blockchain = currency.network.name,
            ),
        )
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
            onFeeReload()
            subscribeOnNotificationsErrors()
        }
    }

    override fun onFeeReload() {
        val load = feeLoads.start()
        modelScope.launch { onLoadFee(load) }
    }

    fun createFeeSelectorParams(callback: FeeSelectorModelCallback): FeeSelectorParams.FeeSelectorDetailsParams? {
        return feeCurrencyChoice?.toFeeSelectorParams(
            userWalletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            nativeFeeCurrencyStatus = nativeFeeCurrencyStatus,
            callback = callback,
        )
    }

    fun onFeeCurrencyPicked(feeCurrencyStatus: CryptoCurrencyStatus) {
        pickedFeeCurrencyStatus = feeCurrencyStatus
        onFeeReload()
    }

    fun onReadMoreClick() {
        modelScope.launch {
            urlOpener.openUrl(tangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.WhatIsTransactionFee))
        }
    }

    fun onClick() {
        val yieldSupplyFeeUM = uiState.value.yieldSupplyFeeUM as? YieldSupplyFeeUM.Content ?: return
        params.callback.onTransactionProgress(true)
        val feeCurrency = yieldSupplyFeeUM.feeCurrency ?: feeCryptoCurrencyStatusFlow.value.currency
        analytics.send(
            YieldSupplyAnalytics.ButtonStopEarning(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            ),
        )
        uiState.update(YieldSupplyTransactionInProgressTransformer)

        val claimIndex = yieldSupplyFeeUM.transactionDataList.indexOfFirst {
            (it.extras as? EthereumTransactionExtras)?.callData?.unwrapYieldModuleUpgrade() is MerklClaimCallData
        }
        // Safe to pair with the fee content: Content means the latest load has written both the claim and the fee
        val claim = withdrawalClaim?.takeIf { claimIndex != -1 }
        if (claim != null && claim.preparedAt.elapsedNow() >= YieldSupplyClaimPreparer.PREPARATION_VALIDITY) {
            uiState.update(YieldSupplyTransactionReadyTransformer)
            params.callback.onTransactionProgress(false)
            onFeeReload()
            return
        }

        modelScope.launch(dispatchers.default) {
            yieldSupplySmartGas.sendTransactions(
                userWallet = userWallet,
                network = cryptoCurrency.network,
                yieldSupplyFeeUM = yieldSupplyFeeUM,
            ).fold(
                ifLeft = { error ->
                    TangemLogger.e(error.toString())
                    uiState.update(YieldSupplyTransactionReadyTransformer)
                    if (claim != null && error !is SendTransactionError.UserCancelledError) holdUnresolvedClaim(claim)
                    analytics.send(
                        YieldSupplyAnalytics.EarnErrors(
                            action = YieldSupplyAnalytics.Action.Stop,
                            errorDescription = error.getAnalyticsDescription(),
                        ),
                    )
                    yieldSupplyAlertFactory.getSendTransactionErrorState(
                        error = error,
                        popBack = params.callback::onDismissClick,
                        onFailedTxEmailClick = { errorMessage ->
                            modelScope.launch(dispatchers.default) {
                                yieldSupplyAlertFactory.onFailedTxEmailClick(
                                    userWallet = params.userWallet,
                                    cryptoCurrency = cryptoCurrency,
                                    errorMessage = errorMessage,
                                )
                            }
                        },
                    )
                    params.callback.onTransactionProgress(false)
                },
                ifRight = { hashes ->
                    // A Smart Gas batch goes on-chain as one transaction, so its hash is the claim's too
                    val claimHash = if (yieldSupplyFeeUM.gaslessFee != null) {
                        hashes.single()
                    } else {
                        hashes.getOrNull(claimIndex)
                    }
                    if (claim != null && claimHash != null) trackWithdrawalClaim(claim, transactionHash = claimHash)
                    onStopEarningTransactionSuccess(yieldSupplyFeeUM, feeCurrency, hashes.last())
                },
            )
        }
    }

    /**
     * A failed batch does not say whether the claim at its head was broadcast, so the module is held as if it were
     * and the fee is reloaded: the retry then goes without the claim instead of signing the same proof again.
     */
    private fun holdUnresolvedClaim(claim: YieldPreClaim.Ready) {
        yieldSupplyClaimTracker.trackUnresolved(
            chainId = claim.chainId,
            userAddress = claim.userAddress,
            claimedRewards = claim.claimedRewards,
        )
        onFeeReload()
    }

    private fun trackWithdrawalClaim(claim: YieldPreClaim.Ready, transactionHash: String) {
        yieldSupplyClaimTracker.track(
            userWalletId = userWallet.walletId,
            cryptoCurrency = claim.token,
            chainId = claim.chainId,
            userAddress = claim.userAddress,
            transactionHash = transactionHash,
            claimedRewards = claim.claimedRewards,
            origin = YieldSupplyClaimOrigin.Withdrawal,
        )
    }

    private suspend fun onStopEarningTransactionSuccess(
        yieldSupplyFeeUM: YieldSupplyFeeUM.Content,
        feeCurrency: CryptoCurrency,
        txId: String,
    ) {
        yieldSupplyRepository.saveTokenProtocolPendingStatus(
            userWalletId = userWallet.walletId,
            cryptoCurrency = cryptoCurrency,
            yieldSupplyPendingStatus = YieldSupplyPendingStatus.Exit(listOf(txId)),
        )
        analytics.send(
            YieldSupplyAnalytics.FundsWithdrawn(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
                referralId = appsFlyerRepository.getConversionData()?.refcode,
            ),
        )
        val event = AnalyticsParam.TxSentFrom.Earning(
            blockchain = cryptoCurrency.network.name,
            token = cryptoCurrency.symbol,
            feeType = AnalyticsParam.FeeType.Normal,
            feeToken = feeCurrency.symbol,
            feeAssetType = yieldSupplyFeeUM.feeAssetType(feeCurrency),
        )
        analytics.send(
            Basic.TransactionSent(
                sentFrom = event,
                memoType = Basic.TransactionSent.MemoType.Null,
                isCustomToken = cryptoCurrency.isCustom,
                isCustomFeeToken = feeCurrency.isCustom,
            ),
        )
        val address = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
        if (address != null) {
            yieldSupplyDeactivateUseCase(cryptoCurrency, address)
        }

        modelScope.launch {
            yieldSupplyPendingTracker.addPending(
                userWalletId = userWallet.walletId,
                cryptoCurrency = cryptoCurrency,
                txIds = listOf(txId),
            )
            params.callback.onStopEarningTransactionSent()
        }
    }

    private suspend fun onLoadFee(load: Int) = coroutineScope {
        // The fee check of the notifications reads this status, and the balance may have moved since the last load
        val feeStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(
            userWalletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        ).getOrNull()
        val nativeFeeStatus = feeStatus ?: cryptoCurrencyStatus
        if (cryptoCurrency !is CryptoCurrency.Token || uiState.value.isTransactionSending) {
            refreshNativeFeeStatus(load, nativeFeeStatus)
            return@coroutineScope
        }

        val exitTransitionData = yieldSupplyStopEarningUseCase(
            userWalletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            fee = null,
        ).getOrNull()
        if (exitTransitionData == null) {
            refreshNativeFeeStatus(load, nativeFeeStatus)
            return@coroutineScope
        }
        if (!feeLoads.takeOver(load, coroutineContext.job)) return@coroutineScope

        uiState.update {
            it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Loading)
        }

        val preparedClaim = async { prepareWithdrawalClaim(isFeeCurrencyKnown = feeStatus != null) }

        getFeeUseCase(
            transactionData = exitTransitionData,
            userWallet = userWallet,
            network = cryptoCurrency.network,
        ).fold(
            ifLeft = { feeError ->
                preparedClaim.cancel()
                feeCryptoCurrencyStatusFlow.update { nativeFeeStatus }
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
            ifRight = { fee ->
                val exitTransaction = exitTransitionData.copy(
                    fee = fee.normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY),
                )
                val claim = preparedClaim.await() as? YieldPreClaim.Ready
                withdrawalClaim = claim
                // The claim goes first: it pays out of the module the withdrawal is about to deactivate
                val transactions = listOfNotNull(claim?.transactionData, exitTransaction)
                onExitFeeEstimated(transactions, nativeFeeStatus)
            },
        )
    }

    /**
     * A load that returns before taking over only refreshes the balance of the native fee currency. It must not
     * replace the status a later load has written, nor the Smart Gas fee token of the fee that is shown.
     */
    private fun refreshNativeFeeStatus(load: Int, nativeFeeStatus: CryptoCurrencyStatus) {
        if (feeLoads.isSuperseded(load)) return
        val shownFee = uiState.value.yieldSupplyFeeUM as? YieldSupplyFeeUM.Content
        if (shownFee?.gaslessFee != null || pickedFeeCurrencyStatus?.currency is CryptoCurrency.Token) return
        feeCryptoCurrencyStatusFlow.update { nativeFeeStatus }
    }

    private suspend fun onExitFeeEstimated(
        transactions: List<TransactionData.Uncompiled>,
        nativeFeeStatus: CryptoCurrencyStatus,
    ) {
        nativeFeeCurrencyStatus = nativeFeeStatus
        updateFeeCurrencySelectable()
        val smartGasFee = if (yieldSupplyFeatureToggles.isSmartGasEnabled) {
            resolveSmartGasFee(transactions).getOrElse { error ->
                showPickedFeeCurrencyError(transactions, error)
                return
            }
        } else {
            null
        }
        val feeCryptoValue = smartGasFee?.feeAmount ?: transactions.sumOf { it.fee?.amount?.value.orZero() }
        val feeStatus = smartGasFee?.feeTokenStatus ?: nativeFeeStatus
        feeCryptoCurrencyStatusFlow.update { feeStatus }
        feeCurrencyChoice = YieldSupplyFeeCurrencyChoice(
            calls = transactions,
            feeCurrencyId = feeStatus.currency.id,
            gaslessFee = smartGasFee?.fee,
        )

        uiState.update(
            YieldSupplyStopEarningFeeContentTransformer(
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                feeCryptoCurrencyStatus = feeStatus,
                appCurrency = appCurrency,
                transactions = transactions,
                feeValue = feeCryptoValue,
                gaslessFee = smartGasFee?.fee,
            ),
        )

        yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
            data = YieldSupplyNotificationData(
                feeValue = feeCryptoValue,
                feeError = null,
            ),
        )
    }

    private suspend fun updateFeeCurrencySelectable() {
        val isSelectable = isSmartGasNetwork && yieldSupplySmartGas.hasFeeCurrencyChoice(
            userWallet = userWallet,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        )
        uiState.update { it.copy(isFeeCurrencySelectable = isSelectable) }
    }

    private suspend fun resolveSmartGasFee(
        calls: List<TransactionData.Uncompiled>,
    ): Either<GetFeeError, YieldSupplySmartGasFee?> {
        val pickedStatus = pickedFeeCurrencyStatus ?: return resolveAutoSmartGasFee(calls).right()
        return resolvePickedSmartGasFee(pickedStatus, calls)
    }

    /** A picked currency is kept even when it can't pay: the fee shows the error instead of moving elsewhere. */
    private suspend fun resolvePickedSmartGasFee(
        pickedStatus: CryptoCurrencyStatus,
        calls: List<TransactionData.Uncompiled>,
    ): Either<GetFeeError, YieldSupplySmartGasFee?> = either {
        if (pickedStatus.currency is CryptoCurrency.Coin) return@either null

        val fee = estimateFeeForGaslessBatchUseCase(
            userWallet = userWallet,
            operationTokenStatus = cryptoCurrencyStatus,
            calls = calls,
            sameTokenRule = YieldModuleSameTokenRule.Exit,
            feeCurrencyStatus = pickedStatus,
        ).bind()?.takeIf { it.isGasless } ?: raise(GetFeeError.UnknownError)
        toSmartGasFee(fee, calls) ?: raise(GetFeeError.UnknownError)
    }

    /**
     * Returns null — and the native fee stays — whenever Smart Gas can't pay: the native coin covers the batch or the
     * fee token isn't in the portfolio. The exit returns everything to the wallet, so the calls go as they are.
     */
    private suspend fun resolveAutoSmartGasFee(calls: List<TransactionData.Uncompiled>): YieldSupplySmartGasFee? {
        val fee = estimateFeeForGaslessBatchUseCase(
            userWallet = userWallet,
            operationTokenStatus = cryptoCurrencyStatus,
            calls = calls,
            sameTokenRule = YieldModuleSameTokenRule.Exit,
        ).getOrElse { error ->
            TangemLogger.e("Yield Mode exit Smart Gas fee estimate failed: $error")
            return null
        }
        if (fee == null || !fee.isGasless) return null
        return toSmartGasFee(fee, calls)
    }

    private suspend fun toSmartGasFee(
        fee: TransactionFeeExtended,
        calls: List<TransactionData.Uncompiled>,
    ): YieldSupplySmartGasFee? {
        val operationTokenStatus = cryptoCurrencyStatus
        val feeAmount = fee.transactionFee.normal.amount.value ?: run {
            TangemLogger.e("Yield Mode exit Smart Gas fee has no amount")
            return null
        }

        val feeTokenStatus = if (fee.feeTokenId == cryptoCurrency.id) {
            operationTokenStatus
        } else {
            yieldSupplySmartGas.getFeeTokenStatus(
                userWalletId = userWallet.walletId,
                feeTokenId = fee.feeTokenId,
                network = cryptoCurrency.network,
            )
        }
        if (feeTokenStatus == null) {
            TangemLogger.e("Yield Mode exit Smart Gas fee token ${fee.feeTokenId} is not in the portfolio")
            return null
        }
        return YieldSupplySmartGasFee(
            fee = fee,
            feeTokenStatus = feeTokenStatus,
            feeAmount = feeAmount,
            transactions = calls,
        )
    }

    private suspend fun showPickedFeeCurrencyError(calls: List<TransactionData.Uncompiled>, error: GetFeeError) {
        val pickedStatus = pickedFeeCurrencyStatus ?: nativeFeeCurrencyStatus
        TangemLogger.e("Yield Mode exit fee in the picked ${pickedStatus.currency.symbol} failed: $error")
        feeCurrencyChoice = YieldSupplyFeeCurrencyChoice(
            calls = calls,
            feeCurrencyId = pickedStatus.currency.id,
            gaslessFee = null,
        )
        feeCryptoCurrencyStatusFlow.update { pickedStatus }
        uiState.update { it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Error) }
        yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
            data = YieldSupplyNotificationData(feeValue = null, feeError = error.toPickedFeeCurrencyError()),
        )
    }

    /** Without the fee currency the combined fee would be weighed against the withdrawn token, not what pays it. */
    private suspend fun prepareWithdrawalClaim(isFeeCurrencyKnown: Boolean): YieldPreClaim {
        if (!isFeeCurrencyKnown) return YieldPreClaim.Skip
        if (!YieldSupplyWithdrawalClaimBatching.isSupported(cryptoCurrency.network)) return YieldPreClaim.Skip

        return yieldSupplyWithdrawalClaimPreparer.prepare(
            userWallet = userWallet,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        )
    }

    private fun subscribeOnNotificationsErrors() {
        yieldSupplyNotificationsUpdateTrigger.hasErrorFlow
            .onEach { hasError ->
                uiState.update {
                    it.copy(isPrimaryButtonEnabled = !hasError)
                }
            }.launchIn(modelScope)
    }
}
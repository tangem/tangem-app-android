package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import arrow.core.getOrElse
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.yieldsupply.providers.ethereum.yield.MerklClaimCallData
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
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.yield.supply.INCREASE_GAS_LIMIT_FOR_SUPPLY
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.increaseGasLimitBy
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOrigin
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import com.tangem.domain.yield.supply.usecase.YieldSupplyClaimTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyDeactivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyPendingTracker
import com.tangem.domain.yield.supply.usecase.YieldSupplyStopEarningUseCase
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.YieldSupplyAlertFactory
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.common.entity.transformer.YieldSupplyTransactionInProgressTransformer
import com.tangem.features.yield.supply.impl.common.entity.transformer.YieldSupplyTransactionReadyTransformer
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class YieldSupplyStopEarningModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val analytics: AnalyticsEventHandler,
    private val getFeeUseCase: GetFeeUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
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

    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus
        get() = feeCryptoCurrencyStatusFlow.value

    private var appCurrency = AppCurrency.Default

    private var withdrawalClaim: YieldPreClaim.Ready? = null

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
        modelScope.launch {
            onLoadFee()
        }
    }

    fun onReadMoreClick() {
        modelScope.launch {
            urlOpener.openUrl(TangemBlogUrlBuilder.build(TangemBlogUrlBuilder.Post.WhatIsTransactionFee))
        }
    }

    fun onClick() {
        params.callback.onTransactionProgress(true)

        val yieldSupplyFeeUM = uiState.value.yieldSupplyFeeUM as? YieldSupplyFeeUM.Content ?: return
        analytics.send(
            YieldSupplyAnalytics.ButtonStopEarning(
                token = cryptoCurrency.symbol,
                blockchain = cryptoCurrency.network.name,
            ),
        )
        uiState.update(YieldSupplyTransactionInProgressTransformer)

        val claimIndex = yieldSupplyFeeUM.transactionDataList.indexOfFirst {
            (it.extras as? EthereumTransactionExtras)?.callData is MerklClaimCallData
        }
        // Read on the same frame as the fee content: both are written together when the fee loads
        val claim = withdrawalClaim?.takeIf { claimIndex != -1 }

        modelScope.launch(dispatchers.default) {
            sendTransactionUseCase(
                txsData = yieldSupplyFeeUM.transactionDataList,
                userWallet = userWallet,
                network = cryptoCurrency.network,
                sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
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
                    val claimHash = hashes.getOrNull(claimIndex)
                    if (claim != null && claimHash != null) trackWithdrawalClaim(claim, transactionHash = claimHash)
                    onStopEarningTransactionSuccess(hashes.last())
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
        modelScope.launch { onLoadFee() }
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

    private suspend fun onStopEarningTransactionSuccess(txId: String) {
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
        val feeAssetType = if (feeCryptoCurrencyStatus.currency is CryptoCurrency.Coin) {
            AnalyticsParam.FeeAssetType.Coin
        } else {
            AnalyticsParam.FeeAssetType.Token
        }
        val event = AnalyticsParam.TxSentFrom.Earning(
            blockchain = cryptoCurrency.network.name,
            token = cryptoCurrency.symbol,
            feeType = AnalyticsParam.FeeType.Normal,
            feeToken = feeCryptoCurrencyStatus.currency.symbol,
            feeAssetType = feeAssetType,
        )
        analytics.send(
            Basic.TransactionSent(
                sentFrom = event,
                memoType = Basic.TransactionSent.MemoType.Null,
                isCustomToken = cryptoCurrency.isCustom,
                isCustomFeeToken = feeCryptoCurrencyStatus.currency.isCustom,
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

    private suspend fun onLoadFee() = coroutineScope {
        // The fee check of the notifications reads this status, and the balance may have moved since the last load
        feeCryptoCurrencyStatusFlow.update {
            getFeePaidCryptoCurrencyStatusSyncUseCase(
                userWalletId = userWallet.walletId,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
            ).getOrNull() ?: cryptoCurrencyStatus
        }
        if (cryptoCurrency !is CryptoCurrency.Token || uiState.value.isTransactionSending) return@coroutineScope

        val exitTransitionData = yieldSupplyStopEarningUseCase(
            userWalletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            fee = null,
        ).getOrNull() ?: return@coroutineScope

        uiState.update {
            it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Loading)
        }

        val preparedClaim = async { prepareWithdrawalClaim() }

        getFeeUseCase(
            transactionData = exitTransitionData,
            userWallet = userWallet,
            network = cryptoCurrency.network,
        ).fold(
            ifLeft = { feeError ->
                preparedClaim.cancel()
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
                val feeCryptoValue = transactions.sumOf { it.fee?.amount?.value.orZero() }

                uiState.update(
                    YieldSupplyStopEarningFeeContentTransformer(
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                        appCurrency = appCurrency,
                        transactions = transactions,
                        feeValue = feeCryptoValue,
                    ),
                )

                yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
                    data = YieldSupplyNotificationData(
                        feeValue = feeCryptoValue,
                        feeError = null,
                    ),
                )
            },
        )
    }

    private suspend fun prepareWithdrawalClaim(): YieldPreClaim {
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
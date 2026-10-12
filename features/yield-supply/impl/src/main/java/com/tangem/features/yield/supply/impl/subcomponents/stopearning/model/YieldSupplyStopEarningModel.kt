package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import arrow.core.getOrElse
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
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
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.yield.supply.INCREASE_GAS_LIMIT_FOR_SUPPLY
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.increaseGasLimitBy
import com.tangem.domain.yield.supply.models.YieldSupplyClaimOrigin
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import com.tangem.domain.yield.supply.usecase.PrepareClaimBeforeWithdrawUseCase
import com.tangem.domain.yield.supply.usecase.YieldPreClaim
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.math.BigDecimal
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
    private val prepareClaimBeforeWithdrawUseCase: PrepareClaimBeforeWithdrawUseCase,
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
            subscribeOnCurrencyStatusUpdates()
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

        modelScope.launch(dispatchers.default) {
            val withdrawalTransaction = yieldSupplyFeeUM.transactionDataList.first()
            claimRewardsBeforeWithdraw(withdrawalFee = withdrawalTransaction.fee?.amount?.value.orZero())

            sendTransactionUseCase(
                txData = withdrawalTransaction,
                userWallet = userWallet,
                network = cryptoCurrency.network,
            ).fold(
                ifLeft = { error ->
                    TangemLogger.e(error.toString())
                    uiState.update(YieldSupplyTransactionReadyTransformer)
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
                ifRight = { txData ->
                    onStopEarningTransactionSuccess(txData)
                },
            )
        }
    }

    /**
     * Claims the Merkl rewards the module still holds, as its own owner-signed transaction ahead of the
     * withdrawal, because the withdrawal deactivates the module they belong to.
     *
     * This is the fallback for a withdrawal the claim sheet was never offered for; once the owner has been asked,
     * their answer stands and nothing is claimed behind it. It is an attempt and nothing more: a claim that cannot
     * be prepared, is declined at the card, or is rejected by the node leaves a log line and the withdrawal
     * untouched.
     */
    private suspend fun claimRewardsBeforeWithdraw(withdrawalFee: BigDecimal) {
        if (params.wasClaimOffered) {
            TangemLogger.i("Claim before withdrawal skipped: the owner was already offered the claim sheet")
            return
        }

        // Bounded because everything up to the signature is optional work the withdrawal waits on: a stalled
        // incentives backend must cost the user a moment, not their exit. The signature itself is not bounded —
        // that is the user reaching for their card.
        val preparedClaim = withTimeoutOrNull(PREPARE_CLAIM_TIMEOUT_MILLIS) {
            prepareClaimBeforeWithdrawUseCase(
                userWallet = userWallet,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                withdrawalFee = withdrawalFee,
            )
        }
        if (preparedClaim == null) {
            TangemLogger.i("Claim before withdrawal skipped: it was not prepared in time")
            return
        }
        val claim = preparedClaim as? YieldPreClaim.Ready ?: return

        sendTransactionUseCase(
            txData = claim.transactionData,
            userWallet = userWallet,
            network = cryptoCurrency.network,
        ).fold(
            ifLeft = { error -> TangemLogger.e("Claim before withdrawal was not submitted: $error") },
            ifRight = { transactionHash ->
                yieldSupplyClaimTracker.track(
                    userWalletId = userWallet.walletId,
                    cryptoCurrency = claim.token,
                    chainId = claim.chainId,
                    userAddress = claim.userAddress,
                    transactionHash = transactionHash,
                    origin = YieldSupplyClaimOrigin.Withdrawal,
                )
            },
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

    private fun subscribeOnCurrencyStatusUpdates() {
        modelScope.launch {
            feeCryptoCurrencyStatusFlow.update {
                getFeePaidCryptoCurrencyStatusSyncUseCase(
                    userWalletId = userWallet.walletId,
                    cryptoCurrencyStatus = cryptoCurrencyStatus,
                ).getOrNull() ?: cryptoCurrencyStatus
            }
            onLoadFee()
        }
    }

    private suspend fun onLoadFee() {
        if (cryptoCurrency !is CryptoCurrency.Token || uiState.value.isTransactionSending) return

        val exitTransitionData = yieldSupplyStopEarningUseCase(
            userWalletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            fee = null,
        ).getOrNull() ?: return

        uiState.update {
            it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Loading)
        }

        getFeeUseCase(
            transactionData = exitTransitionData,
            userWallet = userWallet,
            network = cryptoCurrency.network,
        ).fold(
            ifLeft = { feeError ->
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
                val adjustedFee = fee.normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY)
                val feeCryptoValue = adjustedFee.amount.value.orZero()

                uiState.update(
                    YieldSupplyStopEarningFeeContentTransformer(
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                        appCurrency = appCurrency,
                        transactions = listOf(exitTransitionData.copy(fee = adjustedFee)),
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

    private fun subscribeOnNotificationsErrors() {
        yieldSupplyNotificationsUpdateTrigger.hasErrorFlow
            .onEach { hasError ->
                uiState.update {
                    it.copy(isPrimaryButtonEnabled = !hasError)
                }
            }.launchIn(modelScope)
    }

    private companion object {
        const val PREPARE_CLAIM_TIMEOUT_MILLIS = 15_000L
    }
}
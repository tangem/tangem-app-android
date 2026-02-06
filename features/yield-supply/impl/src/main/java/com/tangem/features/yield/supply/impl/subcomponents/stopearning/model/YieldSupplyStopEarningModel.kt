package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.datasource.local.appsflyer.AppsFlyerStore
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.isHotWallet
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.yield.supply.INCREASE_GAS_LIMIT_FOR_SUPPLY
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.increaseGasLimitBy
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
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
import com.tangem.utils.TangemBlogUrlBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private val appsFlyerStore: AppsFlyerStore,
) : Model(), YieldSupplyNotificationsComponent.ModelCallback {

    private val params: YieldSupplyStopEarningComponent.Params = paramsContainer.require()

    private val cryptoCurrencyStatus
        get() = params.cryptoCurrencyStatusFlow.value
    private val cryptoCurrency = cryptoCurrencyStatus.currency
    private var userWallet = params.userWallet

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
        urlOpener.openUrl(TangemBlogUrlBuilder.FEE_BLOG_LINK)
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
            sendTransactionUseCase(
                txData = yieldSupplyFeeUM.transactionDataList.first(),
                userWallet = userWallet,
                network = cryptoCurrency.network,
            ).fold(
                ifLeft = { error ->
                    Timber.e(error.toString())
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
                referralId = appsFlyerStore.get()?.refcode,
            ),
        )
        val event = AnalyticsParam.TxSentFrom.Earning(
            blockchain = cryptoCurrency.network.name,
            token = cryptoCurrency.symbol,
            feeType = AnalyticsParam.FeeType.Normal,
            feeToken = feeCryptoCurrencyStatus.currency.symbol,
        )
        analytics.send(
            Basic.TransactionSent(
                sentFrom = event,
                memoType = Basic.TransactionSent.MemoType.Null,
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
                val fee = fee.normal.increaseGasLimitBy(INCREASE_GAS_LIMIT_FOR_SUPPLY)
                val feeCryptoValue = fee.amount.value.orZero()

                uiState.update(
                    YieldSupplyStopEarningFeeContentTransformer(
                        cryptoCurrencyStatus = cryptoCurrencyStatus,
                        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
                        appCurrency = appCurrency,
                        transactions = listOf(exitTransitionData.copy(fee = fee)),
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
}
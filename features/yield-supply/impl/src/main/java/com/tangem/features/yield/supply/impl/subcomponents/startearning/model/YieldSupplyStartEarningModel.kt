package com.tangem.features.yield.supply.impl.subcomponents.startearning.model

import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionSender
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldSupplyEnterStatus
import com.tangem.domain.yield.supply.usecase.*
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
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningComponent
import com.tangem.features.yield.supply.impl.subcomponents.startearning.model.transformers.YieldSupplyStartEarningFeeContentTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.update
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val yieldSupplyStartEarningUseCase: YieldSupplyStartEarningUseCase,
    private val yieldSupplyEstimateEnterFeeUseCase: YieldSupplyEstimateEnterFeeUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val yieldSupplyNotificationsUpdateTrigger: YieldSupplyNotificationsUpdateTrigger,
    private val yieldSupplyAlertFactory: YieldSupplyAlertFactory,
    private val yieldSupplyActivateUseCase: YieldSupplyActivateUseCase,
    private val yieldSupplyMinAmountUseCase: YieldSupplyMinAmountUseCase,
    private val yieldSupplyGetMaxFeeUseCase: YieldSupplyGetMaxFeeUseCase,
    private val yieldSupplyGetCurrentFeeUseCase: YieldSupplyGetCurrentFeeUseCase,
    private val yieldSupplyRepository: YieldSupplyRepository,
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
            ),
        )

    private val cryptoCurrencyStatus
        get() = cryptoCurrencyStatusFlow.value
    private var appCurrency = AppCurrency.Default

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

    private suspend fun calculateMinAmount(cryptoCurrencyStatus: CryptoCurrencyStatus) {
        yieldSupplyMinAmountUseCase(userWalletId, cryptoCurrencyStatus).onRight {
            minAmount = it
        }.onLeft {
            minAmount = BigDecimal.ZERO
        }
    }

    private suspend fun onLoadFee() {
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Loading || uiState.value.isTransactionSending) return

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
            cryptoCurrency = feeCryptoCurrencyStatusFlow.value.currency,
            transactionDataList = transactionListData,
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
            ifRight = { updatedTransactionList ->
                val feeSum = updatedTransactionList.sumOf {
                    it.fee?.amount?.value.orZero()
                }

                uiState.update(
                    YieldSupplyStartEarningFeeContentTransformer(
                        cryptoCurrencyStatus = cryptoCurrencyStatusFlow.value,
                        feeCryptoCurrencyStatus = feeCryptoCurrencyStatusFlow.value,
                        appCurrency = appCurrency,
                        updatedTransactionList = updatedTransactionList,
                        feeValue = feeSum,
                        maxNetworkFee = maxFee,
                        estimatedFeeValueInTokenCurrency = estimatedFee.value,
                        minAmount = minAmount,
                    ),
                )
                yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
                    data = YieldSupplyNotificationData(
                        feeValue = feeSum,
                        feeError = null,
                        shouldShowHighFeeNotification = estimatedFee.isHighFee,
                    ),
                )
            },
        )
    }

    fun onClick() {
        val yieldSupplyFeeUM = uiState.value.yieldSupplyFeeUM as? YieldSupplyFeeUM.Content ?: return
        analytics.send(
            YieldSupplyAnalytics.ButtonStartEarning(
                token = params.cryptoCurrency.symbol,
                blockchain = params.cryptoCurrency.network.name,
            ),
        )

        uiState.update(YieldSupplyTransactionInProgressTransformer)
        modelScope.launch(dispatchers.default) {
            sendTransactionUseCase.invoke(
                txsData = yieldSupplyFeeUM.transactionDataList,
                userWallet = userWallet,
                network = cryptoCurrency.network,
                sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
            ).fold(
                ifLeft = { error ->
                    Timber.e(error.toString())
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
                ifRight = {
                    onStartEarningTransactionSuccess(yieldSupplyFeeUM)
                },
            )
        }
    }

    private suspend fun onStartEarningTransactionSuccess(yieldSupplyFeeUM: YieldSupplyFeeUM.Content) {
        yieldSupplyRepository.saveTokenProtocolStatus(
            userWalletId,
            cryptoCurrency,
            YieldSupplyEnterStatus.Enter,
        )
        val event = AnalyticsParam.TxSentFrom.Earning(
            blockchain = cryptoCurrency.network.name,
            token = cryptoCurrency.symbol,
            feeType = AnalyticsParam.FeeType.Normal,
        )
        analytics.send(
            YieldSupplyAnalytics.FundsEarned(
                blockchain = cryptoCurrency.network.name,
                token = cryptoCurrency.symbol,
            ),
        )
        yieldSupplyFeeUM.transactionDataList.forEach {
            analytics.send(
                Basic.TransactionSent(
                    sentFrom = event,
                    memoType = Basic.TransactionSent.MemoType.Null,
                ),
            )
        }

        val address = cryptoCurrencyStatus.value.networkAddress?.defaultAddress?.value
        if (address != null) {
            yieldSupplyActivateUseCase(
                userWalletId = userWalletId,
                cryptoCurrency = cryptoCurrency,
                address = address,
            )
        }

        modelScope.launch {
            params.callback.onTransactionSent()
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        modelScope.launch {
            getUserWalletUseCase(userWalletId).fold(
                ifRight = { wallet ->
                    userWallet = wallet
                    getCurrenciesStatusUpdates()
                },
                ifLeft = {
                    Timber.w(it.toString())
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
        modelScope.launch {
            onLoadFee()
        }
    }

    private fun getCurrenciesStatusUpdates() {
        getSingleCryptoCurrencyStatusUseCase.invokeMultiWallet(
            userWalletId = userWalletId,
            currencyId = cryptoCurrency.id,
            isSingleWalletWithTokens = false,
        ).onEach { maybeCryptoCurrency ->
            maybeCryptoCurrency.fold(
                ifRight = { cryptoCurrencyStatus ->
                    onDataLoaded(
                        currencyStatus = cryptoCurrencyStatus,
                        feeCurrencyStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(
                            userWalletId = userWalletId,
                            cryptoCurrencyStatus = cryptoCurrencyStatus,
                        ).getOrNull() ?: cryptoCurrencyStatus,
                    )
                },
                ifLeft = {
                    Timber.w(it.toString())
                    showAlertError()
                },
            )
        }.launchIn(modelScope)
    }

    private fun onDataLoaded(currencyStatus: CryptoCurrencyStatus, feeCurrencyStatus: CryptoCurrencyStatus) {
        cryptoCurrencyStatusFlow.update { currencyStatus }
        feeCryptoCurrencyStatusFlow.update { feeCurrencyStatus }

        modelScope.launch {
            calculateMinAmount(currencyStatus)
            onLoadFee()
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
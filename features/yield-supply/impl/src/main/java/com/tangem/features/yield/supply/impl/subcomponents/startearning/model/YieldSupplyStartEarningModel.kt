package com.tangem.features.yield.supply.impl.subcomponents.startearning.model

import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.FetchCurrencyStatusUseCase
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyActivateUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyEstimateEnterFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyGetTokenStatusUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyStartEarningUseCase
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

@Suppress("LongParameterList")
@ModelScoped
internal class YieldSupplyStartEarningModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val yieldSupplyStartEarningUseCase: YieldSupplyStartEarningUseCase,
    private val yieldSupplyEstimateEnterFeeUseCase: YieldSupplyEstimateEnterFeeUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val yieldSupplyNotificationsUpdateTrigger: YieldSupplyNotificationsUpdateTrigger,
    private val fetchCurrencyStatusUseCase: FetchCurrencyStatusUseCase,
    private val yieldSupplyAlertFactory: YieldSupplyAlertFactory,
    private val yieldSupplyActivateUseCase: YieldSupplyActivateUseCase,
    private val yieldSupplyGetTokenStatusUseCase: YieldSupplyGetTokenStatusUseCase,
) : Model(), YieldSupplyNotificationsComponent.ModelCallback {

    private val params: YieldSupplyStartEarningComponent.Params = paramsContainer.require()

    private val cryptoCurrency = params.cryptoCurrency
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
                footer = resourceReference(R.string.yield_module_start_earning_sheet_next_deposits),
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
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
            subscribeOnCurrencyStatusUpdates()
            subscribeOnNotificationsErrors()
        }
    }

    private suspend fun getMaxFee(): BigDecimal? {
        if (uiState.value.maxFee != BigDecimal.ZERO) return uiState.value.maxFee
        val yieldTokenStatus = yieldSupplyGetTokenStatusUseCase(cryptoCurrency as CryptoCurrency.Token)
            .getOrNull()
        return yieldTokenStatus?.maxFeeNative?.parseToBigDecimal(cryptoCurrency.decimals)
    }

    private suspend fun onLoadFee() {
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Loading || uiState.value.isTransactionSending) return

        val maxFee = if (uiState.value.maxFee == BigDecimal.ZERO) {
            getMaxFee()
        } else {
            uiState.value.maxFee
        } ?: return

        val transactionListData = yieldSupplyStartEarningUseCase(
            userWalletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            maxNetworkFee = maxFee,
        ).getOrNull() ?: return

        uiState.update {
            it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Loading)
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
                    ),
                )
                yieldSupplyNotificationsUpdateTrigger.triggerUpdate(
                    data = YieldSupplyNotificationData(
                        feeValue = feeSum,
                        feeError = null,
                    ),
                )
            },
        )
    }

    fun onClick() {
        val yieldSupplyFeeUM = uiState.value.yieldSupplyFeeUM as? YieldSupplyFeeUM.Content ?: return

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
                    fetchCurrencyStatusUseCase(userWalletId = userWallet.walletId, cryptoCurrency.id)
                    yieldSupplyActivateUseCase(cryptoCurrency)
                    modelScope.launch {
                        params.callback.onTransactionSent()
                    }
                },
            )
        }
    }

    private fun subscribeOnCurrencyStatusUpdates() {
        modelScope.launch {
            getUserWalletUseCase(params.userWalletId).fold(
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
            userWalletId = params.userWalletId,
            currencyId = cryptoCurrency.id,
            isSingleWalletWithTokens = false,
        ).onEach { maybeCryptoCurrency ->
            maybeCryptoCurrency.fold(
                ifRight = { cryptoCurrencyStatus ->
                    onDataLoaded(
                        currencyStatus = cryptoCurrencyStatus,
                        feeCurrencyStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(
                            userWalletId = params.userWalletId,
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
}
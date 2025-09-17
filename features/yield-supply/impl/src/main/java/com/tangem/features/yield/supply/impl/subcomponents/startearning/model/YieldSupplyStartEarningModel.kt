package com.tangem.features.yield.supply.impl.subcomponents.startearning.model

import arrow.core.getOrElse
import com.tangem.blockchain.common.TransactionSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyEstimateEnterFeeUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyStartEarningUseCase
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.subcomponents.startearning.YieldSupplyStartEarningComponent
import com.tangem.utils.StringsSigns.DOT
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toPersistentList
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
) : Model() {

    private val params: YieldSupplyStartEarningComponent.Params = paramsContainer.require()

    private val cryptoCurrency = params.cryptoCurrency
    private var userWallet: UserWallet by Delegates.notNull()

    private val cryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
    field = MutableStateFlow(
        CryptoCurrencyStatus(
            currency = cryptoCurrency,
            value = CryptoCurrencyStatus.Loading,
        ),
    )
    private val feeCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
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
            footer = combinedReference(
                resourceReference(R.string.yield_module_start_earning_sheet_next_deposits),
                resourceReference(R.string.yield_module_start_earning_sheet_fee_policy),
            ),
            currencyIconState = CryptoCurrencyToIconStateConverter().convert(params.cryptoCurrency),
            yieldSupplyFeeUM = YieldSupplyFeeUM.Loading,
            isPrimaryButtonEnabled = false,
        ),
    )

    private val cryptoCurrencyStatus
        get() = cryptoCurrencyStatusFlow.value
    private var appCurrency = AppCurrency.Default

    init {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
            subscribeOnCurrencyStatusUpdates()
        }
    }

    private suspend fun onLoadFee() {
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Loading) return

        val transactionListData = yieldSupplyStartEarningUseCase(
            userWalletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
        ).getOrNull() ?: return

        val updatedTransactionList = yieldSupplyEstimateEnterFeeUseCase.invoke(
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
            transactionDataList = transactionListData,
        ).getOrNull() ?: return

        val feeSum = updatedTransactionList.sumOf {
            it.fee?.amount?.value ?: BigDecimal.ZERO
        }

        val crypto = feeSum.format { crypto(feeCryptoCurrencyStatusFlow.value.currency) }
        val fiatFeeValue = cryptoCurrencyStatus.value.fiatRate?.let { rate ->
            feeSum.multiply(rate)
        }

        val fiat = fiatFeeValue.format { fiat(appCurrency.code, appCurrency.symbol) }

        uiState.update {
            if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Loading) {
                it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Loading)
            } else {
                it.copy(
                    isPrimaryButtonEnabled = true, // todo yield supply check for notifications
                    yieldSupplyFeeUM = YieldSupplyFeeUM.Content(
                        transactionDataList = updatedTransactionList.toPersistentList(),
                        feeValue = combinedReference(
                            stringReference(crypto),
                            stringReference(" $DOT "),
                            stringReference(fiat),
                        ),
                    ),
                )
            }
        }
    }

    fun onClick() {
        val yieldSupplyFeeUM = uiState.value.yieldSupplyFeeUM as? YieldSupplyFeeUM.Content ?: return

        uiState.update { it.copy(isPrimaryButtonEnabled = false) }
        modelScope.launch(dispatchers.default) {
            sendTransactionUseCase.invoke(
                txsData = yieldSupplyFeeUM.transactionDataList,
                userWallet = userWallet,
                network = cryptoCurrency.network,
                sendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
            ).fold(
                ifLeft = {
                    Timber.e(it.toString())
                    uiState.update { it.copy(isPrimaryButtonEnabled = true) }
                },
                ifRight = {
                    params.callback.onTransactionSent()
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
                    // showAlertError() todo yield supply error alert
                    return@launch
                },
            )
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
                    // todo yield supply error
                    // sendConfirmAlertFactory.getGenericErrorState(
                    //     onFailedTxEmailClick = {
                    //         onFailedTxEmailClick(it.toString())
                    //     },
                    //     popBack = router::pop,
                    // )
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
}
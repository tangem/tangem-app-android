package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import arrow.core.getOrElse
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
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyStopEarningUseCase
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.features.yield.supply.impl.subcomponents.stopearning.YieldSupplyStopEarningComponent
import com.tangem.utils.StringsSigns.DOT
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class YieldSupplyStopEarningModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val getFeeUseCase: GetFeeUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
    private val yieldSupplyStopEarningUseCase: YieldSupplyStopEarningUseCase,
) : Model() {

    private val params: YieldSupplyStopEarningComponent.Params = paramsContainer.require()

    private val cryptoCurrencyStatus
        get() = params.cryptoCurrencyStatusFlow.value
    private val cryptoCurrency = cryptoCurrencyStatus.currency
    private var userWallet = params.userWallet

    private val feeCryptoCurrencyStatusFlow: StateFlow<CryptoCurrencyStatus>
        field = MutableStateFlow(
            CryptoCurrencyStatus(
                cryptoCurrencyStatus.currency,
                value = CryptoCurrencyStatus.Loading,
            ),
        )

    private var appCurrency = AppCurrency.Default

    val uiState: StateFlow<YieldSupplyActionUM>
        field: MutableStateFlow<YieldSupplyActionUM> = MutableStateFlow(
            YieldSupplyActionUM(
                title = resourceReference(R.string.yield_module_stop_earning),
                subtitle = resourceReference(
                    id = R.string.yield_module_stop_earning_sheet_description,
                    formatArgs = wrappedList(cryptoCurrency.symbol),
                ),
                footer = combinedReference(
                    resourceReference(R.string.yield_module_stop_earning_sheet_fee_note),
                    resourceReference(R.string.common_read_more),
                ),
                currencyIconState = CryptoCurrencyToIconStateConverter().convert(cryptoCurrency),
                yieldSupplyFeeUM = YieldSupplyFeeUM.Loading,
                isPrimaryButtonEnabled = false,
            ),
        )

    init {
        modelScope.launch {
            appCurrency = getSelectedAppCurrencyUseCase.invokeSync().getOrElse { AppCurrency.Default }
            subscribeOnCurrencyStatusUpdates()
        }
    }

    fun onClick() {
        val yieldSupplyFeeUM = uiState.value.yieldSupplyFeeUM as? YieldSupplyFeeUM.Content ?: return
        uiState.update { it.copy(isPrimaryButtonEnabled = false) }

        modelScope.launch(dispatchers.default) {
            sendTransactionUseCase(
                txData = yieldSupplyFeeUM.transactionDataList.first(),
                userWallet = userWallet,
                network = cryptoCurrency.network,
            ).fold(
                ifLeft = {
                    Timber.e(it.toString())
                },
                ifRight = {
                    params.callback.onTransactionSent()
                },
            )
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
        val exitTransitionData = yieldSupplyStopEarningUseCase(
            userWalletId = userWallet.walletId,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            fee = null,
        ).getOrNull() ?: return

        val fee = getFeeUseCase(
            transactionData = exitTransitionData,
            userWallet = userWallet,
            network = cryptoCurrency.network,
        ).getOrNull() ?: return

        val crypto = fee.normal.amount.value.format { crypto(feeCryptoCurrencyStatusFlow.value.currency) }
        val fiatFeeValue = cryptoCurrencyStatus.value.fiatRate?.let { rate ->
            fee.normal.amount.value?.multiply(rate)
        }

        val fiat = fiatFeeValue.format { fiat(appCurrency.code, appCurrency.symbol) }

        uiState.update {
            if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Loading) {
                it.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Loading)
            } else {
                it.copy(
                    isPrimaryButtonEnabled = true,
                    yieldSupplyFeeUM = YieldSupplyFeeUM.Content(
                        transactionDataList = persistentListOf(exitTransitionData.copy(fee = fee.normal)),
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
}
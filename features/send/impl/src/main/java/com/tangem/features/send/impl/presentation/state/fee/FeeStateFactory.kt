package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.extensions.isZero
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

/**
 * Factory to produce fee state for [SendUiState]
 */
internal class FeeStateFactory(
    private val clickIntents: SendClickIntents,
    private val currentStateProvider: Provider<SendUiState>,
    private val coinCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
) {
    private val customFeeFieldConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendFeeCustomFieldConverter(
            clickIntents = clickIntents,
            appCurrencyProvider = appCurrencyProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }

    val feeConverter by lazy(LazyThreadSafetyMode.NONE) {
        FeeConverter(
            clickIntents = clickIntents,
            appCurrencyProvider = appCurrencyProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }

    fun onFeeOnLoadingState(): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        return state.copy(
            feeState = feeState.copy(
                feeSelectorState = FeeSelectorState.Loading,
                notifications = persistentListOf(),
                isPrimaryButtonEnabled = false,
            ),
        )
    }

    fun onFeeOnLoadedState(fees: TransactionFee, isSubtractAvailable: Boolean): SendUiState {
        val state = currentStateProvider()
        val balance = coinCryptoCurrencyStatusProvider().value.amount ?: BigDecimal.ZERO
        val feeState = state.feeState ?: return state
        val feeSelectorState = (feeState.feeSelectorState as? FeeSelectorState.Content)?.copy(
            fees = fees,
            customValues = customFeeFieldConverter.convert(fees.normal),
        ) ?: FeeSelectorState.Content(
            fees = fees,
            customValues = customFeeFieldConverter.convert(fees.normal),
        )

        val fee = feeConverter.convert(feeSelectorState)
        val receivedAmount = calculateReceiveAmount(state, fee)
        return state.copy(
            feeState = feeState.copy(
                isSubtractAvailable = isSubtractAvailable,
                feeSelectorState = feeSelectorState,
                fee = fee,
                receivedAmountValue = receivedAmount,
                receivedAmount = getFormattedValue(receivedAmount),
                isSubtract = isSubtractAvailable && checkAutoSubtract(state, fee, balance),
                isFeeApproximate = isFeeApproximate(fee),
            ),
        )
    }

    fun onFeeOnLoadedState(fees: TransactionFee): SendUiState {
        val state = currentStateProvider()
        val balance = coinCryptoCurrencyStatusProvider().value.amount ?: BigDecimal.ZERO
        val feeState = state.feeState ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state

        val updatedFeeSelector = feeSelectorState.copy(
            fees = fees,
            customValues = customFeeFieldConverter.convert(fees.normal),
        )
        val fee = feeConverter.convert(updatedFeeSelector)
        val receivedAmount = calculateReceiveAmount(state, fee)
        return state.copy(
            feeState = feeState.copy(
                feeSelectorState = updatedFeeSelector,
                fee = fee,
                receivedAmountValue = receivedAmount,
                receivedAmount = getFormattedValue(receivedAmount),
                isSubtract = checkAutoSubtract(state, fee, balance),
            ),
        )
    }

    fun onFeeOnErrorState(): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            feeState = state.feeState?.copy(
                feeSelectorState = FeeSelectorState.Error,
            ),
        )
    }

    fun onFeeSelectedState(feeType: FeeType): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state
        val balance = coinCryptoCurrencyStatusProvider().value.amount ?: BigDecimal.ZERO

        val updatedFeeSelectorState = feeSelectorState.copy(selectedFee = feeType)
        val fee = feeConverter.convert(updatedFeeSelectorState)
        val receivedAmount = calculateReceiveAmount(state, fee)
        return state.copy(
            feeState = feeState.copy(
                fee = fee,
                feeSelectorState = updatedFeeSelectorState,
                receivedAmountValue = receivedAmount,
                receivedAmount = getFormattedValue(receivedAmount),
                isSubtract = checkAutoSubtract(state, fee, balance),
            ),
        )
    }

    fun onCustomFeeValueChange(index: Int, value: String): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state
        val updatedFeeSelectorState = customFeeFieldConverter.onValueChange(feeSelectorState, index, value)
        val balance = coinCryptoCurrencyStatusProvider().value.amount ?: BigDecimal.ZERO

        val fee = feeConverter.convert(updatedFeeSelectorState)
        val receivedAmount = calculateReceiveAmount(state, fee)
        return state.copy(
            feeState = feeState.copy(
                feeSelectorState = updatedFeeSelectorState,
                fee = fee,
                receivedAmountValue = receivedAmount,
                receivedAmount = getFormattedValue(receivedAmount),
                isSubtract = checkAutoSubtract(state, fee, balance),
            ),
        )
    }

    fun onSubtractSelect(value: Boolean): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state
        val fee = feeConverter.convert(feeSelectorState)
        val receivedAmount = calculateReceiveAmount(state, fee)
        return state.copy(
            feeState = feeState.copy(
                isSubtract = value,
                isUserSubtracted = true,
                receivedAmountValue = receivedAmount,
                receivedAmount = getFormattedValue(receivedAmount),
                fee = fee,
            ),
        )
    }

    fun getFeeNotificationState(notifications: ImmutableList<SendFeeNotification>): SendUiState {
        val state = currentStateProvider()
        return state.copy(
            feeState = state.feeState?.copy(
                notifications = notifications,
                isPrimaryButtonEnabled = isPrimaryButtonEnabled(state.feeState, notifications),
            ),
        )
    }

    private fun isPrimaryButtonEnabled(
        feeState: SendStates.FeeState,
        notifications: ImmutableList<SendFeeNotification>,
    ): Boolean {
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return false
        val customValue = feeSelectorState.customValues.firstOrNull()
        val balance = coinCryptoCurrencyStatusProvider().value.amount ?: BigDecimal.ZERO
        val fee = feeConverter.convert(feeSelectorState)
        val feeValue = fee.amount.value ?: BigDecimal.ZERO

        val isNotCustom = feeSelectorState.selectedFee != FeeType.CUSTOM
        val isNotEmptyCustom = if (customValue != null) {
            !customValue.value.parseToBigDecimal(customValue.decimals).isZero() && !isNotCustom
        } else {
            false
        }
        val noErrors = notifications.none { it is SendFeeNotification.Error }
        val isSubtractRequired = when {
            !feeState.isSubtractAvailable -> true // current currency is not fee currency
            feeValue + feeState.receivedAmountValue >= balance -> feeState.isSubtract
            else -> feeValue + feeState.receivedAmountValue <= balance
        }

        return noErrors && isSubtractRequired && (isNotEmptyCustom || isNotCustom)
    }

    private fun checkAutoSubtract(state: SendUiState, fee: Fee, balance: BigDecimal): Boolean {
        val feeState = state.feeState ?: return false
        val amountValue = state.amountState?.amountTextField?.cryptoAmount?.value ?: BigDecimal.ZERO
        val feeAmount = fee.amount.value ?: BigDecimal.ZERO
        return if (feeState.isUserSubtracted) {
            feeState.isSubtract
        } else {
            amountValue + feeAmount >= balance
        }
    }

    private fun isFeeApproximate(fee: Fee): Boolean {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        return isFeeApproximateUseCase(
            networkId = cryptoCurrencyStatus.currency.network.id,
            amountType = fee.amount.type,
        )
    }

    private fun getFormattedValue(value: BigDecimal): String {
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        return BigDecimalFormatter.formatCryptoAmount(
            cryptoAmount = value,
            cryptoCurrency = cryptoCurrency.symbol,
            decimals = cryptoCurrency.decimals,
        )
    }
}
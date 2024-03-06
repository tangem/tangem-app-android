package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.extensions.isZero
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

/**
 * Factory to produce fee state for [SendUiState]
 */
internal class FeeStateFactory(
    private val clickIntents: SendClickIntents,
    private val currentStateProvider: Provider<SendUiState>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
) {
    private val customFeeFieldConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendFeeCustomFieldConverter(
            clickIntents = clickIntents,
            appCurrencyProvider = appCurrencyProvider,
            feeCryptoCurrencyStatusProvider = feeCryptoCurrencyStatusProvider,
        )
    }

    val feeConverter by lazy(LazyThreadSafetyMode.NONE) {
        FeeConverter(
            clickIntents = clickIntents,
            appCurrencyProvider = appCurrencyProvider,
            feeCryptoCurrencyStatusProvider = feeCryptoCurrencyStatusProvider,
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

    fun onFeeOnLoadedState(fees: TransactionFee): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = (feeState.feeSelectorState as? FeeSelectorState.Content)?.copy(
            fees = fees,
        ) ?: FeeSelectorState.Content(
            fees = fees,
            customValues = customFeeFieldConverter.convert(fees.normal),
        )

        val fee = feeConverter.convert(feeSelectorState)
        return state.copy(
            feeState = feeState.copy(
                feeSelectorState = feeSelectorState,
                fee = fee,
                isFeeApproximate = isFeeApproximate(fee),
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

        val updatedFeeSelectorState = feeSelectorState.copy(selectedFee = feeType)
        val fee = feeConverter.convert(updatedFeeSelectorState)
        return state.copy(
            feeState = feeState.copy(
                fee = fee,
                feeSelectorState = updatedFeeSelectorState,
            ),
        )
    }

    fun onCustomFeeValueChange(index: Int, value: String): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state
        val updatedFeeSelectorState = customFeeFieldConverter.onValueChange(feeSelectorState, index, value)

        val fee = feeConverter.convert(updatedFeeSelectorState)
        return state.copy(
            feeState = feeState.copy(
                feeSelectorState = updatedFeeSelectorState,
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

        val isNotCustom = feeSelectorState.selectedFee != FeeType.Custom
        val isNotEmptyCustom = if (customValue != null) {
            !customValue.value.parseToBigDecimal(customValue.decimals).isZero() && !isNotCustom
        } else {
            false
        }
        val noErrors = notifications.none { it is SendFeeNotification.Error }

        return noErrors && (isNotEmptyCustom || isNotCustom)
    }

    private fun isFeeApproximate(fee: Fee): Boolean {
        val cryptoCurrencyStatus = feeCryptoCurrencyStatusProvider()
        return isFeeApproximateUseCase(
            networkId = cryptoCurrencyStatus.currency.network.id,
            amountType = fee.amount.type,
        )
    }
}

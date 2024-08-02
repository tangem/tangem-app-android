package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.extensions.isZero
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.send.impl.presentation.state.SendNotification
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Factory to produce fee state for [SendUiState]
 */
internal class FeeStateFactory(
    private val clickIntents: SendClickIntents,
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val isFeeApproximateUseCase: IsFeeApproximateUseCase,
) {
    private val customFeeFieldConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendFeeCustomFieldConverter(
            clickIntents = clickIntents,
            stateRouterProvider = stateRouterProvider,
            appCurrencyProvider = appCurrencyProvider,
            feeCryptoCurrencyStatusProvider = feeCryptoCurrencyStatusProvider,
        )
    }

    val feeConverter by lazy(LazyThreadSafetyMode.NONE) {
        FeeConverter(
            clickIntents = clickIntents,
            stateRouterProvider = stateRouterProvider,
            appCurrencyProvider = appCurrencyProvider,
            feeCryptoCurrencyStatusProvider = feeCryptoCurrencyStatusProvider,
        )
    }

    fun onFeeOnLoadingState(): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val feeState = state.getFeeState(isEditState) ?: return state
        return state.copyWrapped(
            isEditState = isEditState,
            sendState = state.sendState?.copy(
                isPrimaryButtonEnabled = false,
            ),
            feeState = feeState.copy(
                feeSelectorState = if (feeState.feeSelectorState is FeeSelectorState.Content) {
                    feeState.feeSelectorState
                } else {
                    FeeSelectorState.Loading
                },
                notifications = persistentListOf(),
                isPrimaryButtonEnabled = false,
            ),
        )
    }

    fun onFeeOnLoadedState(fees: TransactionFee): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val feeState = state.getFeeState(isEditState) ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content

        val isCustomWasSelected = if (feeState.isCustomSelected) {
            feeSelectorState?.customValues ?: persistentListOf()
        } else {
            customFeeFieldConverter.convert(fees.normal)
        }
        val updatedFeeSelectorState = feeSelectorState?.copy(
            fees = fees,
            customValues = isCustomWasSelected,
        ) ?: FeeSelectorState.Content(
            fees = fees,
            customValues = customFeeFieldConverter.convert(fees.normal),
        )

        val fee = feeConverter.convert(updatedFeeSelectorState)
        return state.copyWrapped(
            isEditState = isEditState,
            sendState = state.sendState?.copy(
                isPrimaryButtonEnabled = true,
            ),
            feeState = feeState.copy(
                feeSelectorState = updatedFeeSelectorState,
                fee = fee,
                isFeeApproximate = isFeeApproximate(fee),
            ),
        )
    }

    fun onFeeOnErrorState(feeError: FeeSelectorState.Error): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        return state.copyWrapped(
            isEditState = isEditState,
            feeState = state.getFeeState(isEditState)?.copy(
                feeSelectorState = feeError,
            ),
            sendState = state.sendState?.copy(
                isPrimaryButtonEnabled = false,
            ),
        )
    }

    fun onFeeSelectedState(feeType: FeeType): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val feeState = state.getFeeState(isEditState) ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state

        val updatedFeeSelectorState = feeSelectorState.copy(selectedFee = feeType)
        val fee = feeConverter.convert(updatedFeeSelectorState)
        val isCustomFeeWasSelected = feeState.isCustomSelected || updatedFeeSelectorState.selectedFee == FeeType.Custom
        return state.copyWrapped(
            isEditState = isEditState,
            feeState = feeState.copy(
                fee = fee,
                isCustomSelected = isCustomFeeWasSelected,
                feeSelectorState = updatedFeeSelectorState,
            ),
        )
    }

    fun onCustomFeeValueChange(index: Int, value: String): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val feeState = state.getFeeState(isEditState) ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state
        val updatedFeeSelectorState = customFeeFieldConverter.onValueChange(feeSelectorState, index, value)

        val fee = feeConverter.convert(updatedFeeSelectorState)
        return state.copyWrapped(
            isEditState = isEditState,
            feeState = feeState.copy(
                feeSelectorState = updatedFeeSelectorState,
                fee = fee,
            ),
        )
    }

    fun getFeeNotificationState(notifications: ImmutableList<SendNotification>): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val feeState = state.getFeeState(isEditState) ?: return state
        return state.copyWrapped(
            isEditState = isEditState,
            feeState = feeState.copy(
                notifications = notifications,
                isPrimaryButtonEnabled = isPrimaryButtonEnabled(feeState, notifications),
            ),
        )
    }

    private fun isPrimaryButtonEnabled(
        feeState: SendStates.FeeState,
        notifications: ImmutableList<SendNotification>,
    ): Boolean {
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return false
        val customValue = feeSelectorState.customValues.firstOrNull()

        val isNotCustom = feeSelectorState.selectedFee != FeeType.Custom
        val isNotEmptyCustom = if (customValue != null) {
            !customValue.value.parseToBigDecimal(customValue.decimals).isZero() && !isNotCustom
        } else {
            false
        }
        val noErrors = notifications.none { it is SendNotification.Error }

        return noErrors && (isNotEmptyCustom || isNotCustom)
    }

    private fun isFeeApproximate(fee: Fee): Boolean {
        val cryptoCurrencyStatus = feeCryptoCurrencyStatusProvider() ?: return false
        return isFeeApproximateUseCase(
            networkId = cryptoCurrencyStatus.currency.network.id,
            amountType = fee.amount.type,
        )
    }
}

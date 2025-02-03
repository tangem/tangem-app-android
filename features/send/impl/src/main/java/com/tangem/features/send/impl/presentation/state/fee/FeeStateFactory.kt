package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.extensions.isZero
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.usecase.IsFeeApproximateUseCase
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.tangem.blockchain.common.AmountType as SdkAmountType

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
                isFeeApproximate = isFeeApproximate(state.amountState),
            ),
        )
    }

    fun onFeeOnErrorState(feeError: GetFeeError?): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        return state.copyWrapped(
            isEditState = isEditState,
            feeState = state.getFeeState(isEditState)?.copy(
                feeSelectorState = FeeSelectorState.Error(feeError),
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

    fun getFeeNotificationState(notifications: ImmutableList<NotificationUM>): SendUiState {
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

    fun tryAutoFixCustomFeeValue(): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val feeState = state.getFeeState(isEditState) ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state
        return when (feeSelectorState.selectedFee) {
            FeeType.Slow,
            FeeType.Market,
            FeeType.Fast,
            -> state
            FeeType.Custom -> {
                val updatedFeeSelectorState = customFeeFieldConverter.tryAutoFixValue(feeSelectorState)

                val fee = feeConverter.convert(updatedFeeSelectorState)
                return state.copyWrapped(
                    isEditState = isEditState,
                    feeState = feeState.copy(
                        feeSelectorState = updatedFeeSelectorState,
                        fee = fee,
                    ),
                )
            }
        }
    }

    private fun isPrimaryButtonEnabled(
        feeState: SendStates.FeeState,
        notifications: ImmutableList<NotificationUM>,
    ): Boolean {
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return false
        val customValue = feeSelectorState.customValues.firstOrNull()

        val isNotCustom = feeSelectorState.selectedFee != FeeType.Custom
        val isNotEmptyCustom = if (customValue != null) {
            !customValue.value.parseToBigDecimal(customValue.decimals).isZero() && !isNotCustom
        } else {
            false
        }
        val noErrors = notifications.none { it is NotificationUM.Error }

        return noErrors && (isNotEmptyCustom || isNotCustom)
    }

    private fun isFeeApproximate(state: AmountState): Boolean {
        val cryptoCurrencyStatus = feeCryptoCurrencyStatusProvider() ?: return false
        val amount = (state as? AmountState.Data)?.amountTextField?.cryptoAmount ?: return false
        return isFeeApproximateUseCase(
            networkId = cryptoCurrencyStatus.currency.network.id,
            amountType = amount.type.toSdkAmountType(),
        )
    }

    private fun AmountType.toSdkAmountType(): SdkAmountType {
        return when (this) {
            AmountType.CoinType -> SdkAmountType.Coin
            is AmountType.FiatType -> error("unsupported type FiatType")
            AmountType.ReserveType -> SdkAmountType.Reserve
            is AmountType.TokenType -> SdkAmountType.Token(
                Token(
                    name = this.token.name,
                    symbol = this.token.symbol,
                    contractAddress = this.token.contractAddress,
                    decimals = this.token.decimals,
                    id = this.token.id.rawCurrencyId?.value,
                ),
            )
        }
    }
}
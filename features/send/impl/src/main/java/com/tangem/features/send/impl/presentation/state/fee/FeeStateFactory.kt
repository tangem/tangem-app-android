package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.isNullOrZero
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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
    userWalletProvider: Provider<UserWallet>,
) {
    private val customFeeFieldConverter by lazy {
        SendFeeCustomFieldConverter(
            clickIntents = clickIntents,
            appCurrencyProvider = appCurrencyProvider,
        )
    }

    private val feeNotificationFactory = FeeNotificationFactory(
        coinCryptoCurrencyStatusProvider = coinCryptoCurrencyStatusProvider,
        userWalletProvider = userWalletProvider,
        clickIntents = clickIntents,
    )

    fun onFeeOnLoadingState(): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        return state.copy(
            feeState = feeState.copy(
                feeSelectorState = FeeSelectorState.Loading,
                notifications = persistentListOf(),
                isPrimaryButtonEnabled = feeState.isPrimaryButtonEnabled(),
            ),
        )
    }

    fun onFeeOnLoadedState(fees: TransactionFee): SendUiState {
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

        val fee = feeSelectorState.getFee()
        val receivedAmount = calculateReceiveAmount(state, fee)
        val updatedState = feeState.copy(
            feeSelectorState = feeSelectorState,
            fee = fee,
            receivedAmountValue = receivedAmount,
            receivedAmount = getFormattedValue(receivedAmount),
            isSubtract = checkAutoSubtract(state, fee, balance),
        )
        return state.copy(
            feeState = updatedState.copy(
                notifications = feeNotificationFactory(
                    feeState = updatedState,
                    amountValue = state.amountState?.amountValue,
                ),
                isPrimaryButtonEnabled = updatedState.isPrimaryButtonEnabled(),
            ),
        )
    }

    fun onFeeOnErrorState(): SendUiState {
        val state = currentStateProvider()
        val updatedState = state.feeState?.copy(
            feeSelectorState = FeeSelectorState.Error,
        )
        return state.copy(
            feeState = updatedState?.copy(
                notifications = feeNotificationFactory(
                    feeState = updatedState,
                    amountValue = state.amountState?.amountValue,
                ),
            ),
        )
    }

    fun onFeeSelectedState(feeType: FeeType): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state
        val balance = coinCryptoCurrencyStatusProvider().value.amount ?: BigDecimal.ZERO

        val updatedFeeSelectorState = feeSelectorState.copy(selectedFee = feeType)
        val fee = updatedFeeSelectorState.getFee()
        val receivedAmount = calculateReceiveAmount(state, fee)
        val updatedState = feeState.copy(
            fee = fee,
            feeSelectorState = updatedFeeSelectorState,
            receivedAmountValue = receivedAmount,
            receivedAmount = getFormattedValue(receivedAmount),
            isSubtract = checkAutoSubtract(state, fee, balance),
        )

        return state.copy(
            feeState = updatedState.copy(
                notifications = feeNotificationFactory(
                    feeState = updatedState,
                    amountValue = state.amountState?.amountValue,
                ),
                isPrimaryButtonEnabled = updatedState.isPrimaryButtonEnabled(),
            ),
        )
    }

    fun onCustomFeeValueChange(index: Int, value: String): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state
        val updatedFeeSelectorState = feeSelectorState.copy(
            customValues = feeSelectorState.customValues.toMutableList().apply {
                set(index, feeSelectorState.customValues[index].copy(value = value))
            }.toImmutableList(),
        )
        val balance = coinCryptoCurrencyStatusProvider().value.amount ?: BigDecimal.ZERO

        val fee = updatedFeeSelectorState.getFee()
        val receivedAmount = calculateReceiveAmount(state, fee)
        val updatedState = feeState.copy(
            feeSelectorState = updatedFeeSelectorState,
            fee = fee,
            receivedAmountValue = receivedAmount,
            receivedAmount = getFormattedValue(receivedAmount),
            isSubtract = checkAutoSubtract(state, fee, balance),
        )
        return state.copy(
            feeState = updatedState.copy(
                notifications = feeNotificationFactory(
                    feeState = updatedState,
                    amountValue = state.amountState?.amountValue,
                ),
                isPrimaryButtonEnabled = updatedState.isPrimaryButtonEnabled(),
            ),
        )
    }

    fun onSubtractSelect(value: Boolean): SendUiState {
        val state = currentStateProvider()
        val feeState = state.feeState ?: return state
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return state
        val fee = feeSelectorState.getFee()
        val receivedAmount = calculateReceiveAmount(state, fee)
        val updatedState = feeState.copy(
            isSubtract = value,
            isUserSubtracted = true,
            receivedAmountValue = receivedAmount,
            receivedAmount = getFormattedValue(receivedAmount),
            fee = fee,
        )
        return state.copy(
            feeState = updatedState.copy(
                notifications = feeNotificationFactory(
                    feeState = updatedState,
                    amountValue = state.amountState?.amountValue,
                ),
                isPrimaryButtonEnabled = updatedState.isPrimaryButtonEnabled(),
            ),
        )
    }

    private fun SendStates.FeeState.isPrimaryButtonEnabled(): Boolean {
        return when (feeSelectorState) {
            is FeeSelectorState.Content -> {
                val customValue = feeSelectorState.customValues.firstOrNull()?.value?.toBigDecimalOrNull()
                val balance = coinCryptoCurrencyStatusProvider().value.amount ?: BigDecimal.ZERO
                val fee = feeSelectorState.getFee()
                val feeValue = fee.amount.value ?: BigDecimal.ZERO

                val isNotCustom = feeSelectorState.selectedFee != FeeType.CUSTOM
                val isNotEmptyCustom = !customValue.isNullOrZero() && !isNotCustom
                val isFiatAnotherCurrency = cryptoCurrencyStatusProvider().currency.symbol != fee.amount.currencySymbol
                val isSubtractRequired = if (feeValue + receivedAmountValue >= balance) isSubtract else true
                val isBalanceEnough = feeValue + receivedAmountValue <= balance

                (isFiatAnotherCurrency || isBalanceEnough && isSubtractRequired) && (isNotEmptyCustom || isNotCustom)
            }
            else -> false
        }
    }

    private fun checkAutoSubtract(state: SendUiState, fee: Fee, balance: BigDecimal): Boolean {
        val feeState = state.feeState ?: return false
        val amountValue = state.amountState?.amountValue ?: BigDecimal.ZERO
        val feeAmount = fee.amount.value ?: BigDecimal.ZERO
        return if (feeState.isUserSubtracted) {
            feeState.isSubtract
        } else {
            amountValue + feeAmount >= balance
        }
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

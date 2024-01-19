package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal class FeeNotificationFactory(
    private val coinCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val userWalletProvider: Provider<UserWallet>,
    private val clickIntents: SendClickIntents,
) {

    operator fun invoke(feeState: SendStates.FeeState, amountValue: BigDecimal?): ImmutableList<SendFeeNotification> =
        buildList {
            when (val feeSelectorState = feeState.feeSelectorState) {
                FeeSelectorState.Loading -> Unit
                FeeSelectorState.Error -> {
                    addFeeUnreachableNotification(feeSelectorState)
                }
                is FeeSelectorState.Content -> {
                    val customFee = feeSelectorState.customValues
                    val selectedFee = feeSelectorState.selectedFee
                    addTooLowNotification(feeSelectorState.fees, selectedFee, customFee)
                    addTooHighNotification(feeSelectorState.fees, selectedFee, customFee)
                    addFeeCoverageNotification(feeState, amountValue)
                    addExceedsBalanceNotification(feeState.fee)
                }
            }
        }.toImmutableList()

    private fun MutableList<SendFeeNotification>.addFeeUnreachableNotification(feeSelectorState: FeeSelectorState) {
        if (feeSelectorState is FeeSelectorState.Error) {
            add(SendFeeNotification.Warning.NetworkFeeUnreachable(clickIntents::feeReload))
        }
    }

    private fun MutableList<SendFeeNotification>.addTooLowNotification(
        transactionFee: TransactionFee,
        selectedFee: FeeType,
        customFee: List<SendTextField.CustomFee>,
    ) {
        val multipleFees = transactionFee as? TransactionFee.Choosable ?: return
        val minimumValue = multipleFees.minimum.amount.value ?: return
        val customValue = customFee.firstOrNull()?.value?.toBigDecimalOrNull() ?: return
        if (selectedFee == FeeType.CUSTOM && minimumValue > customValue) {
            add(SendFeeNotification.Informational.TooLow)
        }
    }

    private fun MutableList<SendFeeNotification>.addTooHighNotification(
        transactionFee: TransactionFee,
        selectedFee: FeeType,
        customFee: List<SendTextField.CustomFee>,
    ) {
        val multipleFees = transactionFee as? TransactionFee.Choosable ?: return
        val highValue = multipleFees.priority.amount.value ?: return
        val customValue = customFee.firstOrNull()?.value?.toBigDecimalOrNull() ?: return
        val diff = customValue / highValue
        if (selectedFee == FeeType.CUSTOM && diff > FEE_MAX_DIFF) {
            add(SendFeeNotification.Warning.TooHigh(diff.toInt().toString()))
        }
    }

    private fun MutableList<SendFeeNotification>.addFeeCoverageNotification(
        feeState: SendStates.FeeState,
        amountValue: BigDecimal?,
    ) {
        val cryptoAmount = coinCryptoCurrencyStatusProvider().value.amount ?: BigDecimal.ZERO
        val feeValue = feeState.fee?.amount?.value ?: BigDecimal.ZERO
        val value = amountValue ?: BigDecimal.ZERO
        if (cryptoAmount <= value + feeValue && feeState.isSubtract && !feeState.isUserSubtracted) {
            add(SendFeeNotification.Warning.NetworkCoverage)
        }
    }

    private fun MutableList<SendFeeNotification>.addExceedsBalanceNotification(fee: Fee?) {
        val coinCryptoCurrency = coinCryptoCurrencyStatusProvider()
        val cryptoAmount = coinCryptoCurrency.value.amount ?: BigDecimal.ZERO
        val feeValue = fee?.amount?.value ?: BigDecimal.ZERO

        if (feeValue > cryptoAmount) {
            add(
                SendFeeNotification.Error.ExceedsBalance(
                    coinCryptoCurrency.currency.networkIconResId,
                ) {
                    clickIntents.onTokenDetailsClick(
                        userWalletProvider().walletId,
                        coinCryptoCurrency.currency,
                    )
                },
            )
        }
    }

    companion object {
        private val FEE_MAX_DIFF = BigDecimal(5)
    }
}
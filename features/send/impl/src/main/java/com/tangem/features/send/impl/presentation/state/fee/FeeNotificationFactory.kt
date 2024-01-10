package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal class FeeNotificationFactory(
    private val coinCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val userWalletProvider: Provider<UserWallet>,
    private val clickIntents: SendClickIntents,
) {

    operator fun invoke(feeState: SendStates.FeeState): ImmutableList<SendFeeNotification> {
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return persistentListOf()
        val customFee = feeSelectorState.customValues
        val selectedFee = feeSelectorState.selectedFee

        return buildList {
            addTooLowNotification(feeSelectorState.fees, selectedFee, customFee)
            addTooHighNotification(feeSelectorState.fees, selectedFee, customFee)
            addFeeCoverageNotification()
            addExceedsBalanceNotification(feeSelectorState)
        }.toImmutableList()
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

    private fun MutableList<SendFeeNotification>.addFeeCoverageNotification() {
        // TODO add fee coverage condition [REDACTED_JIRA]
        add(SendFeeNotification.Warning.NetworkCoverage)
    }

    private fun MutableList<SendFeeNotification>.addExceedsBalanceNotification(
        feeSelectorState: FeeSelectorState.Content,
    ) {
        val coinCryptoCurrency = coinCryptoCurrencyStatusProvider()
        val choosableFee = feeSelectorState.fees as? TransactionFee.Choosable
        val fee = when (feeSelectorState.selectedFee) {
            FeeType.SLOW -> choosableFee?.minimum?.amount?.value
            FeeType.MARKET -> feeSelectorState.fees.normal.amount.value
            FeeType.FAST -> choosableFee?.priority?.amount?.value
            FeeType.CUSTOM -> feeSelectorState.customValues.firstOrNull()?.value?.toBigDecimalOrNull()
        } ?: return

        if (fee > coinCryptoCurrency.value.amount) {
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
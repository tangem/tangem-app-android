package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.tokens.GetBalanceNotEnoughForFeeWarningUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.toFormattedString
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

internal class FeeNotificationFactory(
    private val coinCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val currentStateProvider: Provider<SendUiState>,
    private val userWalletProvider: Provider<UserWallet>,
    private val clickIntents: SendClickIntents,
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
) {

    fun create() = currentStateProvider().currentState
        .filter { it == SendUiStateType.Fee }
        .map {
            val state = currentStateProvider()
            val feeState = state.feeState ?: return@map persistentListOf()
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
                        addFeeCoverageNotification(feeState, state.amountState)
                        addExceedsBalanceNotification(feeState.fee)
                    }
                }
            }.toImmutableList()
        }

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
        val customAmount = customFee.firstOrNull() ?: return
        val customValue = customAmount.value.parseToBigDecimal(customAmount.decimals)
        if (selectedFee == FeeType.CUSTOM && minimumValue > customValue) {
            add(SendFeeNotification.Warning.TooLow)
        }
    }

    private fun MutableList<SendFeeNotification>.addTooHighNotification(
        transactionFee: TransactionFee,
        selectedFee: FeeType,
        customFee: List<SendTextField.CustomFee>,
    ) {
        val multipleFees = transactionFee as? TransactionFee.Choosable ?: return
        val highValue = multipleFees.priority.amount.value ?: return
        val customAmount = customFee.firstOrNull() ?: return
        val customValue = customAmount.value.parseToBigDecimal(customAmount.decimals)
        val diff = customValue / highValue
        if (selectedFee == FeeType.CUSTOM && diff > FEE_MAX_DIFF) {
            add(SendFeeNotification.Warning.TooHigh(diff.toFormattedString(HIGH_FEE_DIFF_DECIMALS)))
        }
    }

    private fun MutableList<SendFeeNotification>.addFeeCoverageNotification(
        feeState: SendStates.FeeState,
        amountState: SendStates.AmountState?,
    ) {
        if (!feeState.isSubtractAvailable) return

        val cryptoAmount = coinCryptoCurrencyStatusProvider().value.amount ?: return
        val feeValue = feeState.fee?.amount?.value ?: return
        val value = amountState?.amountTextField?.cryptoAmount?.value ?: return
        if (cryptoAmount <= value + feeValue && feeState.isSubtract && !feeState.isUserSubtracted) {
            add(SendFeeNotification.Warning.NetworkCoverage)
        }
    }

    private suspend fun MutableList<SendFeeNotification>.addExceedsBalanceNotification(fee: Fee?) {
        val feeValue = fee?.amount?.value ?: BigDecimal.ZERO
        val userWalletId = userWalletProvider().walletId
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()

        val warning = getBalanceNotEnoughForFeeWarningUseCase(
            fee = feeValue,
            userWalletId = userWalletId,
            tokenStatus = cryptoCurrencyStatus,
            coinStatus = coinCryptoCurrencyStatusProvider(),
        ).fold(
            ifLeft = { null },
            ifRight = { it },
        ) ?: return

        when (warning) {
            is CryptoCurrencyWarning.BalanceNotEnoughForFee -> {
                add(
                    SendFeeNotification.Error.ExceedsBalance(
                        warning.coinCurrency.networkIconResId,
                        networkName = warning.coinCurrency.name,
                        currencyName = cryptoCurrencyStatus.currency.name,
                        feeName = warning.coinCurrency.name,
                        feeSymbol = warning.coinCurrency.symbol,
                        onClick = {
                            clickIntents.onTokenDetailsClick(
                                userWalletId = userWalletId,
                                currency = warning.coinCurrency,
                            )
                        },
                    ),
                )
            }
            is CryptoCurrencyWarning.CustomTokenNotEnoughForFee -> {
                val currency = warning.feeCurrency
                add(
                    SendFeeNotification.Error.ExceedsBalance(
                        networkIconId = currency?.networkIconResId ?: R.drawable.ic_alert_24,
                        currencyName = warning.currency.name,
                        feeName = warning.feeCurrencyName,
                        feeSymbol = warning.feeCurrencySymbol,
                        networkName = warning.networkName,
                        onClick = currency?.let {
                            {
                                clickIntents.onTokenDetailsClick(
                                    userWalletId,
                                    currency,
                                )
                            }
                        },
                    ),
                )
            }
            else -> Unit
        }
    }

    companion object {
        private val FEE_MAX_DIFF = BigDecimal(5)
        private const val HIGH_FEE_DIFF_DECIMALS = 0
    }
}

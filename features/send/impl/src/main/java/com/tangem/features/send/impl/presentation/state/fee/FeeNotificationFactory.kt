package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.tokens.GetBalanceNotEnoughForFeeWarningUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.toFormattedString
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class FeeNotificationFactory(
    private val coinCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val currentStateProvider: Provider<SendUiState>,
    private val userWalletProvider: Provider<UserWallet>,
    private val stateRouterProvider: Provider<StateRouter>,
    private val clickIntents: SendClickIntents,
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun create() = stateRouterProvider().currentState
        .filter { it.type == SendUiStateType.Fee }
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
                        addTooHighNotification(feeSelectorState.fees, selectedFee, customFee)
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
        if (selectedFee == FeeType.Custom && diff > FEE_MAX_DIFF) {
            add(SendFeeNotification.Warning.TooHigh(diff.toFormattedString(HIGH_FEE_DIFF_DECIMALS)))
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

        val mergeFeeNetworkName = cryptoCurrencyStatus.shouldMergeFeeNetworkName()
        when (warning) {
            is CryptoCurrencyWarning.BalanceNotEnoughForFee -> {
                add(
                    SendFeeNotification.Error.ExceedsBalance(
                        networkIconId = warning.coinCurrency.networkIconResId,
                        networkName = warning.coinCurrency.name,
                        currencyName = cryptoCurrencyStatus.currency.name,
                        feeName = warning.coinCurrency.name,
                        feeSymbol = warning.coinCurrency.symbol,
                        mergeFeeNetworkName = mergeFeeNetworkName,
                        onClick = {
                            clickIntents.onTokenDetailsClick(
                                userWalletId = userWalletId,
                                currency = warning.coinCurrency,
                            )
                        },
                    ),
                )
                analyticsEventHandler.send(
                    SendAnalyticEvents.NoticeNotEnoughFee(
                        token = cryptoCurrencyStatus.currency.symbol,
                        blockchain = cryptoCurrencyStatus.currency.network.name,
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
                        mergeFeeNetworkName = mergeFeeNetworkName,
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
                analyticsEventHandler.send(
                    SendAnalyticEvents.NoticeNotEnoughFee(
                        token = warning.currency.symbol,
                        blockchain = warning.networkName,
                    ),
                )
            }
            else -> Unit
        }
    }

    // workaround for networks that users have misunderstanding
    private fun CryptoCurrencyStatus.shouldMergeFeeNetworkName(): Boolean {
        return Blockchain.fromNetworkId(this.currency.network.backendId) == Blockchain.Arbitrum
    }

    companion object {
        private val FEE_MAX_DIFF = BigDecimal(5)
        private const val HIGH_FEE_DIFF_DECIMALS = 0
    }
}
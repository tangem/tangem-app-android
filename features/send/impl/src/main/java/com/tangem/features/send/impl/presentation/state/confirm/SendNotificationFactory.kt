package com.tangem.features.send.impl.presentation.state.confirm

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.extensions.networkIconResId
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.minimalAmount
import com.tangem.domain.tokens.GetBalanceNotEnoughForFeeWarningUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.analytics.SendAnalyticEvents
import com.tangem.features.send.impl.presentation.state.*
import com.tangem.features.send.impl.presentation.state.fee.*
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.lib.crypto.BlockchainUtils.isDogecoin
import com.tangem.lib.crypto.BlockchainUtils.isTezos
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class SendNotificationFactory(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val coinCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val currentStateProvider: Provider<SendUiState>,
    private val userWalletProvider: Provider<UserWallet>,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val stateRouterProvider: Provider<StateRouter>,
    private val isSubtractAvailableProvider: Provider<Boolean>,
    private val clickIntents: SendClickIntents,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getBalanceNotEnoughForFeeWarningUseCase: GetBalanceNotEnoughForFeeWarningUseCase,
) {

    fun create(): Flow<ImmutableList<SendNotification>> = stateRouterProvider().currentState
        .filter { it.type == SendUiStateType.Send }
        .map {
            val state = currentStateProvider()
            val sendState = state.sendState ?: return@map persistentListOf()
            val feeState = state.feeState ?: return@map persistentListOf()
            val balance = cryptoCurrencyStatusProvider().value.amount ?: BigDecimal.ZERO
            val amountValue = state.amountState?.amountTextField?.cryptoAmount?.value ?: BigDecimal.ZERO
            val feeValue = feeState.fee?.amount?.value ?: BigDecimal.ZERO
            val isFeeCoverage = checkFeeCoverage(
                isSubtractAvailable = isSubtractAvailableProvider(),
                balance = balance,
                amountValue = amountValue,
                feeValue = feeValue,
            )
            val sendingAmount = calculateSubtractedAmount(
                isFeeCoverage = isFeeCoverage,
                cryptoCurrencyStatus = cryptoCurrencyStatusProvider(),
                amountValue = amountValue,
                feeValue = feeValue,
            )
            buildList {
                // errors
                addFeeUnreachableNotification(feeState.feeSelectorState)
                addExceedBalanceNotification(feeValue, sendingAmount)
                addExceedsBalanceNotification(feeState.fee)
                addMinimumAmountErrorNotification(feeValue, sendingAmount)
                addDustWarningNotification(feeValue, sendingAmount)
                addTransactionLimitErrorNotification(feeValue, sendingAmount)
                // warnings
                addExistentialWarningNotification(feeValue, amountValue)
                addFeeCoverageNotification(isFeeCoverage)
                addHighFeeWarningNotification(amountValue, sendState.ignoreAmountReduce)
                addTooHighNotification(feeState.feeSelectorState)
                addTooLowNotification(feeState)
            }.toImmutableList()
        }

    fun dismissNotificationState(clazz: Class<out SendNotification>, isIgnored: Boolean = false): SendUiState {
        val state = currentStateProvider()
        val sendState = state.sendState ?: return state
        val notificationsToRemove = sendState.notifications.filterIsInstance(clazz)
        val updatedNotifications = sendState.notifications.toMutableList()
        updatedNotifications.removeAll(notificationsToRemove)
        return state.copy(
            sendState = sendState.copy(
                ignoreAmountReduce = isIgnored,
                notifications = updatedNotifications.toImmutableList(),
            ),
        )
    }

    private fun MutableList<SendNotification>.addFeeUnreachableNotification(feeSelectorState: FeeSelectorState) {
        if (feeSelectorState is FeeSelectorState.Error) {
            add(SendNotification.Warning.NetworkFeeUnreachable(clickIntents::feeReload))
        }
    }

    private fun MutableList<SendNotification>.addExceedBalanceNotification(
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO

        if (!isSubtractAvailableProvider()) return

        val showNotification = receivedAmount + feeAmount > balance
        if (showNotification) {
            add(SendNotification.Error.TotalExceedsBalance)
        }
    }

    // todo temporarily disabling notification
    // private suspend fun MutableList<SendNotification>.addReserveAmountErrorNotification(recipientAddress: String) {
    //     val userWalletId = userWalletProvider().walletId
    //     val cryptoCurrency = cryptoCurrencyStatusProvider().currency
    //     val isAccountFunded = currencyChecksRepository.checkIfAccountFunded(
    //         userWalletId,
    //         cryptoCurrency.network,
    //         recipientAddress,
    //     )
    //     val minimumAmount = currencyChecksRepository.getReserveAmount(userWalletId, cryptoCurrency.network)
    //     if (!isAccountFunded && minimumAmount != null && minimumAmount > BigDecimal.ZERO) {
    //         add(
    //             SendNotification.Error.ReserveAmountError(
    //                 BigDecimalFormatter.formatCryptoAmount(
    //                     cryptoAmount = minimumAmount,
    //                     cryptoCurrency = cryptoCurrency,
    //                 ),
    //             ),
    //         )
    //     }
    // }

    private suspend fun MutableList<SendNotification>.addTransactionLimitErrorNotification(
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
    ) {
        val userWalletId = userWalletProvider().walletId
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        val utxoLimit = currencyChecksRepository.checkUtxoAmountLimit(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
            amount = receivedAmount,
            fee = feeAmount,
        )

        if (utxoLimit != null) {
            add(
                SendNotification.Error.TransactionLimitError(
                    cryptoCurrency = cryptoCurrency.name,
                    utxoLimit = utxoLimit.maxLimit.toPlainString(),
                    amountLimit = BigDecimalFormatter.formatCryptoAmount(
                        cryptoAmount = utxoLimit.maxAmount,
                        cryptoCurrency = cryptoCurrency,
                    ),
                    onConfirmClick = {
                        clickIntents.onAmountReduceClick(
                            utxoLimit.maxAmount,
                            SendNotification.Error.TransactionLimitError::class.java,
                        )
                    },
                ),
            )
        }
    }

    private suspend fun MutableList<SendNotification>.addExistentialWarningNotification(
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
    ) {
        val userWalletId = userWalletProvider().walletId
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val balance = cryptoCurrencyStatus.value.amount ?: return
        val spendingAmount = if (cryptoCurrency is CryptoCurrency.Token) {
            feeAmount
        } else {
            feeAmount + receivedAmount
        }
        val currencyDeposit = currencyChecksRepository.getExistentialDeposit(
            userWalletId,
            cryptoCurrency.network,
        )
        val diff = balance.minus(spendingAmount)
        if (currencyDeposit != null && currencyDeposit > diff) {
            add(
                SendNotification.Error.ExistentialDeposit(
                    BigDecimalFormatter.formatCryptoAmountUncapped(
                        cryptoAmount = currencyDeposit,
                        cryptoCurrency = cryptoCurrency,
                    ),
                ),
            )
        }
    }

    private fun MutableList<SendNotification>.addFeeCoverageNotification(sendingAmount: Boolean) {
        if (sendingAmount) {
            analyticsEventHandler.send(SendAnalyticEvents.NoticeFeeCoverage)
            add(SendNotification.Warning.FeeCoverageNotification)
        }
    }

    private fun MutableList<SendNotification>.addHighFeeWarningNotification(
        sendAmount: BigDecimal,
        ignoreAmountReduce: Boolean,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
        val isTezos = isTezos(cryptoCurrencyStatus.currency.network.id.value)
        val threshold = Blockchain.Tezos.minimalAmount()
        val isTotalBalance = sendAmount >= balance && balance > threshold
        if (!ignoreAmountReduce && isTotalBalance && isTezos) {
            add(
                SendNotification.Warning.HighFeeError(
                    amount = threshold.toPlainString(),
                    onConfirmClick = {
                        val reduceTo = sendAmount.minus(threshold)
                        clickIntents.onAmountReduceClick(reduceTo, SendNotification.Warning.HighFeeError::class.java)
                    },
                    onCloseClick = {
                        clickIntents.onNotificationCancel(SendNotification.Warning.HighFeeError::class.java)
                    },
                ),
            )
        }
    }

    // todo remove in AND-6877
    private fun MutableList<SendNotification>.addMinimumAmountErrorNotification(
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
    ) {
        val coinCryptoCurrencyStatus = coinCryptoCurrencyStatusProvider()
        val minimum = BigDecimal(DOGECOIN_MINIMUM)

        val isDogecoin = isDogecoin(coinCryptoCurrencyStatus.currency.network.id.value)
        val isExceedDustLimit = checkDustLimits(feeAmount, receivedAmount, minimum)
        if (isDogecoin && isExceedDustLimit) {
            add(SendNotification.Error.MinimumAmountError(DOGECOIN_MINIMUM))
        }
    }

    private suspend fun MutableList<SendNotification>.addDustWarningNotification(
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val dustValue = currencyChecksRepository.getDustValue(
            userWalletProvider().walletId,
            cryptoCurrencyStatus.currency.network,
        ) ?: return

        if (checkDustLimits(feeAmount, receivedAmount, dustValue)) {
            add(
                SendNotification.Error.MinimumAmountError(dustValue.toPlainString()),
            )
        }
    }

    private fun MutableList<SendNotification>.addTooLowNotification(feeState: SendStates.FeeState) {
        val feeSelectorState = feeState.feeSelectorState as? FeeSelectorState.Content ?: return
        val multipleFees = feeSelectorState.fees as? TransactionFee.Choosable ?: return
        val minimumValue = multipleFees.minimum.amount.value ?: return
        val customAmount = feeSelectorState.customValues.firstOrNull() ?: return
        val customValue = customAmount.value.parseToBigDecimal(customAmount.decimals)
        if (feeSelectorState.selectedFee == FeeType.Custom && minimumValue > customValue) {
            add(SendNotification.Warning.FeeTooLow)
            analyticsEventHandler.send(
                SendAnalyticEvents.NoticeTransactionDelays(
                    cryptoCurrencyStatusProvider().currency.symbol,
                ),
            )
        }
    }

    private fun MutableList<SendNotification>.addTooHighNotification(feeSelectorState: FeeSelectorState) {
        if (feeSelectorState !is FeeSelectorState.Content) return

        checkIfFeeTooHigh(feeSelectorState) { diff ->
            add(SendNotification.Warning.TooHigh(diff))
        }
    }

    private suspend fun MutableList<SendNotification>.addExceedsBalanceNotification(fee: Fee?) {
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
                    SendNotification.Error.ExceedsBalance(
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
                    SendNotification.Error.ExceedsBalance(
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

    private fun checkDustLimits(feeAmount: BigDecimal, receivedAmount: BigDecimal, dustValue: BigDecimal): Boolean {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO

        val totalAmount = feeAmount + receivedAmount
        val change = balance - totalAmount
        val isChangeLowerThanDust = change < dustValue && change > BigDecimal.ZERO
        return receivedAmount < dustValue || isChangeLowerThanDust
    }

    companion object {
        private const val DOGECOIN_MINIMUM = "0.01"
    }
}

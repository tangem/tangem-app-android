package com.tangem.features.send.impl.presentation.state

import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.repository.CurrencyChecksRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.isNullOrZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

internal class SendNotificationFactory(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val coinCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val currentStateProvider: Provider<SendUiState>,
    private val userWalletProvider: Provider<UserWallet>,
    private val currencyChecksRepository: CurrencyChecksRepository,
    private val clickIntents: SendClickIntents,
) {

    fun create(): Flow<ImmutableList<SendNotification>> = currentStateProvider().currentState
        .filter { it == SendUiStateType.Send }
        .map {
            val state = currentStateProvider()
            val feeState = state.feeState ?: return@map persistentListOf()
            val recipientState = state.recipientState ?: return@map persistentListOf()
            val feeAmount = feeState.fee?.amount?.value ?: BigDecimal.ZERO
            val amountValue = state.amountState?.amountTextField?.value?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val sendAmount = if (feeState.isSubtract) feeState.receivedAmountValue else amountValue
            buildList {
                // errors
                addExceedBalanceNotification(feeAmount, sendAmount)
                addInvalidAmountNotification(feeState.isSubtract, sendAmount)
                addMinimumAmountErrorNotification(feeAmount, sendAmount)
                addDustWarningNotification(feeAmount, sendAmount)
                addReserveAmountErrorNotification(recipientState.addressTextField.value)
                addTransactionLimitErrorNotification(feeAmount, sendAmount)
                // warnings
                addExistentialWarningNotification(feeAmount, sendAmount)
                addHighFeeWarningNotification(amountValue, state.sendState.ignoreAmountReduce)
            }.toImmutableList()
        }

    fun dismissHighFeeWarningState(): SendUiState {
        val state = currentStateProvider()
        val sendState = state.sendState
        val updatedNotifications = sendState.notifications.filterNot { it is SendNotification.Warning.HighFeeError }
        return state.copy(
            sendState = sendState.copy(
                ignoreAmountReduce = true,
                notifications = updatedNotifications.toImmutableList(),
            ),
        )
    }

    private fun MutableList<SendNotification>.addExceedBalanceNotification(
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val coinCryptoCurrencyStatus = coinCryptoCurrencyStatusProvider()
        val cryptoAmount = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
        val coinCryptoAmount = coinCryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO

        val showNotification = if (cryptoCurrencyStatus.currency is CryptoCurrency.Token) {
            receivedAmount > cryptoAmount || feeAmount > coinCryptoAmount
        } else {
            receivedAmount + feeAmount > cryptoAmount
        }

        if (showNotification) {
            add(SendNotification.Error.TotalExceedsBalance)
        }
    }

    private fun MutableList<SendNotification>.addInvalidAmountNotification(
        isSubtractAmount: Boolean,
        receivedAmount: BigDecimal,
    ) {
        if (isSubtractAmount && receivedAmount <= BigDecimal.ZERO) {
            add(SendNotification.Error.InvalidAmount)
        }
    }

    private fun MutableList<SendNotification>.addMinimumAmountErrorNotification(
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
    ) {
        val coinCryptoCurrencyStatus = coinCryptoCurrencyStatusProvider()

        val totalAmount = feeAmount + receivedAmount
        val balance = coinCryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO

        // TODO Move Blockchain check elsewhere
        when (coinCryptoCurrencyStatus.currency.network.id.value) {
            Blockchain.Cardano.id -> {
                if (receivedAmount > BigDecimal.ONE || balance - totalAmount < BigDecimal.ONE) {
                    add(SendNotification.Error.MinimumAmountError(CARDANO_MINIMUM))
                }
            }
            Blockchain.Dogecoin.id -> {
                val minimum = BigDecimal(DOGECOIN_MINIMUM)
                if (receivedAmount > minimum || balance - totalAmount < minimum) {
                    add(SendNotification.Error.MinimumAmountError(DOGECOIN_MINIMUM))
                }
            }
            else -> Unit
        }
    }

    private suspend fun MutableList<SendNotification>.addReserveAmountErrorNotification(recipientAddress: String) {
        val userWalletId = userWalletProvider().walletId
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        val isAccountFunded = currencyChecksRepository.checkIfAccountFunded(
            userWalletId,
            cryptoCurrency.network,
            recipientAddress,
        )
        val minimumAmount = currencyChecksRepository.getReserveAmount(userWalletId, cryptoCurrency.network)
        if (!isAccountFunded && minimumAmount != null && minimumAmount > BigDecimal.ZERO) {
            add(
                SendNotification.Error.ReserveAmountError(
                    BigDecimalFormatter.formatCryptoAmount(
                        cryptoAmount = minimumAmount,
                        cryptoCurrency = cryptoCurrency,
                    ),
                ),
            )
        }
    }

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
                ),
            )
        }
    }

    private suspend fun MutableList<SendNotification>.addExistentialWarningNotification(
        feeAmount: BigDecimal,
        receivedAmount: BigDecimal,
    ) {
        val userWalletId = userWalletProvider().walletId
        val cryptoCurrency = cryptoCurrencyStatusProvider().currency
        val spendingAmount = if (cryptoCurrency is CryptoCurrency.Token) {
            feeAmount
        } else {
            feeAmount + receivedAmount
        }
        val currencyDeposit = currencyChecksRepository.getExistentialDeposit(
            userWalletId,
            cryptoCurrency.network,
        )
        if (currencyDeposit != null && currencyDeposit > spendingAmount) {
            add(
                SendNotification.Warning.ExistentialDeposit(
                    BigDecimalFormatter.formatCryptoAmount(
                        cryptoAmount = currencyDeposit,
                        cryptoCurrency = cryptoCurrency,
                    ),
                ),
            )
        }
    }

    private fun MutableList<SendNotification>.addHighFeeWarningNotification(
        sendAmount: BigDecimal,
        ignoreAmountReduce: Boolean,
    ) {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
        val isTezos = cryptoCurrencyStatus.currency.network.id.value == Blockchain.Tezos.id
        if (!ignoreAmountReduce && sendAmount == balance && isTezos) {
            add(
                SendNotification.Warning.HighFeeError(
                    amount = TEZOS_FEE_THRESHOLD.toPlainString(),
                    onConfirmClick = {
                        val reduceTo = sendAmount.minus(TEZOS_FEE_THRESHOLD).toPlainString()
                        clickIntents.onAmountReduceClick(reduceTo)
                    },
                    onDismissClick = clickIntents::onAmountReduceIgnoreClick,
                ),
            )
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
        )
        val balance = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
        if (dustValue != null && !balance.isNullOrZero() && receivedAmount < balance) {
            val totalAmount = feeAmount + receivedAmount
            val change = balance - totalAmount
            val isChangeLowerThanDust = change < dustValue && change != BigDecimal.ZERO
            val isShowWarning = totalAmount < dustValue || isChangeLowerThanDust
            if (isShowWarning) {
                add(
                    SendNotification.Error.MinimumAmountError(dustValue.toPlainString()),
                )
            }
        }
    }

    companion object {
        private const val CARDANO_MINIMUM = "1"
        private const val DOGECOIN_MINIMUM = "0.01"
        private val TEZOS_FEE_THRESHOLD = BigDecimal("0.01")
    }
}
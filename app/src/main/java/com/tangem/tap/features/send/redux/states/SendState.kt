package com.tangem.tap.features.send.redux.states

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.extensions.isZero
import com.tangem.tap.common.CurrencyConverter
import com.tangem.tap.common.entities.TapCurrency
import com.tangem.tap.features.send.redux.AmountAction
import com.tangem.tap.store
import org.rekotlin.StateType
import java.math.BigDecimal

/**
* [REDACTED_AUTHOR]
 */

interface IdStateHolder {
    val stateId: StateId
}

enum class StateId {
    SEND_SCREEN, ADDRESS_PAY_ID, AMOUNT, FEE, RECEIPT
}

interface SendScreenState : StateType, IdStateHolder

data class SendState(
        val amount: Amount? = null,
        val walletManager: WalletManager? = null,
        val currencyConverter: CurrencyConverter = CurrencyConverter(BigDecimal.ONE),
        val lastChangedStates: LinkedHashSet<StateId> = linkedSetOf(),
        val addressPayIdState: AddressPayIdState = AddressPayIdState(),
        val amountState: AmountState = AmountState(),
        val feeState: FeeState = FeeState(),
        val receiptState: ReceiptState = ReceiptState(),
        val sendButtonState: SendButtonState = SendButtonState.DISABLED,
        override val stateId: StateId = StateId.SEND_SCREEN
) : SendScreenState {

    fun isReadyToSend(): Boolean {
        val sendState = store.state.sendState
        return addressPayIdIsReady() && sendState.amountState.isReady() && sendState.feeState.isReady()
    }

    fun addressPayIdIsReady(): Boolean = store.state.sendState.addressPayIdState.isReady()

    fun getDecimals(type: MainCurrencyType): Int = when (type) {
        MainCurrencyType.FIAT -> 2
        MainCurrencyType.CRYPTO -> amount?.decimals ?: 0
    }

    fun getButtonState(): SendButtonState = if (isReadyToSend()) SendButtonState.ENABLED else SendButtonState.DISABLED
}

enum class SendButtonState {
    ENABLED, DISABLED, PROGRESS
}

data class AmountState(
        val viewAmountValue: String = BigDecimal.ZERO.toPlainString(),
        val viewBalanceValue: String = BigDecimal.ZERO.toPlainString(),
        val mainCurrency: Value<MainCurrencyType> = Value(MainCurrencyType.FIAT, TapCurrency.main),
        val typeOfAmount: AmountType = AmountType.Coin,
        val amountToSendCrypto: BigDecimal = BigDecimal.ZERO,
        val balanceCrypto: BigDecimal = BigDecimal.ZERO,
        val cursorAtTheSamePosition: Boolean = true,
        val maxLengthOfAmount: Int = 2,
        val error: AmountAction.Error? = null,
        override val stateId: StateId = StateId.AMOUNT
) : SendScreenState {
    fun isReady(): Boolean = error == null && !amountToSendCrypto.isZero()
}

enum class MainCurrencyType {
    FIAT, CRYPTO
}

data class Value<T>(
        val value: T,
        val displayedValue: String
)
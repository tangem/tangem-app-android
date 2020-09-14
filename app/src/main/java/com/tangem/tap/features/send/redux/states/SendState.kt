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
[REDACTED_AUTHOR]
 */
data class SendState(
        val amount: Amount? = null,
        val walletManager: WalletManager? = null,
        val currencyConverter: CurrencyConverter = CurrencyConverter(BigDecimal.ONE),
        val lastChangedStateType: StateType = NoneState(),
        val addressPayIdState: AddressPayIdState = AddressPayIdState(),
        val amountState: AmountState = AmountState(),
        val feeState: FeeState = FeeState(),
        val receiptState: ReceiptState = ReceiptState(),
        val sendButtonIsEnabled: Boolean = false,
) : StateType {

    fun isReadyToSend(): Boolean {
        val sendState = store.state.sendState
        return addressPayIdIsReady() && sendState.amountState.isReady() && sendState.feeState.isReady()
    }

    fun addressPayIdIsReady(): Boolean = store.state.sendState.addressPayIdState.isReady()
}

class NoneState : StateType

data class AmountState(
        val viewAmountValue: String = BigDecimal.ZERO.toPlainString(),
        val viewBalanceValue: String = BigDecimal.ZERO.toPlainString(),
        val mainCurrency: Value<MainCurrencyType> = Value(MainCurrencyType.FIAT, TapCurrency.main),
        val typeOfAmount: AmountType = AmountType.Coin,
        val amountToSendCrypto: BigDecimal = BigDecimal.ZERO,
        val balanceCrypto: BigDecimal = BigDecimal.ZERO,
        val cursorAtTheSamePosition: Boolean = true,
        val error: AmountAction.Error? = null
) : StateType {
    fun isReady(): Boolean = error == null && !amountToSendCrypto.isZero()
}

enum class MainCurrencyType {
    FIAT, CRYPTO
}

data class Value<T>(
        val value: T,
        val displayedValue: String
)
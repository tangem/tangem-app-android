package com.tangem.tap.features.send.redux.states

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.extensions.isZero
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.redux.StateDialog
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.tap.common.CurrencyConverter
import com.tangem.tap.common.entities.IndeterminateProgressButton
import com.tangem.tap.common.text.DecimalDigitsInputFilter
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.store
import org.rekotlin.StateType
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */

interface IdStateHolder {
    val stateId: StateId
}

enum class StateId {
    SEND_SCREEN, ADDRESS_PAY_ID, TRANSACTION_EXTRAS, AMOUNT, FEE, RECEIPT
}

interface SendScreenState : StateType, IdStateHolder

data class SendState(
    val walletManager: WalletManager? = null,
    val currency: CryptoCurrency? = null,
    val coinConverter: CurrencyConverter? = null,
    val tokenConverter: CurrencyConverter? = null,
    val customFeeConverter: CurrencyConverter? = null,
    val lastChangedStates: LinkedHashSet<StateId> = linkedSetOf(),
    val addressState: AddressState = AddressState(),
    val transactionExtrasState: TransactionExtrasState = TransactionExtrasState(),
    val amountState: AmountState = AmountState(),
    val feeState: FeeState = FeeState(),
    val receiptState: ReceiptState = ReceiptState(),
    val sendWarningsList: List<WarningMessage> = listOf(),
    val sendButtonState: IndeterminateProgressButton = IndeterminateProgressButton(ButtonState.DISABLED),
    val dialog: StateDialog? = null,
    val externalTransactionData: ExternalTransactionData? = null,
    val isSuccessSend: Boolean = false,
    val canIncludeFee: Boolean = false,
) : SendScreenState {

    override val stateId: StateId = StateId.SEND_SCREEN

    fun getDecimals(type: MainCurrencyType): Int = when (type) {
        MainCurrencyType.FIAT -> 2
        MainCurrencyType.CRYPTO -> amountState.amountToExtract?.decimals ?: 0
    }

    private fun convertFiatToCoin(value: BigDecimal): BigDecimal {
        return if (!this.coinIsConvertible()) value else coinConverter!!.toCrypto(value)
    }

    private fun convertFiatToToken(value: BigDecimal): BigDecimal {
        return if (!this.tokenIsConvertible()) value else tokenConverter!!.toCrypto(value)
    }

    private fun convertCoinToFiat(value: BigDecimal, scaleWithPrecision: Boolean = false): BigDecimal {
        if (!this.coinIsConvertible()) return value

        val converter = coinConverter!!
        return if (!scaleWithPrecision) converter.toFiat(value) else converter.toFiatWithPrecision(value)
    }

    private fun convertTokenToFiat(value: BigDecimal, scaleWithPrecision: Boolean = false): BigDecimal {
        if (!this.tokenIsConvertible()) return value

        val converter = tokenConverter!!
        return if (!scaleWithPrecision) converter.toFiat(value) else converter.toFiatWithPrecision(value)
    }

    fun convertFiatToExtractCrypto(fiatValue: BigDecimal): BigDecimal = when (amountState.typeOfAmount) {
        AmountType.Coin -> convertFiatToCoin(fiatValue)
        is AmountType.Token -> convertFiatToToken(fiatValue)
        is AmountType.FeeResource,
        AmountType.Reserve,
        -> fiatValue
    }

    fun convertExtractCryptoToFiat(cryptoValue: BigDecimal, scaleWithPrecision: Boolean = false): BigDecimal {
        return when (amountState.typeOfAmount) {
            AmountType.Coin -> convertCoinToFiat(cryptoValue, scaleWithPrecision)
            is AmountType.Token -> convertTokenToFiat(cryptoValue, scaleWithPrecision)
            is AmountType.FeeResource,
            AmountType.Reserve,
            -> cryptoValue
        }
    }

    fun getButtonState(): ButtonState = if (isReadyToSend()) ButtonState.ENABLED else ButtonState.DISABLED

    fun getTotalAmountToSend(value: BigDecimal = amountState.amountToSendCrypto): BigDecimal {
        val needToExtractFee = amountState.canIncludeFee() && feeState.feeIsIncluded
        return if (needToExtractFee) value.minus(feeState.getCurrentFeeValue()) else value
    }

    fun coinIsConvertible(): Boolean = coinConverter != null
    fun tokenIsConvertible(): Boolean = tokenConverter != null
    fun feeIsConvertible(): Boolean = customFeeConverter != null

    fun mainCurrencyCanBeSwitched(): Boolean {
        return when (amountState.typeOfAmount) {
            AmountType.Coin -> coinIsConvertible()
            is AmountType.Token -> tokenIsConvertible()
            is AmountType.FeeResource,
            AmountType.Reserve,
            -> false
        }
    }

    companion object {
        private fun addressIsReady(): Boolean = store.state.sendState.addressState.isReady()

        private fun amountIsReady(): Boolean = store.state.sendState.amountState.isReady()

        fun isReadyToRequestFee(): Boolean = addressIsReady() && amountIsReady()

        fun isReadyToSend(): Boolean = addressIsReady() && amountIsReady() &&
            store.state.sendState.feeState.isReady()
    }
}

enum class ButtonState {
    ENABLED, DISABLED, PROGRESS
}

data class AmountState(
    val amountToExtract: Amount? = null,
    val typeOfAmount: AmountType = AmountType.Coin,
    val viewAmountValue: InputViewValue = InputViewValue(BigDecimal.ZERO.toPlainString()),
    val viewBalanceValue: String = BigDecimal.ZERO.toPlainString(),
    val mainCurrency: MainCurrency = MainCurrency(MainCurrencyType.FIAT, AppCurrency.Default.code),
    val amountToSendCrypto: BigDecimal = BigDecimal.ZERO,
    val balanceCrypto: BigDecimal = BigDecimal.ZERO,
    val hideBalance: Boolean = false,
    val cursorAtTheSamePosition: Boolean = true,
    val maxLengthOfAmount: Int = 2,
    val decimalSeparator: String = ".",
    val error: TapError? = null,
    val inputIsEnabled: Boolean = true,
) : SendScreenState {

    override val stateId: StateId = StateId.AMOUNT

    fun isReady(): Boolean = error == null && !amountToSendCrypto.isZero()

    fun canIncludeFee(): Boolean = store.state.sendState.canIncludeFee

    fun createMainCurrency(type: MainCurrencyType, canSwitched: Boolean): MainCurrency {
        return if (!canSwitched) {
            MainCurrency(type, amountToExtract?.currencySymbol ?: "NONE", false)
        } else {
            when (type) {
                MainCurrencyType.FIAT -> MainCurrency(type, store.state.globalState.appCurrency.code)
                MainCurrencyType.CRYPTO -> MainCurrency(type, amountToExtract?.currencySymbol ?: "NONE")
            }
        }
    }

    fun toBigDecimalSeparator(value: String): String {
        return value.replace(",", ".")
    }

    fun restoreDecimalSeparator(value: String): String {
        return DecimalDigitsInputFilter.setDecimalSeparator(value, decimalSeparator)
    }
}

data class InputViewValue(val value: String, val isFromUserInput: Boolean = false)
enum class MainCurrencyType {
    FIAT, CRYPTO
}

data class MainCurrency(
    val type: MainCurrencyType,
    val currencySymbol: String,
    val isEnabled: Boolean = true,
)

data class ExternalTransactionData(
    val amount: String,
    val destinationAddress: String,
    val transactionId: String,
    val canAmountBeModified: Boolean = false,
    val canDestinationAddressBeModified: Boolean = false,
)
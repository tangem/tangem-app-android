package com.tangem.tap.features.send.redux.states

import androidx.core.text.isDigitsOnly
import com.tangem.blockchain.blockchains.stellar.StellarMemo
import com.tangem.tap.features.send.redux.AddressVerifyAction
import java.math.BigInteger

data class AddressState(
    val viewFieldValue: InputViewValue = InputViewValue(""),
    val normalFieldValue: String? = null,
    val truncatedFieldValue: String? = null,
    val destinationWalletAddress: String? = null,
    val error: AddressVerifyAction.Error? = null,
    val truncateHandler: ((String) -> String)? = null,
    val pasteIsEnabled: Boolean = false,
    val inputIsEnabled: Boolean = true,
) : SendScreenState {

    override val stateId: StateId = StateId.ADDRESS_PAY_ID

    fun truncate(value: String): String = truncateHandler?.invoke(value) ?: value

    fun isReady(): Boolean = error == null && destinationWalletAddress?.isNotEmpty() ?: false
}

data class TransactionExtrasState(
    val xlmMemo: XlmMemoState? = null,
    val binanceMemo: BinanceMemoState? = null,
    val xrpDestinationTag: XrpDestinationTagState? = null,
    val tonMemoState: TonMemoState? = null,
    val cosmosMemoState: CosmosMemoState? = null,
) : IdStateHolder {
    override val stateId: StateId = StateId.TRANSACTION_EXTRAS

    fun isNull(): Boolean {
        return xlmMemo == null && binanceMemo == null && xrpDestinationTag == null && tonMemoState == null &&
            cosmosMemoState == null
    }

    fun isEmpty(): Boolean {
        val isXlmEmpty = xlmMemo?.viewFieldValue?.value?.isEmpty() ?: false
        val isBinanceEmpty = binanceMemo?.viewFieldValue?.value?.isEmpty() ?: false
        val isXrpEmpty = xrpDestinationTag?.viewFieldValue?.value?.isEmpty() ?: false
        val isTonEmpty = tonMemoState?.viewFieldValue?.value?.isEmpty() ?: false
        val isCosmosEmpty = cosmosMemoState?.viewFieldValue?.value?.isEmpty() ?: false

        return isXlmEmpty || isBinanceEmpty || isXrpEmpty || isTonEmpty || isCosmosEmpty
    }
}

enum class XlmMemoType {
    TEXT, ID
}

data class XlmMemoState(
    val viewFieldValue: InputViewValue = InputViewValue(""),
    val text: StellarMemo.Text? = null,
    val id: StellarMemo.Id? = null,
    val error: TransactionExtraError? = null,
) {
    val memo: StellarMemo?
        get() = when (selectedMemoType) {
            XlmMemoType.TEXT -> text
            XlmMemoType.ID -> id
        }

    val selectedMemoType: XlmMemoType
        get() = determineMemoType(viewFieldValue.value)

    companion object {
        fun determineMemoType(value: String): XlmMemoType = when {
            value.isNotEmpty() && value.isDigitsOnly() -> XlmMemoType.ID
            else -> XlmMemoType.TEXT
        }

        @Suppress("MagicNumber")
        fun isAssignableValue(value: String): Boolean = when (determineMemoType(value)) {
            XlmMemoType.TEXT -> {
                // from org.stellar.sdk.MemoText
                value.toByteArray().size <= 28
            }
            XlmMemoType.ID -> {
                try {
                    // from com.tangem.blockchain.blockchains.stellar.StellarMemo.toStellarSdkMemo
                    value.toBigInteger() in BigInteger.ZERO..Long.MAX_VALUE.toBigInteger() * 2.toBigInteger()
                } catch (ex: NumberFormatException) {
                    false
                }
            }
        }
    }
}

data class BinanceMemoState(
    val viewFieldValue: InputViewValue = InputViewValue(""),
    val memo: BigInteger? = null,
    val error: TransactionExtraError? = null,
)

// tag must contains only digits
data class XrpDestinationTagState(
    val viewFieldValue: InputViewValue = InputViewValue(""),
    val tag: Long? = null,
    val error: TransactionExtraError? = null,
) {
    companion object {
        const val MAX_NUMBER: Long = 4294967295
    }
}

data class TonMemoState(
    val viewFieldValue: InputViewValue = InputViewValue(""),
    val memo: String? = null,
    val error: TransactionExtraError? = null,
)

data class CosmosMemoState(
    val viewFieldValue: InputViewValue = InputViewValue(""),
    val memo: String? = null,
    val error: TransactionExtraError? = null,
)

enum class TransactionExtraError {
    INVALID_DESTINATION_TAG,
    INVALID_XLM_MEMO,
    INVALID_BINANCE_MEMO,
}
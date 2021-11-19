package com.tangem.tap.features.send.redux.states

import com.tangem.blockchain.blockchains.stellar.StellarMemo
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction
import java.math.BigInteger

data class AddressPayIdState(
        val viewFieldValue: InputViewValue = InputViewValue(""),
        val normalFieldValue: String? = null,
        val truncatedFieldValue: String? = null,
        val destinationWalletAddress: String? = null,
        val error: AddressPayIdVerifyAction.Error? = null,
        val truncateHandler: ((String) -> String)? = null,
        val sendingToPayIdEnabled: Boolean = false,
        val pasteIsEnabled: Boolean = false,
        val inputIsEnabled: Boolean = true
) : SendScreenState {

    override val stateId: StateId = StateId.ADDRESS_PAY_ID

    fun truncate(value: String): String = truncateHandler?.invoke(value) ?: value

    fun isReady(): Boolean = error == null && destinationWalletAddress?.isNotEmpty() ?: false

    fun isPayIdState(): Boolean = destinationWalletAddress != null && destinationWalletAddress != normalFieldValue
}

data class TransactionExtrasState(
        val xlmMemo: XlmMemoState? = null,
        val binanceMemo: BinanceMemoState? = null,
        val xrpDestinationTag: XrpDestinationTagState? = null
) : IdStateHolder {
    override val stateId: StateId = StateId.TRANSACTION_EXTRAS
}

enum class XlmMemoType {
    TEXT, ID
}

data class XlmMemoState(
        val viewFieldValue: InputViewValue = InputViewValue(""),
        val selectedMemoType: XlmMemoType = XlmMemoType.ID,
        val text: StellarMemo.Text? = null,
        val id: StellarMemo.Id? = null,
        val error: TransactionExtraError? = null,
) {
    val memo: StellarMemo?
        get() = when (selectedMemoType) {
            XlmMemoType.TEXT -> text
            XlmMemoType.ID -> id
        }

    companion object {
        val MAX_NUMBER: BigInteger = BigInteger("FFFFFFFFFFFFFFFF", 16)
    }
}

data class BinanceMemoState(
    val viewFieldValue: InputViewValue = InputViewValue(""),
    val memo: BigInteger? = null,
    val error: TransactionExtraError? = null
    ) {
    companion object {
        val MAX_NUMBER: BigInteger = BigInteger("FFFFFFFFFFFFFFFF", 16)
    }
}

// tag must contains only digits
data class XrpDestinationTagState(
        val viewFieldValue: InputViewValue = InputViewValue(""),
        val tag: Long? = null,
        val error: TransactionExtraError? = null
) {
    companion object {
        const val MAX_NUMBER: Long = 4294967295
    }
}

enum class TransactionExtraError {
    INVALID_DESTINATION_TAG,
    INVALID_XLM_MEMO,
    INVALID_BINANCE_MEMO
}
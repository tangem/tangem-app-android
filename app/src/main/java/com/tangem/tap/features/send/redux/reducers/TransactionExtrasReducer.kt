package com.tangem.tap.features.send.redux.reducers

import com.tangem.blockchain.blockchains.stellar.StellarMemo
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.TransactionExtrasAction.*
import com.tangem.tap.features.send.redux.states.*

/**
[REDACTED_AUTHOR]
 */
class TransactionExtrasReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState {
        return when (action) {
            is Prepare -> handleInitialization(action, sendState)
            Release -> handleRelease(action, sendState)
            is XlmMemo -> handleMemo(action, sendState, sendState.transactionExtrasState)
            is XrpDestinationTag -> handleXrpTag(action, sendState, sendState.transactionExtrasState)
            else -> sendState
        }
    }

    private fun handleInitialization(action: Prepare, sendState: SendState): SendState {
        val emptyResult = TransactionExtrasState()
        val result = when (action.blockchain) {
            Blockchain.XRP -> {
                // 'r' - without tag, 'x' - with tag
                if (action.walletAddress.startsWith("r", true)) {
                    val tag = action.xrpTag?.toLongOrNull()
                    if (tag == null) {
                        TransactionExtrasState(xrpDestinationTag = XrpDestinationTagState())
                    } else {
                        TransactionExtrasState(xrpDestinationTag = XrpDestinationTagState(
                                InputViewValue("$tag", false), tag)
                        )
                    }
                } else {
                    emptyResult
                }
            }
            Blockchain.Stellar -> TransactionExtrasState(xlmMemo = XlmMemoState())
            else -> emptyResult
        }
        return updateLastState(sendState.copy(transactionExtrasState = result), result)
    }

    private fun handleRelease(action: SendScreenAction, sendState: SendState): SendState {
        val result = TransactionExtrasState()
        return updateLastState(sendState.copy(transactionExtrasState = result), result)
    }

    private fun handleMemo(
            action: XlmMemo,
            sendState: SendState,
            infoState: TransactionExtrasState,
    ): SendState {
        fun clearMemo(memo: XlmMemoState): XlmMemoState = memo.copy(text = null, id = null, error = null)

        val result = when (action) {
//            is XlmMemo.ChangeSelectedMemo -> {
//                val inputViewValue = InputViewValue("", false)
//                val memo = infoState.xlmMemo?.copy(
//                        viewFieldValue = inputViewValue,
//                        selectedMemoType = action.memoType,
//                ) ?: XlmMemoState(inputViewValue, action.memoType)
//
//                infoState.copy(xlmMemo = clearMemo(memo))
//            }
            is XlmMemo.HandleUserInput -> {
                val inputViewValue = InputViewValue(action.data, true)
                var memo = infoState.xlmMemo?.copy(viewFieldValue = inputViewValue)
                        ?: XlmMemoState(inputViewValue)
                memo = clearMemo(memo)
                memo = when (infoState.xlmMemo?.selectedMemoType) {
                    XlmMemoType.TEXT -> memo.copy(text = StellarMemo.Text(action.data))
                    XlmMemoType.ID -> {
                        val id = action.data.toBigIntegerOrNull()
                        if (id != null) {
                            if (id > XlmMemoState.MAX_NUMBER) {
                                memo.copy(error = TransactionExtraError.INVALID_XLM_MEMO)
                            } else {
                                memo.copy(id = StellarMemo.Id(id))
                            }
                        } else {
                            memo
                        }
                    }
                    null -> memo
                }
                infoState.copy(xlmMemo = memo)
            }
        }
        return updateLastState(sendState.copy(transactionExtrasState = result), result)
    }

    private fun handleXrpTag(
            action: XrpDestinationTag,
            sendState: SendState,
            infoState: TransactionExtrasState,
    ): SendState {
        val result = when (action) {
            is XrpDestinationTag.HandleUserInput -> {
                val tag = action.data.toLongOrNull()
                if (tag != null) {
                    val input = InputViewValue(action.data, true)
                    val tagState = if (tag <= XrpDestinationTagState.MAX_NUMBER){
                        XrpDestinationTagState(input, tag)
                    } else {
                        XrpDestinationTagState(input, error = TransactionExtraError.INVALID_DESTINATION_TAG)
                    }
                    infoState.copy(xrpDestinationTag = tagState)
                } else {
                    infoState
                }
            }
        }
        return updateLastState(sendState.copy(transactionExtrasState = result), result)
    }
}
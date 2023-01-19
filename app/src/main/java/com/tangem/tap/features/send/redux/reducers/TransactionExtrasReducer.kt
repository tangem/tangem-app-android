package com.tangem.tap.features.send.redux.reducers

import com.tangem.blockchain.blockchains.stellar.StellarMemo
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.TransactionExtrasAction.BinanceMemo
import com.tangem.tap.features.send.redux.TransactionExtrasAction.Prepare
import com.tangem.tap.features.send.redux.TransactionExtrasAction.Release
import com.tangem.tap.features.send.redux.TransactionExtrasAction.XlmMemo
import com.tangem.tap.features.send.redux.TransactionExtrasAction.XrpDestinationTag
import com.tangem.tap.features.send.redux.states.BinanceMemoState
import com.tangem.tap.features.send.redux.states.InputViewValue
import com.tangem.tap.features.send.redux.states.SendState
import com.tangem.tap.features.send.redux.states.TransactionExtraError
import com.tangem.tap.features.send.redux.states.TransactionExtrasState
import com.tangem.tap.features.send.redux.states.XlmMemoState
import com.tangem.tap.features.send.redux.states.XlmMemoType
import com.tangem.tap.features.send.redux.states.XrpDestinationTagState

/**
 * Created by Anton Zhilenkov on 16/12/2020.
 */
class TransactionExtrasReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState {
        return when (action) {
            is Prepare -> handleInitialization(action, sendState)
            Release -> handleRelease(sendState)
            is XlmMemo -> handleXlmMemo(action, sendState, sendState.transactionExtrasState)
            is BinanceMemo -> handleBinanceMemo(action, sendState, sendState.transactionExtrasState)
            is XrpDestinationTag -> handleXrpTag(action, sendState, sendState.transactionExtrasState)
            else -> sendState
        }
    }

    private fun handleInitialization(action: Prepare, sendState: SendState): SendState {
        val emptyResult = TransactionExtrasState()
        val result = when (action.blockchain) {
            Blockchain.XRP -> {
                val address = action.walletAddress.substringAfter(":")
                // 'r' - without tag, 'x' - with tag
                if (address.startsWith("r", true)) {
                    val tag = action.xrpTag?.toLongOrNull()
                    if (tag == null) {
                        TransactionExtrasState(xrpDestinationTag = XrpDestinationTagState())
                    } else {
                        TransactionExtrasState(
                            xrpDestinationTag = XrpDestinationTagState(
                                viewFieldValue = InputViewValue("$tag", false),
                                tag = tag,
                            ),
                        )
                    }
                } else {
                    emptyResult
                }
            }
            Blockchain.Stellar -> TransactionExtrasState(xlmMemo = XlmMemoState())
            Blockchain.Binance -> TransactionExtrasState(binanceMemo = BinanceMemoState())
            else -> emptyResult
        }
        return updateLastState(sendState.copy(transactionExtrasState = result), result)
    }

    private fun handleRelease(sendState: SendState): SendState {
        val result = TransactionExtrasState()
        return updateLastState(sendState.copy(transactionExtrasState = result), result)
    }

    private fun handleXlmMemo(
        action: XlmMemo,
        sendState: SendState,
        infoState: TransactionExtrasState,
    ): SendState {
        fun clearMemo(memo: XlmMemoState): XlmMemoState = memo.copy(text = null, id = null, error = null)

        val result = when (action) {
            is XlmMemo.HandleUserInput -> {
                val inputViewValue = InputViewValue(action.data, true)
                var memo = infoState.xlmMemo?.copy(viewFieldValue = inputViewValue)
                    ?: XlmMemoState(inputViewValue)
                memo = clearMemo(memo)
                memo = when (memo.selectedMemoType) {
                    XlmMemoType.TEXT -> {
                        if (XlmMemoState.isAssignableValue(action.data)) {
                            memo.copy(text = StellarMemo.Text(action.data))
                        } else {
                            memo.copy(error = TransactionExtraError.INVALID_XLM_MEMO)
                        }
                    }
                    XlmMemoType.ID -> {
                        if (XlmMemoState.isAssignableValue(action.data)) {
                            memo.copy(id = StellarMemo.Id(action.data.toBigInteger()))
                        } else {
                            memo.copy(error = TransactionExtraError.INVALID_XLM_MEMO)
                        }
                    }
                }
                infoState.copy(xlmMemo = memo)
            }
        }
        return updateLastState(sendState.copy(transactionExtrasState = result), result)
    }

    private fun handleBinanceMemo(
        action: BinanceMemo,
        sendState: SendState,
        infoState: TransactionExtrasState,
    ): SendState {
        val result = when (action) {
            is BinanceMemo.HandleUserInput -> {
                val tag = action.data.toBigIntegerOrNull()
                if (tag != null) {
                    val input = InputViewValue(action.data, true)
                    val tagState = BinanceMemoState(input, tag)
                    infoState.copy(binanceMemo = tagState)
                } else {
                    infoState
                }
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
                    val tagState = if (tag <= XrpDestinationTagState.MAX_NUMBER) {
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

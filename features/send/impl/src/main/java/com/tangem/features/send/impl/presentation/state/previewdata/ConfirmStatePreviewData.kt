package com.tangem.features.send.impl.presentation.state.previewdata

import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiStateType
import kotlinx.collections.immutable.persistentListOf

internal object ConfirmStatePreviewData {

    val sendState = SendStates.SendState(
        type = SendUiStateType.Send,
        isSending = false,
        isSuccess = false,
        transactionDate = 0L,
        txUrl = "",
        ignoreAmountReduce = false,
        reduceAmountBy = null,
        isFromConfirmation = false,
        showTapHelp = true,
        notifications = persistentListOf(),
    )

    val sendDoneState = SendStates.SendState(
        type = SendUiStateType.Send,
        isSending = false,
        isSuccess = true,
        transactionDate = 1695199500000L,
        txUrl = "url",
        ignoreAmountReduce = false,
        reduceAmountBy = null,
        isFromConfirmation = false,
        showTapHelp = false,
        notifications = persistentListOf(),
    )
}
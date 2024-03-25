package com.tangem.features.send.impl.presentation.state.confirm

import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class SendConfirmStateConverter(
    private val isTapHelpPreviewEnabledProvider: Provider<Boolean>,
) : Converter<Unit, SendStates.SendState> {
    override fun convert(value: Unit): SendStates.SendState {
        return SendStates.SendState(
            isPrimaryButtonEnabled = true,
            isSending = false,
            isSuccess = false,
            isSubtract = false,
            transactionDate = 0L,
            txUrl = "",
            ignoreAmountReduce = false,
            isFromConfirmation = true,
            showTapHelp = isTapHelpPreviewEnabledProvider(),
            notifications = persistentListOf(),
        )
    }
}
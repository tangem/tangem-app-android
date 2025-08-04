package com.tangem.features.walletconnect.transaction.entity.common

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.ToastMessage
import kotlinx.coroutines.flow.StateFlow

internal interface WcCommonTransactionModel {

    val uiState: StateFlow<WcCommonTransactionUM?>

    val messageSender: UiMessageSender

    fun dismiss()

    fun popBack()

    // TODO: [REDACTED_JIRA]
    fun showSuccessSignMessage(message: TextReference = stringReference("Successfully signed")) {
        messageSender.send(ToastMessage(message = message))
    }
}
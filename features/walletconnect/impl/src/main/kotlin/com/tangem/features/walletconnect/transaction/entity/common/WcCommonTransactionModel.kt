package com.tangem.features.walletconnect.transaction.entity.common

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.ToastMessage
import com.tangem.features.walletconnect.impl.R
import kotlinx.coroutines.flow.StateFlow

internal interface WcCommonTransactionModel {

    val uiState: StateFlow<WcCommonTransactionUM?>

    val messageSender: UiMessageSender

    fun dismiss()

    fun popBack()

    fun showSuccessSignMessage(message: TextReference = resourceReference(R.string.wc_successfully_signed)) {
        messageSender.send(ToastMessage(message = message))
    }
}
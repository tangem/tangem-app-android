package com.tangem.common.ui.alerts

import com.tangem.common.ui.alerts.models.AlertDemoModeUM
import com.tangem.common.ui.alerts.models.AlertTransactionErrorUM
import com.tangem.common.ui.alerts.models.AlertUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.utils.converter.Converter

class TransactionErrorAlertConverter(
    private val popBackStack: () -> Unit,
    private val onFailedTxEmailClick: (String) -> Unit,
) : Converter<SendTransactionError, AlertUM?> {
    override fun convert(value: SendTransactionError): AlertUM? {
        return when (value) {
            is SendTransactionError.DemoCardError -> AlertDemoModeUM(
                onConfirmClick = popBackStack,
            )
            is SendTransactionError.TangemSdkError -> AlertTransactionErrorUM(
                code = value.code.toString(),
                cause = null,
                causeTextReference = resourceReference(value.messageRes, wrappedList(value.args)),
                onConfirmClick = { onFailedTxEmailClick(value.code.toString()) },
            )
            is SendTransactionError.BlockchainSdkError -> AlertTransactionErrorUM(
                code = value.code.toString(),
                cause = value.message,
                onConfirmClick = { onFailedTxEmailClick("${value.code}: ${value.message.orEmpty()}") },
            )
            is SendTransactionError.DataError -> AlertTransactionErrorUM(
                code = "",
                cause = value.message,
                onConfirmClick = { onFailedTxEmailClick(value.message.orEmpty()) },
            )
            is SendTransactionError.NetworkError -> AlertTransactionErrorUM(
                code = value.code.orEmpty(),
                cause = value.message.orEmpty(),
                onConfirmClick = { onFailedTxEmailClick(value.message.orEmpty()) },
            )
            is SendTransactionError.UnknownError -> AlertTransactionErrorUM(
                code = "",
                cause = value.ex?.localizedMessage,
                onConfirmClick = { onFailedTxEmailClick(value.ex?.localizedMessage.orEmpty()) },
            )
            else -> null
        }
    }
}
package com.tangem.features.send.impl.presentation.state

import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.converter.Converter

internal class SendTransactionAlertConverter(
    private val clickIntents: SendClickIntents,
) : Converter<SendTransactionError, SendAlertState?> {
    override fun convert(value: SendTransactionError): SendAlertState? {
        return when (value) {
            SendTransactionError.DemoCardError -> SendAlertState.DemoMode(
                onConfirmClick = { clickIntents.popBackStack() },
            )
            is SendTransactionError.TangemSdkError -> SendAlertState.TransactionError(
                code = value.code.toString(),
                cause = null,
                causeTextReference = value.messageReference,
                onConfirmClick = { clickIntents.onFailedTxEmailClick(value.code.toString()) },
            )
            is SendTransactionError.BlockchainSdkError -> SendAlertState.TransactionError(
                code = value.code.toString(),
                cause = value.message,
                onConfirmClick = { clickIntents.onFailedTxEmailClick("${value.code}: ${value.message.orEmpty()}") },
            )
            is SendTransactionError.DataError -> SendAlertState.TransactionError(
                code = "",
                cause = value.message,
                onConfirmClick = { clickIntents.onFailedTxEmailClick(value.message.orEmpty()) },
            )
            is SendTransactionError.NetworkError -> SendAlertState.TransactionError(
                code = value.code.orEmpty(),
                cause = value.message.orEmpty(),
                onConfirmClick = { clickIntents.onFailedTxEmailClick(value.message.orEmpty()) },
            )
            is SendTransactionError.UnknownError -> SendAlertState.TransactionError(
                code = "",
                cause = value.ex?.localizedMessage,
                onConfirmClick = { clickIntents.onFailedTxEmailClick(value.ex?.localizedMessage.orEmpty()) },
            )
            else -> null
        }
    }
}
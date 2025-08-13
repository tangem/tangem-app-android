package com.tangem.domain.walletconnect.error

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.core.TangemError
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.parseWrappedError
import com.tangem.domain.walletconnect.model.WcRequestError

fun parseSendError(error: SendTransactionError): WcRequestError.WrappedSendError {
    return WcRequestError.WrappedSendError(error)
}

fun parseTangemSdkError(error: TangemError): WcRequestError.WrappedSendError {
    val sendError = parseWrappedError(BlockchainSdkError.WrappedTangemError(error))
    return parseSendError(sendError)
}
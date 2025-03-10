package com.tangem.domain.transaction.error

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.common.core.TangemSdkError
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.transaction.R
import com.tangem.domain.transaction.error.SendTransactionError.Companion.USER_CANCELLED_ERROR_CODE
import com.tangem.sdk.extensions.localizedDescriptionRes

fun Result.Failure.mapToFeeError(): GetFeeError {
    return when (this.error) {
        is BlockchainSdkError.Tron.AccountActivationError -> {
            GetFeeError.BlockchainErrors.TronActivationError
        }
        is BlockchainSdkError.Kaspa.ZeroUtxoError -> {
            GetFeeError.BlockchainErrors.KaspaZeroUtxo
        }
        is BlockchainSdkError.Sui.OneSuiRequired -> {
            GetFeeError.BlockchainErrors.SuiOneCoinRequired
        }
        else -> GetFeeError.DataError(this.error)
    }
}

fun parseWrappedError(error: BlockchainSdkError.WrappedTangemError): SendTransactionError {
    return if (error.code == USER_CANCELLED_ERROR_CODE) {
        SendTransactionError.UserCancelledError
    } else {
        when (val tangemError = error.tangemError) {
            is TangemSdkError -> {
                val resource = tangemError.localizedDescriptionRes()
                val resId = resource.resId ?: R.string.common_unknown_error
                val resArgs = resource.args.map { it.value }
                SendTransactionError.TangemSdkError(tangemError.code, resId, wrappedList(resArgs))
            }
            is BlockchainSdkError.WrappedTangemError -> {
                parseWrappedError(tangemError) // todo remove when sdk errors are revised
            }
            is BlockchainSdkError.WrappedThrowable -> {
                val causeError = tangemError.cause
                if (causeError is BlockchainSdkError) {
                    SendTransactionError.BlockchainSdkError(causeError.code, causeError.customMessage)
                } else {
                    SendTransactionError.BlockchainSdkError(tangemError.code, tangemError.customMessage)
                }
            }
            else -> {
                SendTransactionError.BlockchainSdkError(error.code, tangemError.customMessage)
            }
        }
    }
}
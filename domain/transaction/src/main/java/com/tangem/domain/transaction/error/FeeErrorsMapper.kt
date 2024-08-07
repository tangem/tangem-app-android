package com.tangem.domain.transaction.error

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result

fun Result.Failure.mapToFeeError(): GetFeeError {
    return when (this.error) {
        is BlockchainSdkError.Tron.AccountActivationError -> {
            GetFeeError.BlockchainErrors.TronActivationError
        }
        else -> GetFeeError.DataError(this.error)
    }
}
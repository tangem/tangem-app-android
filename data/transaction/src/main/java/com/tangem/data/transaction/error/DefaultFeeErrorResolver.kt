package com.tangem.data.transaction.error

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.domain.transaction.error.FeeErrorResolver
import com.tangem.domain.transaction.error.GetFeeError

internal class DefaultFeeErrorResolver : FeeErrorResolver {
    override fun resolve(throwable: Throwable): GetFeeError {
        return when (throwable) {
            is BlockchainSdkError.Tron.AccountActivationError -> {
                GetFeeError.BlockchainErrors.TronActivationError
            }
            is BlockchainSdkError.Kaspa.ZeroUtxoError -> {
                GetFeeError.BlockchainErrors.KaspaZeroUtxo
            }
            is BlockchainSdkError.Sui.OneSuiRequired -> {
                GetFeeError.BlockchainErrors.SuiOneCoinRequired
            }
            else -> GetFeeError.DataError(throwable)
        }
    }
}
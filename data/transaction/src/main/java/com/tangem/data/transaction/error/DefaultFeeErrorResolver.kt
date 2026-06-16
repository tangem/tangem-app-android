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
            is BlockchainSdkError.Ethereum.EstimateOverrideError -> {
                GetFeeError.EstimateOverrideError(
                    blockchain = throwable.blockchain,
                    tokenSymbol = throwable.tokenSymbol,
                    rpcProvider = throwable.rpcProvider,
                    error = throwable.underlyingError,
                )
            }
            else -> GetFeeError.DataError(throwable)
        }
    }
}
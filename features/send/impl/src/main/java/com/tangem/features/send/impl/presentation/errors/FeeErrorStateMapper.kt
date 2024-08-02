package com.tangem.features.send.impl.presentation.errors

import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.impl.presentation.state.fee.FeeSelectorState

internal class FeeErrorStateMapper {

    fun getFeeError(loadFeeError: GetFeeError?, tokenName: String): FeeSelectorState.Error {
        return when (loadFeeError) {
            GetFeeError.BlockchainErrors.TronActivationError -> FeeSelectorState.Error.TronAccountActivationError(
                tokenName,
            )
            is GetFeeError.DataError,
            GetFeeError.UnknownError,
            null,
            -> FeeSelectorState.Error.NetworkError
        }
    }
}
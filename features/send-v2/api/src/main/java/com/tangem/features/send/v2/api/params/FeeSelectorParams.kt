package com.tangem.features.send.v2.api.params

import arrow.core.Either
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError

sealed class FeeSelectorParams {
    abstract val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>
    abstract val network: Network
    abstract val cryptoCurrencyStatus: CryptoCurrencyStatus

    data class FeeSelectorBlockParams(
        override val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
        override val network: Network,
        override val cryptoCurrencyStatus: CryptoCurrencyStatus,
        val suggestedFeeState: SuggestedFeeState,
    ) : FeeSelectorParams()

    sealed class SuggestedFeeState {
        data object None : SuggestedFeeState()
        data class Suggestion(val title: TextReference, val fee: Fee) : SuggestedFeeState()
    }
}
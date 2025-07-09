package com.tangem.features.send.v2.api.params

import arrow.core.Either
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.v2.api.callbacks.FeeSelectorModelCallback
import com.tangem.features.send.v2.api.entity.FeeSelectorUM

sealed class FeeSelectorParams {
    abstract val state: FeeSelectorUM
    abstract val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>
    abstract val cryptoCurrencyStatus: CryptoCurrencyStatus
    abstract val suggestedFeeState: SuggestedFeeState
    abstract val feeDisplaySource: FeeDisplaySource

    data class FeeSelectorBlockParams(
        override val state: FeeSelectorUM,
        override val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
        override val cryptoCurrencyStatus: CryptoCurrencyStatus,
        override val suggestedFeeState: SuggestedFeeState,
        override val feeDisplaySource: FeeDisplaySource,
    ) : FeeSelectorParams()

    data class FeeSelectorDetailsParams(
        override val state: FeeSelectorUM,
        override val onLoadFee: suspend () -> Either<GetFeeError, TransactionFee>,
        override val cryptoCurrencyStatus: CryptoCurrencyStatus,
        override val suggestedFeeState: SuggestedFeeState,
        override val feeDisplaySource: FeeDisplaySource,
        val callback: FeeSelectorModelCallback,
    ) : FeeSelectorParams()

    sealed class SuggestedFeeState {
        data object None : SuggestedFeeState()
        data class Suggestion(val title: TextReference, val fee: Fee) : SuggestedFeeState()
    }

    enum class FeeDisplaySource {
        Screen,
        BottomSheet,
    }
}
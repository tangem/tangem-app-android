package com.tangem.features.staking.impl.presentation.state

import com.tangem.blockchain.common.transaction.TransactionFee

internal sealed class InnerFeeState {

    data class Content(
        val fees: TransactionFee,
    ) : InnerFeeState()

    data object Loading : InnerFeeState()

    data object Error : InnerFeeState()
}

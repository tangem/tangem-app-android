package com.tangem.features.walletconnect.transaction.entity.common

import com.tangem.blockchain.common.transaction.Fee

internal sealed class WcTransactionFeeState {
    data class Success(val dAppFee: Fee?, val onClick: () -> Unit) : WcTransactionFeeState()
    data object None : WcTransactionFeeState()
}
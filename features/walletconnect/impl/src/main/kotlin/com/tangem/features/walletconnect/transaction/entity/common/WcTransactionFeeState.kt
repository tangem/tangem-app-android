package com.tangem.features.walletconnect.transaction.entity.common

import com.tangem.blockchain.common.transaction.Fee

internal sealed class WcTransactionFeeState {
    object Loading : WcTransactionFeeState()
    data class Success(val fee: Fee) : WcTransactionFeeState()
    data object None : WcTransactionFeeState()
}
package com.tangem.data.walletconnect.network.bitcoin

import com.tangem.blockchain.common.transaction.Fee

sealed interface WcBitcoinTxAction {

    data class UpdateFee(val fee: Fee) : WcBitcoinTxAction
}
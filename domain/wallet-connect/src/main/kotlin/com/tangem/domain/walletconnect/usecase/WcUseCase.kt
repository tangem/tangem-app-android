package com.tangem.domain.walletconnect.usecase

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest

interface WcUseCase

interface WcMethodUseCase : WcUseCase {
    val session: WcSession
    val rawSdkRequest: WcSdkSessionRequest
    val network: Network
}

interface WcMutableFee {
    fun updateFee(fee: Fee)
}
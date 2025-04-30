package com.tangem.domain.walletconnect

import com.tangem.domain.walletconnect.model.WcPairRequest
import kotlinx.coroutines.flow.Flow

interface WcPairService {
    val pairFlow: Flow<WcPairRequest>

    fun pair(request: WcPairRequest)
}
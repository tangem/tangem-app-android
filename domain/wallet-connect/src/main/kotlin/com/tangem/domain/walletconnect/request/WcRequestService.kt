package com.tangem.domain.walletconnect.request

import com.tangem.domain.walletconnect.model.WcRequest
import kotlinx.coroutines.flow.Flow

interface WcRequestService {
    val requests: Flow<WcRequest<*>>
}
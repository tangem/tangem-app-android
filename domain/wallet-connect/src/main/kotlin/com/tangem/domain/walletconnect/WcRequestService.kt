package com.tangem.domain.walletconnect

import com.tangem.domain.walletconnect.model.WcMethodName
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import kotlinx.coroutines.flow.Flow

interface WcRequestService {
    val wcRequest: Flow<Pair<WcMethodName, WcSdkSessionRequest>>
}
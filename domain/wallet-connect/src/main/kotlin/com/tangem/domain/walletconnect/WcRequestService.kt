package com.tangem.domain.walletconnect

import com.tangem.domain.walletconnect.model.WcMethodName
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import kotlinx.coroutines.flow.Flow

interface WcRequestService {
    val wcRequest: Flow<Pair<WcMethodName, WcSdkSessionRequest>>

    /**
     * Rejects [request] towards the dApp without waiting for the result. Used when the request's UI is torn down
     * by navigation before the user could answer it, so the dApp gets a rejection now instead of a timeout.
     */
    fun rejectNonBlock(request: WcSdkSessionRequest)
}

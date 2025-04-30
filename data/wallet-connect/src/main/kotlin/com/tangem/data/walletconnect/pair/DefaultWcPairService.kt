package com.tangem.data.walletconnect.pair

import com.tangem.domain.walletconnect.WcPairService
import com.tangem.domain.walletconnect.model.WcPairRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

class DefaultWcPairService @Inject constructor() : WcPairService {
    private val _pairFlow: Channel<WcPairRequest> = Channel(Channel.BUFFERED)
    override val pairFlow: Flow<WcPairRequest> = _pairFlow.receiveAsFlow()

    override fun pair(request: WcPairRequest) {
        _pairFlow.trySend(request)
    }
}
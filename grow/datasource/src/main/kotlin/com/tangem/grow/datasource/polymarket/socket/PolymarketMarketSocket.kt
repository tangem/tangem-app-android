package com.tangem.grow.datasource.polymarket.socket

import com.tangem.core.socket.Receive
import com.tangem.core.socket.Send
import com.tangem.core.socket.SocketService
import com.tangem.core.socket.SocketState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@SocketService
interface PolymarketMarketSocket {

    @Receive
    fun events(): Flow<PolymarketMarketEventDto>

    @Receive
    fun state(): StateFlow<SocketState>

    @Send
    fun updateSubscription(frame: PolymarketMarketSubscriptionUpdateDto): Boolean
}
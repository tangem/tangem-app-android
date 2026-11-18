package com.tangem.grow.datasource.polymarket.socket

import com.tangem.core.socket.Receive
import com.tangem.core.socket.Send
import com.tangem.core.socket.SocketService
import com.tangem.core.socket.SocketState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Polymarket's live-data socket (PolyBolt): reference prices of coins for crypto Up/Down windows. */
@SocketService
interface PolymarketPriceSocket {

    @Receive
    fun messages(): Flow<PolymarketPriceMessageDto>

    @Receive
    fun state(): StateFlow<SocketState>

    @Send
    fun updateSubscriptions(frame: PolymarketPriceSubscriptionsDto): Boolean
}
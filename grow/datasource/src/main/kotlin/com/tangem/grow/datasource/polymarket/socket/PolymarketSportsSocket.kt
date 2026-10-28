package com.tangem.grow.datasource.polymarket.socket

import com.tangem.core.socket.Receive
import com.tangem.core.socket.SocketService
import com.tangem.core.socket.SocketState
import com.tangem.grow.datasource.polymarket.socket.models.SportsResultDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

@SocketService
interface PolymarketSportsSocket {

    @Receive
    fun results(): Flow<SportsResultDto>

    @Receive
    fun state(): StateFlow<SocketState>
}
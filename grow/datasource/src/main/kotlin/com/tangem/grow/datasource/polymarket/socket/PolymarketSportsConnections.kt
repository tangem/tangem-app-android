package com.tangem.grow.datasource.polymarket.socket

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PolymarketSportsConnections @Inject constructor() {

    val revision: StateFlow<Long>
        field = MutableStateFlow(0L)

    internal fun connected() = revision.update { it + 1 }
}
package com.tangem.data.polymarket.price

import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceAuthDto
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceCredentialsSource
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The live-data socket is one for the whole app and prices are the same for every wallet, so the socket authorizes
 * with the credentials of the most recent price observer. They outlive the last observer for as long as the socket
 * watches them, which is its idle tail, so that coming back does not reconnect; then the secret leaves memory.
 */
@Singleton
internal class DefaultPolymarketPriceCredentialsSource @Inject constructor(
    appScope: AppCoroutineScope,
) : PolymarketPriceCredentialsSource {

    private val observers = MutableStateFlow<List<Observer>>(emptyList())

    override val credentials: StateFlow<PolymarketPriceAuthDto.Credentials?> = observers
        .mapNotNull { it.lastOrNull()?.authorization }
        .stateIn(
            scope = appScope,
            started = SharingStarted.WhileSubscribed(replayExpirationMillis = 0),
            initialValue = null,
        )

    suspend fun <T> authorize(authorization: PolymarketPriceAuthDto.Credentials, block: suspend () -> T): T {
        val observer = Observer(authorization)
        observers.update { it + observer }
        try {
            return block()
        } finally {
            observers.update { it - observer }
        }
    }

    private class Observer(val authorization: PolymarketPriceAuthDto.Credentials)
}
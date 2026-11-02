package com.tangem.domain.polymarket.sessionkey

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.interactor.ReconcilePolymarketSessionKeysInteractor
import com.tangem.domain.polymarket.model.PolymarketWalletError
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class PolymarketSessionKeyStateService(
    private val reconcile: ReconcilePolymarketSessionKeysInteractor,
) {

    private val refreshes = MutableSharedFlow<UserWalletId>(extraBufferCapacity = 1)

    fun observe(userWalletId: UserWalletId): Flow<Either<PolymarketWalletError, PolymarketSessionKeyOverview>> =
        channelFlow {
            val requested = Channel<Unit>(Channel.CONFLATED)
            launch(start = CoroutineStart.UNDISPATCHED) {
                refreshes.filter { it == userWalletId }.collect { requested.send(Unit) }
            }
            var isSettling = false
            while (true) {
                val overview = reconcile(userWalletId)
                send(overview)
                isSettling = overview.fold(
                    ifLeft = { isSettling },
                    ifRight = { it.wallet.sessionKeys.any { key -> key.status.isSettling() } },
                )
                if (isSettling) {
                    withTimeoutOrNull(PolymarketSessionKeyRelay.POLL_INTERVAL) { requested.receive() }
                } else {
                    requested.receive()
                }
            }
        }.distinctUntilChanged()

    fun refresh(userWalletId: UserWalletId) {
        refreshes.tryEmit(userWalletId)
    }
}
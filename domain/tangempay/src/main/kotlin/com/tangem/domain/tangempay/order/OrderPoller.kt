package com.tangem.domain.tangempay.order

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.domain.tangempay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class OrderPoller(
    private val orderRepository: TangemPayOrderRepository,
    private val ordersStore: OrdersStore,
    private val activeOrdersRegistry: ActiveOrdersRegistry,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val appCoroutineScope: AppCoroutineScope,
) {
    private val lock = Any()
    private val pollings = mutableMapOf<OrderRef, Polling>()

    fun poll(orderRef: OrderRef): Flow<OrderPollingState> {
        return getOrStartPolling(orderRef).state.transformWhile { state ->
            emit(state)
            !state.isTerminal
        }
    }

    fun stopPolling(userWalletIds: Collection<UserWalletId>) {
        val stopped = synchronized(lock) {
            val orderRefs = pollings.keys.filter { it.userWalletId in userWalletIds }
            orderRefs.associateWith { pollings.remove(it) }
        }
        stopped.forEach { (orderRef, polling) ->
            polling?.job?.cancel()
            activeOrdersRegistry.remove(orderRef)
            polling?.state?.value = OrderPollingState.Stopped
        }
    }

    private fun getOrStartPolling(orderRef: OrderRef): Polling {
        val polling = synchronized(lock) {
            pollings[orderRef]?.let { return it }

            val state = MutableStateFlow<OrderPollingState>(OrderPollingState.Pending)
            val job = appCoroutineScope.launch(start = CoroutineStart.LAZY) {
                pollUntilTerminal(orderRef = orderRef, state = state)
            }
            Polling(state = state, job = job).also { pollings[orderRef] = it }
        }
        polling.job.invokeOnCompletion {
            synchronized(lock) {
                if (pollings[orderRef] === polling) pollings.remove(orderRef)
            }
        }
        polling.job.start()
        return polling
    }

    private suspend fun pollUntilTerminal(orderRef: OrderRef, state: MutableStateFlow<OrderPollingState>) {
        var previousStep: OrderStep? = null
        while (true) {
            val result = orderRepository.getOrder(userWalletId = orderRef.userWalletId, orderId = orderRef.id)
            val snapshot = result.getOrNull()
            when {
                snapshot != null && snapshot.status.isTerminal -> {
                    finishPolling(
                        orderRef = orderRef,
                        state = state,
                        terminalState = OrderPollingState.Loaded(snapshot),
                    )
                    return
                }
                snapshot != null -> {
                    activeOrdersRegistry.put(orderRef = orderRef, snapshot = snapshot)
                    state.value = OrderPollingState.Loaded(snapshot)
                    if (previousStep != snapshot.step) {
                        refreshAccountStatus(orderRef)
                    }
                    previousStep = snapshot.step
                }
                result.leftOrNull() == TangemPayApiError.OrderNotFound -> {
                    finishPolling(orderRef = orderRef, state = state, terminalState = OrderPollingState.NotFound)
                    return
                }
            }
            delay(5.seconds)
        }
    }

    private suspend fun finishPolling(
        orderRef: OrderRef,
        state: MutableStateFlow<OrderPollingState>,
        terminalState: OrderPollingState,
    ) {
        activeOrdersRegistry.remove(orderRef)
        ordersStore.remove(orderRef)
        refreshAccountStatus(orderRef)
        state.value = terminalState
    }

    private suspend fun refreshAccountStatus(orderRef: OrderRef) {
        paymentAccountStatusFetcher(orderRef.userWalletId)
    }

    private class Polling(val state: MutableStateFlow<OrderPollingState>, val job: Job)
}
package com.tangem.domain.pay.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

class StartTangemPayOrderPollingUseCase(
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {

    /**
     * Order keys (`walletId:orderId`) currently being polled. Keeps polling idempotent so callers that
     * may fire repeatedly for the same order (e.g. order restore on every wallet (re)load) never spawn a
     * second poller for it.
     */
    private val activeOrders = ConcurrentHashMap.newKeySet<String>()

    /**
     * @param onTerminalReached invoked once the order is terminal, **before** the status refresh. Callers
     * use it to forget the locally stored order-id hint (issue / reissue / close) so the refresh does not
     * re-issue a `GET /order/{id}` for the order that was just resolved.
     * @param timeout when set, bounds the poll loop — a still-non-terminal order after [timeout] elapses
     * makes this return `false` (as if canceled) without further polling. `null` (default) polls
     * indefinitely, preserving existing callers' behavior exactly.
     */
    suspend operator fun invoke(
        order: TangemPayOrderInfo,
        userWalletId: UserWalletId,
        onTerminalReached: (suspend () -> Unit)? = null,
        timeout: Duration? = null,
    ): Boolean {
        // A poller for this exact order is already running — `false` only reaches fire-and-forget issue
        // callers (restore / issue-additional); the awaiting freeze caller always polls a fresh order id.
        val key = "${userWalletId.stringValue}:${order.orderId}"
        if (!activeOrders.add(key)) return false

        try {
            val onTerminal = onTerminalReached ?: {}
            val pollBlock: suspend () -> Boolean = { pollUntilTerminal(order, userWalletId, onTerminal) }
            return if (timeout != null) {
                withTimeoutOrNull(timeout) { pollBlock() } == true
            } else {
                pollBlock()
            }
        } finally {
            activeOrders.remove(key)
        }
    }

    private suspend fun pollUntilTerminal(
        order: TangemPayOrderInfo,
        userWalletId: UserWalletId,
        onTerminalReached: suspend () -> Unit,
    ): Boolean {
        while (true) {
            val newOrder = if (order.orderStatus.isTerminal) {
                order
            } else {
                cardDetailsRepository.getOrderInfo(userWalletId, order.orderId).getOrNull()
            }

            if (newOrder != null && newOrder.orderStatus.isTerminal) {
                onTerminalReached()
                paymentAccountStatusFetcher.invoke(userWalletId)
                return newOrder.orderStatus == OrderStatus.COMPLETED
            }

            delay(POLLING_DELAY)
        }
    }

    companion object {
        private const val POLLING_DELAY = 3000L
    }
}
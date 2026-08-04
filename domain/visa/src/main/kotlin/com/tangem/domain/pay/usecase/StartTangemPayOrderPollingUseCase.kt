package com.tangem.domain.pay.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

class StartTangemPayOrderPollingUseCase(
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {

    private val logger = TangemLogger.withTag("StartTangemPayOrderPollingUseCase")

    /**
     * Order keys (`walletId:orderId`) currently being polled. Keeps polling idempotent so callers that
     * may fire repeatedly for the same order (e.g. order restore on every wallet (re)load) never spawn a
     * second poller for it.
     */
    private val activeOrders = ConcurrentHashMap.newKeySet<String>()

    /**
     * @param onOrderStateChange invoked on every observed order change, including the terminal one, and
     * **before** the status refresh. Callers use it to forget the locally stored order-id hint
     * (issue / reissue / close) so the refresh does not re-issue an order request for the order that was
     * just resolved, and to react to intermediate steps such as `AWAITING_DEPOSIT`.
     * @param timeout when set, bounds the poll loop — a still-non-terminal order after [timeout] elapses
     * makes this return `false` (as if canceled) without further polling. `null` (default) polls
     * indefinitely, preserving existing callers' behavior exactly.
     */
    suspend operator fun invoke(
        order: TangemPayOrderInfo,
        userWalletId: UserWalletId,
        onOrderStateChange: (suspend (TangemPayOrderInfo) -> Unit)? = null,
        timeout: Duration? = null,
    ): Boolean {
        // A poller for this exact order is already running — `false` only reaches fire-and-forget issue
        // callers (restore / issue-additional); the awaiting freeze caller always polls a fresh order id.
        val key = "${userWalletId.stringValue}:${order.orderId}"
        if (!activeOrders.add(key)) return false

        try {
            val pollBlock: suspend () -> Boolean = {
                pollUntilTerminal(order, userWalletId, onOrderStateChange)
            }
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
        onOrderStateChange: (suspend (TangemPayOrderInfo) -> Unit)?,
    ): Boolean {
        var currentOrder: TangemPayOrderInfo? = null
        while (true) {
            val newOrder = if (order.orderStatus.isTerminal) {
                order
            } else {
                getOrderInfo(userWalletId = userWalletId, orderId = order.orderId)
            }

            if (newOrder != null && newOrder != currentOrder) {
                currentOrder = newOrder
                onOrderStateChange?.invoke(newOrder)
            }

            if (newOrder != null && newOrder.orderStatus.isTerminal) {
                paymentAccountStatusFetcher.invoke(userWalletId)
                return newOrder.orderStatus == OrderStatus.COMPLETED
            }

            delay(POLLING_DELAY)
        }
    }

    private suspend fun getOrderInfo(userWalletId: UserWalletId, orderId: String): TangemPayOrderInfo? {
        return cardDetailsRepository.getOrderInfo(userWalletId, orderId).fold(
            ifLeft = { error ->
                if (error == VisaApiError.OrderNotFound) {
                    logger.i("$orderId: does not exist on the backend, resolving as CANCELED")
                    TangemPayOrderInfo(orderId = orderId, orderStatus = OrderStatus.CANCELED)
                } else {
                    logger.e("$orderId: poll failed, will retry — $error")
                    null
                }
            },
            ifRight = { it },
        )
    }

    companion object {
        private const val POLLING_DELAY = 5_000L
    }
}
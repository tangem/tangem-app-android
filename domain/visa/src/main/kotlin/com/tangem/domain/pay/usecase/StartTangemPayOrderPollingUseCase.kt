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
import kotlin.time.Duration.Companion.seconds

class StartTangemPayOrderPollingUseCase(
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
) {

    private val logger = TangemLogger.withTag("StartTangemPayOrderPollingUseCase")
    private val activeOrders = ConcurrentHashMap.newKeySet<String>()

    suspend operator fun invoke(
        order: TangemPayOrderInfo,
        userWalletId: UserWalletId,
        onOrderStateChange: (suspend (TangemPayOrderInfo) -> Unit)? = null,
        onTerminalRefresh: (suspend (UserWalletId) -> Unit) = { paymentAccountStatusFetcher.invoke(userWalletId) },
        timeout: Duration? = null,
    ): Boolean {
        val key = "${userWalletId.stringValue}:${order.orderId}"
        if (!activeOrders.add(key)) return false

        try {
            val pollBlock: suspend () -> Boolean = {
                pollUntilTerminal(
                    order = order,
                    userWalletId = userWalletId,
                    onOrderStateChange = onOrderStateChange,
                    onTerminalRefresh = onTerminalRefresh,
                )
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
        onTerminalRefresh: (suspend (UserWalletId) -> Unit),
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
                onTerminalRefresh(userWalletId)
                return newOrder.orderStatus == OrderStatus.COMPLETED
            }

            delay(5.seconds)
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
}
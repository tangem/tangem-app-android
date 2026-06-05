package com.tangem.domain.pay

import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.model.TangemPayPendingOrder
import com.tangem.domain.pay.repository.TangemPayPendingOrdersRepository
import com.tangem.domain.pay.usecase.StartTangemPayOrderPollingUseCase
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap

class TangemPayOrderPollingScheduler(
    private val pendingOrdersRepository: TangemPayPendingOrdersRepository,
    private val startTangemPayOrderPollingUseCase: StartTangemPayOrderPollingUseCase,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val appCoroutineScope: AppCoroutineScope,
) {
    private val jobs = ConcurrentHashMap<String, Deferred<Boolean>>()

    suspend fun scheduleOrderAsync(order: TangemPayPendingOrder): Deferred<Boolean> {
        pendingOrdersRepository.save(order)
        return startPolling(order)
    }

    suspend fun resumeAll() {
        runSuspendCatching {
            pendingOrdersRepository.getAll().forEach { startPolling(it) }
        }
    }

    private fun startPolling(order: TangemPayPendingOrder): Deferred<Boolean> {
        val newJob = appCoroutineScope.async(start = CoroutineStart.LAZY) {
            try {
                startTangemPayOrderPollingUseCase(
                    order = TangemPayOrderInfo(orderId = order.orderId, orderStatus = order.status),
                    userWalletId = order.userWalletId,
                ).apply {
                    pendingOrdersRepository.remove(order.orderId)
                    paymentAccountStatusFetcher.invoke(order.userWalletId)
                }
            } finally {
                jobs.remove(order.orderId)
            }
        }
        val existing = jobs.putIfAbsent(order.orderId, newJob)
        return if (existing != null) {
            newJob.cancel()
            existing
        } else {
            newJob.start()
            newJob
        }
    }
}
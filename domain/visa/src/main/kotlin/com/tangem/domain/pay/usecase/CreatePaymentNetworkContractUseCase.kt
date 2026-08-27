package com.tangem.domain.pay.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.Order
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.CustomerOrderRepository
import com.tangem.domain.pay.util.OrderResolver
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Creates (or resumes) the smart-contract issuance order for a not-yet-issued multichain network,
 * so the network flips from `NOT_ISSUED` to `ENABLED`.
 *
 * Fire-and-forget: [invoke] suspends only for the fast part (finding/creating the order) and returns
 * once the order is active; the completion polling runs in [AppCoroutineScope], so it neither blocks
 * the caller nor dies with the caller's scope (e.g. when the Choose-network sheet is dismissed).
 * The outcome propagates through the poller's payment-account status refresh — observers see the
 * network become `Available`. The poll itself gives up after [POLL_TIMEOUT] without a terminal state.
 *
 * The wire `chain_id` is derived here from the domain [Network] (via the blockchain SDK), so callers
 * stay blockchain-SDK-free. A network whose chain id cannot be derived (non-EVM, unknown) yields
 * [VisaApiError.Unspecified] without touching the backend.
 *
 * Not-yet-finalized backend contract:
 *  - [OrderType.SMART_CONTRACT_ISSUE_RAIN] is a placeholder wire value pending backend confirmation.
 *  - The chain id cannot be matched against existing orders yet — [Order] does not expose a chain id,
 *    so any active [OrderType.SMART_CONTRACT_ISSUE_RAIN] order for the wallet is treated as "the"
 *    active order (see [OrderResolver.selectActive]). Revisit once the backend order payload carries a
 *    chain id (or another per-network hint) so multiple in-flight network contracts can be
 *    disambiguated.
 *
 * Idempotent: an active ([OrderStatus.NEW] / [OrderStatus.PROCESSING]) order is resumed (polled)

 * (1s / 2s / 4s) for transient server/network failures only ([VisaApiError.ServerUnavailable] or
 * [VisaApiError.UnknownWithoutCode] — see [TangemPayErrorConverter]); any other (4xx / business) error
 * is returned immediately, without retrying.
 *
 * @return `Right(Unit)` once the order is active and background polling has started; `Left` if the

 */
class CreatePaymentNetworkContractUseCase(
    private val customerOrderRepository: CustomerOrderRepository,
    private val pollingUseCase: StartTangemPayOrderPollingUseCase,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, network: Network): Either<VisaApiError, Unit> = either {
        val chainId = network.toBlockchain().getChainId() ?: run {
            TangemLogger.e("TangemPay: unable to derive chain id for network ${network.rawId}")
            raise(VisaApiError.Unspecified)
        }

        val activeOrders = customerOrderRepository.findOrders(
            userWalletId = userWalletId,
            types = setOf(OrderType.SMART_CONTRACT_ISSUE_RAIN),
            statuses = OrderStatus.activeStatuses,
        ).bind()

        val order = OrderResolver.selectActive(orders = activeOrders, type = OrderType.SMART_CONTRACT_ISSUE_RAIN)
            ?: createOrderWithRetry(userWalletId = userWalletId, chainId = chainId).bind()

        startPolling(order = order, userWalletId = userWalletId)
    }

    private fun startPolling(order: Order, userWalletId: UserWalletId) {
        appCoroutineScope.launch {
            pollingUseCase.invoke(
                order = TangemPayOrderInfo(orderId = order.id, orderStatus = order.status),
                userWalletId = userWalletId,
                timeout = POLL_TIMEOUT,
            )
        }
    }

    /**
     * `createOrder` retried on transient failures only, reusing a single idempotency key across
     * attempts (the key exists precisely to make repeated identical requests safe to retry).
     */
    private suspend fun createOrderWithRetry(userWalletId: UserWalletId, chainId: Int): Either<VisaApiError, Order> {
        val idempotencyKey = UUID.randomUUID().toString()
        var attempt = 0
        while (true) {
            val result = customerOrderRepository.createOrder(
                userWalletId = userWalletId,
                type = OrderType.SMART_CONTRACT_ISSUE_RAIN,
                specificationName = null,
                idempotencyKey = idempotencyKey,
                chainId = chainId,
            )
            val error = result.leftOrNull()
            if (error == null || attempt >= RETRY_DELAYS.size || !error.isRetryable()) return result
            delay(RETRY_DELAYS[attempt])
            attempt++
        }
    }

    /** Transient (server/network) failures are retryable; 4xx business errors are not. */
    private fun VisaApiError.isRetryable(): Boolean =
        this == VisaApiError.ServerUnavailable || this == VisaApiError.UnknownWithoutCode

    private companion object {
        val POLL_TIMEOUT: Duration = 60.seconds
        val RETRY_DELAYS: List<Duration> = listOf(1.seconds, 2.seconds, 4.seconds)
    }
}
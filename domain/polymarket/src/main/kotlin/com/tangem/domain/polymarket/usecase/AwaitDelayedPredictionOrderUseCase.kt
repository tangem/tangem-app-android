package com.tangem.domain.polymarket.usecase

import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PredictionOrderSigningRoute
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStateError
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class AwaitDelayedPredictionOrderUseCase(
    private val polymarketRepository: PolymarketRepository,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
) {

    /** @param route the route the order was placed on; its identity reads the order back */
    suspend operator fun invoke(
        addresses: PolymarketAddresses,
        orderId: String,
        route: PredictionOrderSigningRoute = PredictionOrderSigningRoute.Owner,
    ): DelayedPredictionOrderOutcome {
        val identity = when (route) {
            PredictionOrderSigningRoute.Owner -> {
                val credentials = getApiCredentials(userWalletId = addresses.userWalletId)
                    ?: return DelayedPredictionOrderOutcome.Failed(detail = "No stored CLOB credentials")
                PolymarketClobIdentity.Owner(address = addresses.ownerAddress, credentials = credentials)
            }
            is PredictionOrderSigningRoute.Session -> route.identity
        }

        repeat(times = MAX_READS) {
            delay(READ_INTERVAL)

            val outcome = polymarketRepository
                .getOrder(identity = identity, orderId = orderId)
                .fold(
                    ifLeft = { error -> error.toOutcome(orderId = orderId) },
                    ifRight = { order -> order.toOutcome(orderId = orderId) },
                )

            if (outcome != null) return outcome
        }

        return DelayedPredictionOrderOutcome.Unconfirmed
    }

    private fun PredictionOrderStateError.toOutcome(orderId: String): DelayedPredictionOrderOutcome? {
        return if (leavesStatusUnknown()) {
            TangemLogger.v("Delayed order $orderId: read failed with $this, reading again")
            null
        } else {
            DelayedPredictionOrderOutcome.Failed(detail = "Read failed with $this")
        }
    }

    private fun PredictionOrderStateError.leavesStatusUnknown(): Boolean = when (this) {
        PredictionOrderStateError.Network,
        PredictionOrderStateError.Timeout,
        -> true
        is PredictionOrderStateError.Unknown -> httpCode in SERVER_ERROR_CODES
        PredictionOrderStateError.NotFound,
        is PredictionOrderStateError.Unauthorized,
        -> false
    }

    private fun PredictionOrderState.toOutcome(orderId: String): DelayedPredictionOrderOutcome? {
        return when (val status = status) {
            PredictionOrderStatus.Matched -> DelayedPredictionOrderOutcome.Matched(order = this)
            is PredictionOrderStatus.Rejected -> DelayedPredictionOrderOutcome.Failed(
                detail = "Order $orderId is ${status.raw}",
            )
            is PredictionOrderStatus.Other -> {
                TangemLogger.v("Delayed order $orderId is ${status.raw}, reading again")
                null
            }
        }
    }

    private companion object {
        const val MAX_READS = 3
        val READ_INTERVAL = 10.seconds
        val SERVER_ERROR_CODES = 500..599
    }
}

sealed interface DelayedPredictionOrderOutcome {

    data class Matched(val order: PredictionOrderState) : DelayedPredictionOrderOutcome

    /** The book answered, and the answer is not a match. */
    data class Failed(val detail: String?) : DelayedPredictionOrderOutcome

    /** The reads ran out without the book saying whether the order matched; it may still have. */
    data object Unconfirmed : DelayedPredictionOrderOutcome
}
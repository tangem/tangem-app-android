package com.tangem.domain.polymarket.usecase

import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStateError
import com.tangem.domain.polymarket.model.PredictionOrderStatus
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

/**
 * Waits for an order the exchange accepted as `delayed` to be matched, reading it back a fixed number of times.
 *
 * A read that leaves the status unknown — a timeout, a dropped connection, a 5xx — is tried again: the book
 * has not answered, so the order may still be matching. Every actual answer is final — a match, a status that
 * is not a match, an id the book does not know, refused credentials — and the wait ends with it. When the
 * reads run out without an answer the order stays [DelayedPredictionOrderOutcome.Unconfirmed].
 */
class AwaitDelayedPredictionOrderUseCase(
    private val polymarketRepository: PolymarketRepository,
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase,
) {

    suspend operator fun invoke(addresses: PolymarketAddresses, orderId: String): DelayedPredictionOrderOutcome {
        val credentials = getApiCredentials(userWalletId = addresses.userWalletId)
            ?: return DelayedPredictionOrderOutcome.Failed(detail = "No stored CLOB credentials")

        repeat(times = MAX_READS) {
            delay(READ_INTERVAL)

            val outcome = polymarketRepository
                .getOrder(ownerAddress = addresses.ownerAddress, credentials = credentials, orderId = orderId)
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

    private fun PredictionOrderState.toOutcome(orderId: String): DelayedPredictionOrderOutcome {
        return when (val status = status) {
            PredictionOrderStatus.Matched -> DelayedPredictionOrderOutcome.Matched(order = this)
            is PredictionOrderStatus.Other -> DelayedPredictionOrderOutcome.Failed(
                detail = "Order $orderId is ${status.raw}, not matched",
            )
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
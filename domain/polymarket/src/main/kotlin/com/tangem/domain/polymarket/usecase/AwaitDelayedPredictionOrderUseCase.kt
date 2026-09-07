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
 * Only a read that times out is tried again: the book has not answered, so the order may still be matching.
 * Every other answer is final — a match, a status that is not a match, an id the book does not know, or a
 * transport failure — and the wait ends with it. The wait also ends when the reads run out.
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

        return DelayedPredictionOrderOutcome.TimedOut
    }

    private fun PredictionOrderStateError.toOutcome(orderId: String): DelayedPredictionOrderOutcome? = when (this) {
        PredictionOrderStateError.Timeout -> {
            TangemLogger.v("Delayed order $orderId: read timed out, reading again")
            null
        }
        PredictionOrderStateError.Network,
        PredictionOrderStateError.NotFound,
        is PredictionOrderStateError.Unauthorized,
        is PredictionOrderStateError.Unknown,
        -> DelayedPredictionOrderOutcome.Failed(detail = "Read failed with $this")
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
    }
}

sealed interface DelayedPredictionOrderOutcome {

    data class Matched(val order: PredictionOrderState) : DelayedPredictionOrderOutcome

    /** The book answered, and the answer is not a match. */
    data class Failed(val detail: String?) : DelayedPredictionOrderOutcome

    /** The reads ran out without an answer from the book. */
    data object TimedOut : DelayedPredictionOrderOutcome
}
package com.tangem.data.pay.util

import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState
import com.tangem.spend.datasource.pay.models.response.OrderResponse
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger

private const val TAG = "BankOfframpOrderStateConverter"

/**
 * Reads where a bank off-ramp order stands. A signature is only asked for once the payload it belongs to has been
 * checked against its hash — an order the app cannot verify is treated as failed rather than signed blindly.
 */
internal object BankOfframpOrderStateConverter : Converter<OrderResponse.Result, BankOfframpOrderState> {

    override fun convert(value: OrderResponse.Result): BankOfframpOrderState = when (value.status) {
        OrderResponse.Result.Status.COMPLETED -> BankOfframpOrderState.Completed
        OrderResponse.Result.Status.CANCELED -> BankOfframpOrderState.Canceled
        OrderResponse.Result.Status.NEW,
        OrderResponse.Result.Status.PROCESSING,
        OrderResponse.Result.Status.UNKNOWN,
        -> convertActiveOrder(value)
    }

    private fun convertActiveOrder(value: OrderResponse.Result): BankOfframpOrderState {
        val step = OrderStep.fromString(value.step)
        if (step != OrderStep.AWAITING_SIGNATURE) return BankOfframpOrderState.Processing(step)

        val withdrawData = value.data.withdrawData
            ?: return failed(orderId = value.id, reason = "order asks for a signature without withdraw data")
        val structuredData = withdrawData.structuredData
            ?: return failed(orderId = value.id, reason = "order asks for a signature without structured data")

        val isConsistent = BankOfframpPayloadVerifier.isConsistent(
            hash = withdrawData.hash,
            structuredData = structuredData,
        )
        return if (isConsistent) {
            BankOfframpOrderState.AwaitingSignature(payloadHash = withdrawData.hash)
        } else {
            failed(orderId = value.id, reason = "the hash is not the digest of the structured data")
        }
    }

    private fun failed(orderId: String, reason: String): BankOfframpOrderState.Failed {
        TangemLogger.withTag(TAG).e("Bank off-ramp order $orderId cannot be signed: $reason")
        return BankOfframpOrderState.Failed(
            error = BankOfframpError.Unknown(cause = IllegalStateException("Order $orderId: $reason")),
        )
    }
}
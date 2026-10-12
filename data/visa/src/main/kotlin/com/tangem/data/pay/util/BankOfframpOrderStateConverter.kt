package com.tangem.data.pay.util

import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState
import com.tangem.spend.datasource.pay.models.response.OrderResponse
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger

private const val TAG = "BankOfframpOrderStateConverter"

/**
 * Reads where a bank off-ramp order stands. A signature is only asked for once the payload it belongs to has been
 * checked against the order it was placed for — an order the app cannot verify is treated as failed rather than
 * signed blindly.
 */
internal object BankOfframpOrderStateConverter :
    Converter<BankOfframpOrderStateConverter.Input, BankOfframpOrderState> {

    data class Input(val result: OrderResponse.Result, val request: BankOfframpOrderRequest)

    override fun convert(value: Input): BankOfframpOrderState = when (value.result.status) {
        OrderResponse.Result.Status.COMPLETED -> BankOfframpOrderState.Completed
        OrderResponse.Result.Status.CANCELED -> BankOfframpOrderState.Canceled
        OrderResponse.Result.Status.NEW,
        OrderResponse.Result.Status.PROCESSING,
        OrderResponse.Result.Status.UNKNOWN,
        -> convertActiveOrder(value)
    }

    private fun convertActiveOrder(value: Input): BankOfframpOrderState {
        val result = value.result
        val step = OrderStep.fromString(result.step)
        if (step != OrderStep.AWAITING_SIGNATURE) return BankOfframpOrderState.Processing(step)

        val withdrawData = result.data.withdrawData
            ?: return failed(orderId = result.id, reason = "order asks for a signature without withdraw data")

        return TangemPayWithdrawDataVerifier.verify(
            hash = withdrawData.hash,
            structuredData = withdrawData.structuredData,
            expectation = value.request.toExpectation(),
        ).fold(
            ifLeft = { mismatch -> failed(orderId = result.id, reason = "withdraw data mismatch: $mismatch") },
            ifRight = { BankOfframpOrderState.AwaitingSignature(payloadHash = withdrawData.hash) },
        )
    }

    private fun BankOfframpOrderRequest.toExpectation() = TangemPayWithdrawDataVerifier.Expectation(
        amountInCents = amountInCents.toBigDecimal(),
        tokenDecimals = tokenDecimals,
        recipientAddress = null,
        chainId = chainId,
        tokenContractAddress = tokenContractAddress,
    )

    private fun failed(orderId: String, reason: String): BankOfframpOrderState.Failed {
        TangemLogger.withTag(TAG).e("Bank off-ramp order $orderId cannot be signed: $reason")
        return BankOfframpOrderState.Failed(
            error = BankOfframpError.Unknown(cause = IllegalStateException("Order $orderId: $reason")),
        )
    }
}
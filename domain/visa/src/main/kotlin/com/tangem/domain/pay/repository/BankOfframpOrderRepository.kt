package com.tangem.domain.pay.repository

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState

/**
 * Placing a bank off-ramp withdrawal: create the order, follow it until it asks for a signature, then sign
 * it or abandon it.
 *
 * Every function is total: failures come back as [BankOfframpError], never thrown.
 */
interface BankOfframpOrderRepository {

    suspend fun createOrder(
        userWalletId: UserWalletId,
        request: BankOfframpOrderRequest,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpOrderResult>

    /** An order asking for a signature on anything but [request] is reported as [BankOfframpOrderState.Failed]. */
    suspend fun getOrderState(
        userWalletId: UserWalletId,
        orderId: String,
        request: BankOfframpOrderRequest,
    ): Either<BankOfframpError, BankOfframpOrderState>

    /** [idempotencyKey] is the one the order was created with, so a resubmitted signature settles the same order. */
    suspend fun submitSignature(
        userWalletId: UserWalletId,
        orderId: String,
        signature: String,
        idempotencyKey: String,
    ): Either<BankOfframpError, Unit>

    suspend fun cancelOrder(userWalletId: UserWalletId, orderId: String): Either<BankOfframpError, Unit>
}
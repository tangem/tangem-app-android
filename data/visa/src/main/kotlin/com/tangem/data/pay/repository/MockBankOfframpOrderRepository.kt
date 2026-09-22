package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.repository.BankOfframpOrderRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory stand-in for placing a withdrawal while the order endpoints of the bank off-ramp BFF are still
 * being agreed. Recipients and routes already come from the backend.
 *
 * Orders move `Processing → AwaitingSignature` by wall-clock age and `→ Completed` once signed; a few request
 * values trigger each backend error or outcome so every path is testable from the UI. Only the details the
 * wizard has typed in are checked — for a recipient the customer picked from the saved list, the backend is
 * the one that validates.
 */
internal class MockBankOfframpOrderRepository(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : BankOfframpOrderRepository {

    private val mutex = Mutex()
    private val orders = mutableMapOf<String, MockOrder>()
    private var nextOrderIndex = 1

    override suspend fun createOrder(
        userWalletId: UserWalletId,
        request: BankOfframpOrderRequest,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpOrderResult> = mutex.withLock {
        either {
            triggeredError(request)?.let { raise(it) }
            val orderId = "$ORDER_ID_PREFIX${nextOrderIndex++}"
            orders[orderId] = MockOrder(createdAtMillis = currentTimeMillis(), outcome = triggeredOutcome(request))
            BankOfframpOrderResult(
                orderId = orderId,
                bankAccountId = (request.recipient as? BankOfframpRecipient.Saved)?.bankAccountId,
            )
        }
    }

    override suspend fun getOrderState(
        userWalletId: UserWalletId,
        orderId: String,
    ): Either<BankOfframpError, BankOfframpOrderState> = mutex.withLock {
        val order = orders[orderId] ?: return@withLock unknownOrder(orderId).left()
        stateOf(orderId, order).right()
    }

    override suspend fun submitSignature(
        userWalletId: UserWalletId,
        orderId: String,
        signature: String,
        idempotencyKey: String,
    ): Either<BankOfframpError, Unit> = mutex.withLock {
        either {
            val order = orders[orderId] ?: raise(unknownOrder(orderId))
            ensure(stateOf(orderId, order) is BankOfframpOrderState.AwaitingSignature) {
                BankOfframpError.Unknown(cause = IllegalStateException("Order $orderId is not awaiting a signature"))
            }
            orders[orderId] = order.copy(isSigned = true)
        }
    }

    override suspend fun cancelOrder(userWalletId: UserWalletId, orderId: String): Either<BankOfframpError, Unit> {
        return mutex.withLock {
            either {
                val order = orders[orderId] ?: raise(unknownOrder(orderId))
                ensure(!order.isSigned) {
                    BankOfframpError.Unknown(cause = IllegalStateException("Order $orderId is already completed"))
                }
                orders[orderId] = order.copy(isCanceled = true)
            }
        }
    }

    private fun stateOf(orderId: String, order: MockOrder): BankOfframpOrderState {
        val isProcessing = currentTimeMillis() - order.createdAtMillis < AWAITING_SIGNATURE_DELAY_MILLIS
        return when {
            order.isCanceled -> BankOfframpOrderState.Canceled
            order.isSigned -> BankOfframpOrderState.Completed
            isProcessing || order.outcome == MockOutcome.STAYS_PROCESSING ->
                BankOfframpOrderState.Processing(OrderStep.UNKNOWN)
            order.outcome == MockOutcome.FAILS -> BankOfframpOrderState.Failed(
                BankOfframpError.Unknown(cause = IllegalStateException("Mock order $orderId failed on the backend")),
            )
            else -> BankOfframpOrderState.AwaitingSignature(payloadHash = FAKE_PAYLOAD_HASH)
        }
    }

    private fun triggeredError(request: BankOfframpOrderRequest): BankOfframpError? {
        val details = (request.recipient as? BankOfframpRecipient.New)?.details
        return details?.triggeredError() ?: when (request.amountInCents) {
            SENDER_REGION_TRIGGER_CENTS -> BankOfframpError.SenderRegionUnsupported
            WITHDRAWAL_IN_PROGRESS_TRIGGER_CENTS -> BankOfframpError.WithdrawalInProgress
            else -> null
        }
    }

    private fun BankAccountDetails.triggeredError(): BankOfframpError? {
        val beneficiary = beneficiary
        return when {
            routingNumber == INVALID_ROUTING_NUMBER -> BankOfframpError.Validation
            beneficiaryAddress?.countryCode != SUPPORTED_COUNTRY_CODE -> BankOfframpError.RecipientRegionUnsupported
            beneficiary is Beneficiary.Business && beneficiary.name.equals(BLOCKED_BUSINESS_NAME, ignoreCase = true) ->
                BankOfframpError.BusinessUnsupported
            else -> null
        }
    }

    private fun triggeredOutcome(request: BankOfframpOrderRequest): MockOutcome = when (request.amountInCents) {
        FAILED_TRIGGER_CENTS -> MockOutcome.FAILS
        STUCK_TRIGGER_CENTS -> MockOutcome.STAYS_PROCESSING
        else -> MockOutcome.AWAITS_SIGNATURE
    }

    private fun unknownOrder(orderId: String) = BankOfframpError.Unknown(
        cause = IllegalArgumentException("Unknown bank off-ramp order $orderId"),
    )

    private data class MockOrder(
        val createdAtMillis: Long,
        val outcome: MockOutcome,
        val isSigned: Boolean = false,
        val isCanceled: Boolean = false,
    )

    private enum class MockOutcome { AWAITS_SIGNATURE, FAILS, STAYS_PROCESSING }

    private companion object {
        const val ORDER_ID_PREFIX = "mock-bank-offramp-order-"
        const val FAKE_PAYLOAD_HASH = "0x5f3c4d3e2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e6d5c4b3a2f1e0d9c8b7a6f5e"
        const val AWAITING_SIGNATURE_DELAY_MILLIS = 6_000L
        const val INVALID_ROUTING_NUMBER = "000000001"
        const val SUPPORTED_COUNTRY_CODE = "US"
        const val SENDER_REGION_TRIGGER_CENTS = 66_600L
        const val WITHDRAWAL_IN_PROGRESS_TRIGGER_CENTS = 77_700L
        const val FAILED_TRIGGER_CENTS = 88_800L
        const val STUCK_TRIGGER_CENTS = 99_900L
        const val BLOCKED_BUSINESS_NAME = "Blocked"
    }
}
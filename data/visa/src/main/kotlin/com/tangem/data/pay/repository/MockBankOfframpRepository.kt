package com.tangem.data.pay.repository

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountDetails
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderState
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.Beneficiary
import com.tangem.domain.pay.repository.BankOfframpRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory stand-in for the bank off-ramp BFF while the backend is in development.
 * Orders move `Processing → AwaitingSignature` by wall-clock age and `→ Completed` once signed;
 * a few request values trigger each backend error or outcome so every path is testable from the UI.
 */
internal class MockBankOfframpRepository(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : BankOfframpRepository {

    private val mutex = Mutex()
    private val accounts = MockBankOfframpSeed.accounts.toMutableList()
    private val orders = mutableMapOf<String, MockOrder>()
    private var nextAccountIndex = accounts.size + 1
    private var nextOrderIndex = 1

    override suspend fun getBankAccounts(userWalletId: UserWalletId): Either<BankOfframpError, List<BankAccount>> {
        delay(ACCOUNTS_FETCH_DELAY_MILLIS)
        return mutex.withLock { accounts.toList().right() }
    }

    override suspend fun getOptions(userWalletId: UserWalletId): Either<BankOfframpError, BankOfframpOptions> {
        return MockBankOfframpSeed.options.right()
    }

    override suspend fun createOrder(
        userWalletId: UserWalletId,
        request: BankOfframpOrderRequest,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpOrderResult> = mutex.withLock {
        either {
            val details = when (val recipient = request.recipient) {
                is BankOfframpRecipient.Saved -> accounts.firstOrNull { it.id == recipient.bankAccountId }?.details
                    ?: raise(BankOfframpError.Validation)
                is BankOfframpRecipient.New -> recipient.details
            }
            triggeredError(request, details)?.let { raise(it) }
            val bankAccountId = when (val recipient = request.recipient) {
                is BankOfframpRecipient.Saved -> recipient.bankAccountId
                is BankOfframpRecipient.New -> saveRecipient(recipient.details).id
            }
            val orderId = "$ORDER_ID_PREFIX${nextOrderIndex++}"
            orders[orderId] = MockOrder(createdAtMillis = currentTimeMillis(), outcome = triggeredOutcome(request))
            BankOfframpOrderResult(orderId = orderId, bankAccountId = bankAccountId)
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

    private fun triggeredError(request: BankOfframpOrderRequest, details: BankAccountDetails): BankOfframpError? {
        val beneficiary = details.beneficiary
        return when {
            details.routingNumber == INVALID_ROUTING_NUMBER -> BankOfframpError.Validation
            details.beneficiaryAddress.countryCode != SUPPORTED_COUNTRY_CODE ->
                BankOfframpError.RecipientRegionUnsupported
            request.amountInCents == SENDER_REGION_TRIGGER_CENTS -> BankOfframpError.SenderRegionUnsupported
            request.amountInCents == WITHDRAWAL_IN_PROGRESS_TRIGGER_CENTS -> BankOfframpError.WithdrawalInProgress
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

    private fun saveRecipient(details: BankAccountDetails): BankAccount {
        val account = BankAccount(
            id = "${MockBankOfframpSeed.ACCOUNT_ID_PREFIX}${nextAccountIndex++}",
            displayName = MockBankOfframpSeed.displayName(details.beneficiary),
            details = details,
        )
        accounts += account
        return account
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
        // Long enough for the hub's recents shimmer to be visible against the in-memory mock.
        const val ACCOUNTS_FETCH_DELAY_MILLIS = 600L
        const val INVALID_ROUTING_NUMBER = "000000001"
        const val SUPPORTED_COUNTRY_CODE = "US"
        const val SENDER_REGION_TRIGGER_CENTS = 66_600L
        const val WITHDRAWAL_IN_PROGRESS_TRIGGER_CENTS = 77_700L
        const val FAILED_TRIGGER_CENTS = 88_800L
        const val STUCK_TRIGGER_CENTS = 99_900L
        const val BLOCKED_BUSINESS_NAME = "Blocked"
    }
}